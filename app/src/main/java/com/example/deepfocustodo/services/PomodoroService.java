package com.example.deepfocustodo.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.utils.PreferenceHelper;
import com.example.deepfocustodo.utils.SessionManager;

import java.util.Locale;

public class PomodoroService extends Service {

	public static final String ACTION_START = "com.example.deepfocustodo.action.START";
	public static final String ACTION_PAUSE = "com.example.deepfocustodo.action.PAUSE";
	public static final String ACTION_RESUME = "com.example.deepfocustodo.action.RESUME";
	public static final String ACTION_STOP = "com.example.deepfocustodo.action.STOP";
	public static final String ACTION_RESET = "com.example.deepfocustodo.action.RESET";
	public static final String ACTION_APPLY_SETTINGS = "com.example.deepfocustodo.action.APPLY_SETTINGS";

	public static final String ACTION_STATE = "com.example.deepfocustodo.action.STATE";

	public static final String EXTRA_TIME_LEFT = "extra_time_left";
	public static final String EXTRA_IS_RUNNING = "extra_is_running";
	public static final String EXTRA_IS_FOCUS = "extra_is_focus";
	public static final String EXTRA_COMPLETED_FOCUS = "extra_completed_focus";
	public static final String EXTRA_SESSION_IN_PROGRESS = "extra_session_in_progress";

	private static final String CHANNEL_ID = "pomodoro_service_channel";
	private static final String EVENT_CHANNEL_ID = "pomodoro_event_channel";
	private static final int NOTIFICATION_ID = 7001;
	private static final int EVENT_NOTIFICATION_BASE_ID = 8000;
	private static final int FOCUS_PER_LONG_BREAK = 4;

	private static final String PREF_STATE = "pomodoro_service_state";
	private static final String KEY_TIME_LEFT = "time_left";
	private static final String KEY_IS_RUNNING = "is_running";
	private static final String KEY_IS_FOCUS = "is_focus";
	private static final String KEY_COMPLETED_FOCUS = "completed_focus";
	private static final String KEY_SESSION_IN_PROGRESS = "session_in_progress";

	private CountDownTimer countDownTimer;
	private PreferenceHelper preferenceHelper;
	private SharedPreferences statePrefs;
	private MediaPlayer focusMediaPlayer;
	private NotificationManager notificationManager;
	private AudioManager audioManager;
	private AudioFocusRequest audioFocusRequest;
	private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = this::onAudioFocusChanged;
	private long lastPersistElapsedMs;
	private int lastBroadcastSecond = -1;
	private int eventNotificationCounter;
	private int currentMusicResId = -1;

	private long focusTimeMs;
	private long shortBreakTimeMs;
	private long longBreakTimeMs;

	private long timeLeftMs;
	private boolean isRunning;
	private boolean isFocus = true;
	private boolean sessionInProgress;
	private int completedFocusSessions;

	public static final class TimerState {
		public final long timeLeftMs;
		public final boolean isRunning;
		public final boolean isFocus;
		public final int completedFocusSessions;
		public final boolean sessionInProgress;

		TimerState(long timeLeftMs, boolean isRunning, boolean isFocus, int completedFocusSessions, boolean sessionInProgress) {
			this.timeLeftMs = timeLeftMs;
			this.isRunning = isRunning;
			this.isFocus = isFocus;
			this.completedFocusSessions = completedFocusSessions;
			this.sessionInProgress = sessionInProgress;
		}
	}

	public static TimerState readState(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREF_STATE, MODE_PRIVATE);
		long savedTimeLeft = prefs.getLong(KEY_TIME_LEFT, 0L);
		boolean savedRunning = prefs.getBoolean(KEY_IS_RUNNING, false);
		boolean savedFocus = prefs.getBoolean(KEY_IS_FOCUS, true);
		int savedCompleted = prefs.getInt(KEY_COMPLETED_FOCUS, 0);
		boolean savedSession = prefs.getBoolean(KEY_SESSION_IN_PROGRESS, false);

		PreferenceHelper preferenceHelper = new PreferenceHelper(context);
		if (savedTimeLeft <= 0L) {
			int defaultFocusMinutes = Math.max(1, preferenceHelper.getFocusTime());
			savedTimeLeft = defaultFocusMinutes * 60L * 1000L;
		}

		return new TimerState(savedTimeLeft, savedRunning, savedFocus, savedCompleted, savedSession);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		preferenceHelper = new PreferenceHelper(this);
		statePrefs = getSharedPreferences(PREF_STATE, MODE_PRIVATE);

		loadDurations();
		restoreState();
		createNotificationChannel();
		notificationManager = getSystemService(NotificationManager.class);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		startForeground(NOTIFICATION_ID, buildNotification());

		if (isRunning && timeLeftMs > 0L) {
			if (isFocus) {
				setFocusMode(true);
			}
			resumeFocusMusicIfEnabled();
			startTimer();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent != null ? intent.getAction() : null;
		if (action == null) {
			broadcastState(true);
			return START_STICKY;
		}

		switch (action) {
			case ACTION_START:
				handleStart();
				break;
			case ACTION_PAUSE:
				handlePause();
				break;
			case ACTION_RESUME:
				handleResume();
				break;
			case ACTION_STOP:
				handleStop();
				break;
			case ACTION_RESET:
				handleReset();
				break;
				case ACTION_APPLY_SETTINGS:
					handleApplySettings();
					break;
			default:
				break;
		}

		updateForegroundNotification();
		persistState();
		broadcastState(true);
		return START_STICKY;
	}

	private void handleStart() {
		if (isRunning) {
			return;
		}

		loadDurations();
		if (isFocus && !sessionInProgress) {
			SessionManager.startSession(this, "FOCUS", getFocusPlannedMinutes());
			sessionInProgress = true;
			setFocusMode(true);
			notifyPhaseEvent("Bat dau tap trung", "Da bat dau phien pomodoro moi");
		}
		resumeFocusMusicIfEnabled();

		if (timeLeftMs <= 0L) {
			timeLeftMs = isFocus ? focusTimeMs : getCurrentBreakTimeMs();
		}
		startTimer();
	}

	private void handlePause() {
		if (!isRunning) {
			return;
		}

		cancelTimer();
		isRunning = false;
		if (isFocus) {
			setFocusMode(false);
		}
		pauseFocusMusic();
	}

	private void handleResume() {
		if (isRunning) {
			return;
		}

		loadDurations();
		if (isFocus) {
			setFocusMode(true);
		}
		resumeFocusMusicIfEnabled();
		startTimer();
	}

	private void handleStop() {
		cancelTimer();
		if (isFocus && sessionInProgress) {
			SessionManager.recordSession(this, getCurrentFocusDurationMinutes(), false);
		}

		isRunning = false;
		isFocus = true;
		sessionInProgress = false;
		timeLeftMs = focusTimeMs;
		setFocusMode(false);
		stopFocusMusic();
	}

	private void handleReset() {
		cancelTimer();
		isRunning = false;
		timeLeftMs = getCurrentPhaseDurationMs();

		if (isFocus) {
			setFocusMode(false);
			stopFocusMusic();
		}
	}

	private void handleApplySettings() {
		long oldPhaseDuration = getCurrentPhaseDurationMs();
		loadDurations();
		long newPhaseDuration = getCurrentPhaseDurationMs();

		boolean hasProgress = timeLeftMs > 0L && timeLeftMs < oldPhaseDuration;
		if (!isRunning) {
			if (hasProgress) {
				timeLeftMs = Math.min(timeLeftMs, newPhaseDuration);
			} else {
				timeLeftMs = newPhaseDuration;
			}
		}

		if (!preferenceHelper.isMusicEnabled()) {
			stopFocusMusic();
		} else if (isRunning && isFocus) {
			resumeFocusMusicIfEnabled();
		}
	}

	private void startTimer() {
		cancelTimer();
		isRunning = true;

		countDownTimer = new CountDownTimer(timeLeftMs, 1000L) {
			@Override
			public void onTick(long millisUntilFinished) {
				timeLeftMs = millisUntilFinished;
				throttledPersistState();
				updateForegroundNotification();
				broadcastState(false);
			}

			@Override
			public void onFinish() {
				onPhaseFinished();
			}
		}.start();
	}

	private void onPhaseFinished() {
		isRunning = false;

		if (isFocus) {
			if (sessionInProgress) {
				SessionManager.recordSession(this, getFocusPlannedMinutes(), true);
			}
			completedFocusSessions++;
			sessionInProgress = false;
			setFocusMode(false);
			notifyPhaseEvent("Hoan thanh phien", "Ban vua hoan thanh mot phien tap trung");

			isFocus = false;
			timeLeftMs = getCurrentBreakTimeMs();
			notifyPhaseEvent("Bat dau nghi", "Den gio nghi ngoi de phuc hoi");
			resumeFocusMusicIfEnabled();
			startTimer();
		} else {
			isFocus = true;
			SessionManager.startSession(this, "FOCUS", getFocusPlannedMinutes());
			sessionInProgress = true;
			setFocusMode(true);
			resumeFocusMusicIfEnabled();
			notifyPhaseEvent("Bat dau tap trung", "Bat dau phien tiep theo");

			timeLeftMs = focusTimeMs;
			startTimer();
		}

		persistState();
		updateForegroundNotification();
		broadcastState(true);
	}

	private void loadDurations() {
		int focusMinutes = Math.max(1, preferenceHelper.getFocusTime());
		int breakMinutes = Math.max(1, preferenceHelper.getBreakTime());
		int longBreakMinutes = Math.max(1, preferenceHelper.getLongBreakTime());

		focusTimeMs = focusMinutes * 60L * 1000L;
		shortBreakTimeMs = breakMinutes * 60L * 1000L;
		longBreakTimeMs = longBreakMinutes * 60L * 1000L;
	}

	private long getCurrentBreakTimeMs() {
		if (completedFocusSessions > 0 && completedFocusSessions % FOCUS_PER_LONG_BREAK == 0) {
			return longBreakTimeMs;
		}
		return shortBreakTimeMs;
	}

	private long getCurrentPhaseDurationMs() {
		return isFocus ? focusTimeMs : getCurrentBreakTimeMs();
	}

	private void setFocusMode(boolean active) {
		preferenceHelper.setFocusActive(active);

		Intent blockerIntent = new Intent(this, BlockerService.class);
		if (active) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				ContextCompat.startForegroundService(this, blockerIntent);
			} else {
				startService(blockerIntent);
			}
		} else {
			stopService(blockerIntent);
		}
	}

	private void resumeFocusMusicIfEnabled() {
		if (!preferenceHelper.isMusicEnabled()) {
			abandonMusicFocus();
			return;
		}
		int selectedMusicResId = getSelectedMusicResId();
		if (!requestMusicFocus()) {
			return;
		}
		if (focusMediaPlayer != null) {
			if (currentMusicResId != selectedMusicResId) {
				stopFocusMusic();
			} else {
				if (!focusMediaPlayer.isPlaying()) {
					focusMediaPlayer.start();
				}
				return;
			}
		}

		focusMediaPlayer = MediaPlayer.create(this, selectedMusicResId);
		currentMusicResId = selectedMusicResId;
		if (focusMediaPlayer != null) {
			focusMediaPlayer.setLooping(true);
			focusMediaPlayer.setOnErrorListener((mp, what, extra) -> {
				stopFocusMusic();
				return true;
			});
			focusMediaPlayer.start();
		}
	}

	private void pauseFocusMusic() {
		if (focusMediaPlayer != null && focusMediaPlayer.isPlaying()) {
			focusMediaPlayer.pause();
		}
	}

	private void stopFocusMusic() {
		if (focusMediaPlayer != null) {
			focusMediaPlayer.setOnErrorListener(null);
			focusMediaPlayer.release();
			focusMediaPlayer = null;
		}
		currentMusicResId = -1;
		abandonMusicFocus();
	}

	private boolean requestMusicFocus() {
		if (audioManager == null) {
			return true;
		}

		int result;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (audioFocusRequest == null) {
				audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
						.setAudioAttributes(new AudioAttributes.Builder()
								.setUsage(AudioAttributes.USAGE_MEDIA)
								.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
								.build())
						.setAcceptsDelayedFocusGain(true)
						.setOnAudioFocusChangeListener(audioFocusChangeListener)
						.build();
			}
			result = audioManager.requestAudioFocus(audioFocusRequest);
		} else {
			result = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		}

		return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
	}

	private void abandonMusicFocus() {
		if (audioManager == null) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (audioFocusRequest != null) {
				audioManager.abandonAudioFocusRequest(audioFocusRequest);
			}
		} else {
			audioManager.abandonAudioFocus(audioFocusChangeListener);
		}
	}

	private void onAudioFocusChanged(int focusChange) {
		if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
			pauseFocusMusic();
		} else if (focusChange == AudioManager.AUDIOFOCUS_GAIN && isRunning && isFocus && preferenceHelper.isMusicEnabled()) {
			resumeFocusMusicIfEnabled();
		}
	}

	private void cancelTimer() {
		if (countDownTimer != null) {
			countDownTimer.cancel();
			countDownTimer = null;
		}
	}

	private void restoreState() {
		TimerState state = readState(this);
		timeLeftMs = state.timeLeftMs;
		isRunning = state.isRunning;
		isFocus = state.isFocus;
		completedFocusSessions = state.completedFocusSessions;
		sessionInProgress = state.sessionInProgress;
	}

	private void persistState() {
		statePrefs.edit()
				.putLong(KEY_TIME_LEFT, timeLeftMs)
				.putBoolean(KEY_IS_RUNNING, isRunning)
				.putBoolean(KEY_IS_FOCUS, isFocus)
				.putInt(KEY_COMPLETED_FOCUS, completedFocusSessions)
				.putBoolean(KEY_SESSION_IN_PROGRESS, sessionInProgress)
				.apply();
		lastPersistElapsedMs = SystemClock.elapsedRealtime();
	}

	private void throttledPersistState() {
		long now = SystemClock.elapsedRealtime();
		if (now - lastPersistElapsedMs >= 5000L) {
			persistState();
		}
	}

	private void broadcastState(boolean force) {
		int currentSecond = (int) (timeLeftMs / 1000L);
		if (!force && isRunning && currentSecond == lastBroadcastSecond) {
			return;
		}
		lastBroadcastSecond = currentSecond;

		Intent broadcast = new Intent(ACTION_STATE);
		broadcast.setPackage(getPackageName());
		broadcast.putExtra(EXTRA_TIME_LEFT, timeLeftMs);
		broadcast.putExtra(EXTRA_IS_RUNNING, isRunning);
		broadcast.putExtra(EXTRA_IS_FOCUS, isFocus);
		broadcast.putExtra(EXTRA_COMPLETED_FOCUS, completedFocusSessions);
		broadcast.putExtra(EXTRA_SESSION_IN_PROGRESS, sessionInProgress);
		sendBroadcast(broadcast);
	}

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			return;
		}

		NotificationChannel channel = new NotificationChannel(
				CHANNEL_ID,
				"Pomodoro Timer",
				NotificationManager.IMPORTANCE_LOW
		);
		NotificationManager manager = getSystemService(NotificationManager.class);
		if (manager != null) {
			manager.createNotificationChannel(channel);

			NotificationChannel eventChannel = new NotificationChannel(
					EVENT_CHANNEL_ID,
					"Pomodoro Events",
					NotificationManager.IMPORTANCE_LOW
			);
			eventChannel.setSound(null, (AudioAttributes) null);
			eventChannel.enableVibration(false);
			eventChannel.setVibrationPattern(new long[]{0L});
			manager.createNotificationChannel(eventChannel);
		}
	}

	private void notifyPhaseEvent(String title, String message) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(title)
				.setContentText(message)
				.setAutoCancel(true)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.setDefaults(0)
				.setVibrate(new long[]{0L})
				.setSound(null)
				.setSilent(true);

		NotificationManager manager = getSystemService(NotificationManager.class);
		if (manager != null) {
			manager.notify(EVENT_NOTIFICATION_BASE_ID + (eventNotificationCounter++ % 50), builder.build());
		}
	}

	private void updateForegroundNotification() {
		Notification notification = buildNotification();
		if (notificationManager != null) {
			notificationManager.notify(NOTIFICATION_ID, notification);
		} else {
			startForeground(NOTIFICATION_ID, notification);
		}
	}

	private Notification buildNotification() {
		String phase = isFocus ? "Tap trung" : "Nghi";
		String timerText = formatTime(timeLeftMs);
		String content = phase + " - " + timerText;

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle("Pomodoro dang chay")
				.setContentText(content)
				.setOnlyAlertOnce(true)
				.setOngoing(true)
				.setPriority(NotificationCompat.PRIORITY_LOW);

		if (isRunning) {
			builder.addAction(0, "Pause", actionIntent(ACTION_PAUSE));
		} else {
			builder.addAction(0, "Start", actionIntent(sessionInProgress ? ACTION_RESUME : ACTION_START));
		}

		builder.addAction(0, "Stop", actionIntent(ACTION_STOP));
		builder.addAction(0, "Reset", actionIntent(ACTION_RESET));

		return builder.build();
	}

	private PendingIntent actionIntent(String action) {
		Intent intent = new Intent(this, PomodoroService.class);
		intent.setAction(action);

		int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

		return PendingIntent.getService(this, action.hashCode(), intent, flags);
	}

	private String formatTime(long millis) {
		int seconds = (int) (millis / 1000L);
		int minutesPart = seconds / 60;
		int secondsPart = seconds % 60;
		return String.format(Locale.getDefault(), "%02d:%02d", minutesPart, secondsPart);
	}

	private int getFocusPlannedMinutes() {
		return (int) Math.max(1L, focusTimeMs / 60000L);
	}

	private int getCurrentFocusDurationMinutes() {
		int plannedMinutes = Math.max(1, preferenceHelper.getSessionPlannedDuration());
		long plannedMs = plannedMinutes * 60_000L;
		long elapsedMs = Math.max(0L, plannedMs - Math.max(0L, timeLeftMs));
		return (int) (elapsedMs / 60_000L);
	}

	private int getSelectedMusicResId() {
		String playlistId = preferenceHelper.getSelectedPlaylistId();
		if (playlistId == null) {
			return R.raw.lofi1;
		}

		switch (playlistId) {
			case "classical_focus":
				return R.raw.lofi2;
			case "nature_sounds":
				return R.raw.lofi3;
			case "ambient_study":
				return R.raw.lofi4;
			case "lofi_beats":
			default:
				return R.raw.lofi1;
		}
	}

	@Override
	public void onDestroy() {
		cancelTimer();
		stopFocusMusic();
		setFocusMode(false);
		super.onDestroy();
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
