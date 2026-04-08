package com.example.deepfocustodo.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

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
	private int eventNotificationCounter;

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
		startForeground(NOTIFICATION_ID, buildNotification());

		if (isRunning && timeLeftMs > 0L) {
			if (isFocus) {
				setFocusMode(true);
				startFocusMusicIfEnabled();
			}
			startTimer();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent != null ? intent.getAction() : null;
		if (action == null) {
			broadcastState();
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
		broadcastState();
		return START_STICKY;
	}

	private void handleStart() {
		if (isRunning) {
			return;
		}

		loadDurations();
		if (isFocus && !sessionInProgress) {
			SessionManager.startSession(this, "FOCUS", (int)(focusTimeMs / 60000));
			sessionInProgress = true;
			setFocusMode(true);
			startFocusMusicIfEnabled();
			notifyPhaseEvent("Bắt đầu tập trung", "Đã bắt đầu phiên pomodoro mới");
		}

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
			stopFocusMusic();
		}
	}

	private void handleResume() {
		if (isRunning) {
			return;
		}

		loadDurations();
		if (isFocus) {
			setFocusMode(true);
			startFocusMusicIfEnabled();
		}
		startTimer();
	}

	private void handleStop() {
		cancelTimer();
		if (sessionInProgress) {
            long durationMs = isFocus ? (focusTimeMs - timeLeftMs) : (getCurrentBreakTimeMs() - timeLeftMs);
            int durationMins = (int)(durationMs / 60000);
			SessionManager.recordSession(this, Math.max(0, durationMins), false);
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
			startFocusMusicIfEnabled();
		}
	}

	private void startTimer() {
		cancelTimer();
		isRunning = true;

		countDownTimer = new CountDownTimer(timeLeftMs, 1000L) {
			@Override
			public void onTick(long millisUntilFinished) {
				timeLeftMs = millisUntilFinished;
				persistState();
				updateForegroundNotification();
				broadcastState();
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
				SessionManager.recordSession(this, (int)(focusTimeMs / 60000), true);
			}
			completedFocusSessions++;
			sessionInProgress = false;
			setFocusMode(false);
			stopFocusMusic();
			notifyPhaseEvent("Hoàn thành phiên", "Bạn vừa hoàn thành một phiên tập trung");

			isFocus = false;
			timeLeftMs = getCurrentBreakTimeMs();
            
            SessionManager.startSession(this, completedFocusSessions % FOCUS_PER_LONG_BREAK == 0 ? "LONG_BREAK" : "SHORT_BREAK", (int)(timeLeftMs / 60000));
            sessionInProgress = true;
            
			notifyPhaseEvent("Bắt đầu nghỉ", "Đến giờ nghỉ ngơi để phục hồi");
			startTimer();
		} else {
            if (sessionInProgress) {
                SessionManager.recordSession(this, (int)(getCurrentBreakTimeMs() / 60000), true);
            }
            
			isFocus = true;
			SessionManager.startSession(this, "FOCUS", (int)(focusTimeMs / 60000));
			sessionInProgress = true;
			setFocusMode(true);
			startFocusMusicIfEnabled();
			notifyPhaseEvent("Bắt đầu tập trung", "Bắt đầu phiên tiếp theo");

			timeLeftMs = focusTimeMs;
			startTimer();
		}

		persistState();
		updateForegroundNotification();
		broadcastState();
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

        // Fix: Use Class reference instead of String to avoid constructor error
		Intent blockerIntent = new Intent(this, com.example.deepfocustodo.services.BlockerService.class);
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

	private void startFocusMusicIfEnabled() {
		if (!preferenceHelper.isMusicEnabled()) {
			return;
		}
		if (focusMediaPlayer != null) {
			return;
		}
        
        try {
            focusMediaPlayer = MediaPlayer.create(this, R.raw.stop_right_there);
            if (focusMediaPlayer != null) {
                focusMediaPlayer.setLooping(true);
                focusMediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	private void stopFocusMusic() {
		if (focusMediaPlayer != null) {
			focusMediaPlayer.release();
			focusMediaPlayer = null;
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
	}

	private void broadcastState() {
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

		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pomodoro Timer",
                    NotificationManager.IMPORTANCE_LOW
            );
			manager.createNotificationChannel(channel);

			NotificationChannel eventChannel = new NotificationChannel(
					EVENT_CHANNEL_ID,
					"Pomodoro Events",
					NotificationManager.IMPORTANCE_DEFAULT
			);
			manager.createNotificationChannel(eventChannel);
		}
	}

	private void notifyPhaseEvent(String title, String message) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle(title)
				.setContentText(message)
				.setAutoCancel(true)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT);

		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (manager != null) {
			manager.notify(EVENT_NOTIFICATION_BASE_ID + (eventNotificationCounter++ % 50), builder.build());
		}
	}

	private void updateForegroundNotification() {
		Notification notification = buildNotification();
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
	}

	private Notification buildNotification() {
		String phase = isFocus ? "Tập trung" : "Nghỉ";
		String timerText = formatTime(timeLeftMs);
		String content = phase + " - " + timerText;

		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle("Pomodoro đang chạy")
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

		int flags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			flags |= PendingIntent.FLAG_IMMUTABLE;
		}

		return PendingIntent.getService(this, action.hashCode(), intent, flags);
	}

	private String formatTime(long millis) {
		int seconds = (int) (millis / 1000L);
		int minutesPart = seconds / 60;
		int secondsPart = seconds % 60;
		return String.format(Locale.getDefault(), "%02d:%02d", minutesPart, secondsPart);
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