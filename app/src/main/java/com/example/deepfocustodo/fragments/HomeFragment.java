package com.example.deepfocustodo.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.activities.HistoryActivity;
import com.example.deepfocustodo.adapters.TaskAdapter;
import com.example.deepfocustodo.database.AppDatabase;
import com.example.deepfocustodo.database.StatsRepository;
import com.example.deepfocustodo.models.Task;
import com.example.deepfocustodo.services.PomodoroService;
import com.example.deepfocustodo.utils.AppExecutors;
import com.example.deepfocustodo.utils.PreferenceHelper;
import com.example.deepfocustodo.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements TaskAdapter.OnTaskClickListener, TabRefreshable {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2001;
    private static final int FOCUS_SESSIONS_PER_CYCLE = 4;

    private Button btnStart, btnPause, btnStop, btnReset, btnViewHistory;
    private TextView tvTimer, tvMode, tvHomePoints, tvCurrentTaskName;
    private RecyclerView recyclerTasks;
    private MaterialCardView cardSelectedTask;
    private ImageButton btnCancelTask;

    private AppDatabase db;
    private PreferenceHelper preferenceHelper;
    private TaskAdapter taskAdapter;

    private long focusTimeMs, shortBreakTimeMs, longBreakTimeMs;
    private long timeLeftMs;
    private boolean isRunning, isFocus = true, sessionInProgress;
    private int completedFocusSessions;
    private boolean receiverRegistered;

    private final BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!PomodoroService.ACTION_STATE.equals(intent.getAction())) return;

            timeLeftMs = intent.getLongExtra(PomodoroService.EXTRA_TIME_LEFT, timeLeftMs);
            isRunning = intent.getBooleanExtra(PomodoroService.EXTRA_IS_RUNNING, isRunning);
            isFocus = intent.getBooleanExtra(PomodoroService.EXTRA_IS_FOCUS, isFocus);
            completedFocusSessions = intent.getIntExtra(PomodoroService.EXTRA_COMPLETED_FOCUS, completedFocusSessions);
            sessionInProgress = intent.getBooleanExtra(PomodoroService.EXTRA_SESSION_IN_PROGRESS, sessionInProgress);

            updateTimerText();
            updateModeText();
            updateButtonStates();
            updateSelectedTaskUI();
        }
    };

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnStart = view.findViewById(R.id.btnStart);
        btnPause = view.findViewById(R.id.btnPause);
        btnStop = view.findViewById(R.id.btnStop);
        btnReset = view.findViewById(R.id.btnReset);
        btnViewHistory = view.findViewById(R.id.btnViewHistory);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvMode = view.findViewById(R.id.tvMode);
        tvHomePoints = view.findViewById(R.id.tvHomePoints);
        tvCurrentTaskName = view.findViewById(R.id.tvCurrentTaskName);
        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        cardSelectedTask = view.findViewById(R.id.cardSelectedTask);
        btnCancelTask = view.findViewById(R.id.btnCancelTask);

        db = AppDatabase.getInstance(requireContext());
        preferenceHelper = new PreferenceHelper(requireContext());

        requestNotificationPermissionIfNeeded();
        loadDurations();
        loadStateFromService();

        setupTaskList();
        loadTasks();
        updateTimerText();
        updateModeText();
        updateButtonStates();
        updateSelectedTaskUI();

        btnStart.setOnClickListener(v -> dispatchServiceAction(PomodoroService.ACTION_START));
        btnPause.setOnClickListener(v -> dispatchServiceAction(isRunning ? PomodoroService.ACTION_PAUSE : PomodoroService.ACTION_RESUME));
        btnStop.setOnClickListener(v -> dispatchServiceAction(PomodoroService.ACTION_STOP));
        btnReset.setOnClickListener(v -> dispatchServiceAction(PomodoroService.ACTION_RESET));
        btnViewHistory.setOnClickListener(v -> startActivity(new Intent(requireContext(), HistoryActivity.class)));
        btnCancelTask.setOnClickListener(v -> {
            SessionManager.clearSelectedTask(requireContext());
            updateSelectedTaskUI();
            loadTasks();
        });
    }

    private void updateSelectedTaskUI() {
        Integer selectedId = SessionManager.getSelectedTaskId(requireContext());
        if (selectedId == null) {
            cardSelectedTask.setVisibility(View.GONE);
        } else {
            AppExecutors.diskIO().execute(() -> {
                Task task = db.taskDao().getTaskById(selectedId);
                AppExecutors.mainThread(() -> {
                    if (!isAdded()) return;
                    if (task != null) {
                        tvCurrentTaskName.setText(task.getTitle());
                        cardSelectedTask.setVisibility(View.VISIBLE);
                        btnCancelTask.setVisibility(sessionInProgress ? View.INVISIBLE : View.VISIBLE);
                    } else {
                        SessionManager.clearSelectedTask(requireContext());
                        cardSelectedTask.setVisibility(View.GONE);
                    }
                });
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        registerTimerReceiver();
        loadDurations();
        loadStateFromService();
        updateTimerText();
        updateModeText();
        updateButtonStates();
        updateSelectedTaskUI();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterTimerReceiver();
    }

    @Override
    public void onResume() {
        super.onResume();
        onTabSelected();
        refreshPointsUI();
    }

    private void refreshPointsUI() {
        if (!isAdded()) return;
        AppExecutors.diskIO().execute(() -> {
            try {
                Context context = getContext();
                if (context == null) return;
                StatsRepository repo = new StatsRepository(context);
                int todayPoints = repo.getPointsToday();
                int totalPoints = repo.getTotalPoints();

                AppExecutors.mainThread(() -> {
                    if (isAdded() && tvHomePoints != null) {
                        tvHomePoints.setText(String.format(Locale.getDefault(), 
                            "Today: %d pts | Total: %d pts", todayPoints, totalPoints));
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onTabSelected() {
        if (!isAdded() || getView() == null) return;
        loadDurations();
        loadStateFromService();
        loadTasks();
        updateTimerText();
        updateModeText();
        updateButtonStates();
        updateSelectedTaskUI();
    }

    private void registerTimerReceiver() {
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter(PomodoroService.ACTION_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(timerReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void unregisterTimerReceiver() {
        if (!receiverRegistered) return;
        requireContext().unregisterReceiver(timerReceiver);
        receiverRegistered = false;
    }

    private void dispatchServiceAction(@Nullable String action) {
        Intent intent = new Intent(requireContext(), PomodoroService.class);
        if (action != null) intent.setAction(action);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && (PomodoroService.ACTION_START.equals(action) || PomodoroService.ACTION_RESUME.equals(action))) {
            ContextCompat.startForegroundService(requireContext(), intent);
        } else {
            requireContext().startService(intent);
        }
    }

    private void loadStateFromService() {
        PomodoroService.TimerState state = PomodoroService.readState(requireContext());
        timeLeftMs = state.timeLeftMs;
        isRunning = state.isRunning;
        isFocus = state.isFocus;
        completedFocusSessions = state.completedFocusSessions;
        sessionInProgress = state.sessionInProgress;

        long phaseDuration = isFocus ? focusTimeMs : getCurrentBreakTimeMs();
        boolean hasProgress = timeLeftMs > 0L && timeLeftMs < phaseDuration;

        if (timeLeftMs <= 0L || (!isRunning && !hasProgress)) {
            timeLeftMs = phaseDuration;
        } else if (!isRunning && hasProgress) {
            timeLeftMs = Math.min(timeLeftMs, phaseDuration);
        }
    }

    private void setupTaskList() {
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        taskAdapter = new TaskAdapter(this);
        recyclerTasks.setAdapter(taskAdapter);
    }

    private void loadTasks() {
        AppExecutors.diskIO().execute(() -> {
            try {
                List<Task> tasks = db.taskDao().getTasksByStatus(false);
                AppExecutors.mainThread(() -> {
                    if (isAdded()) {
                        taskAdapter.setTasks(tasks, requireContext());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
        if (completedFocusSessions > 0 && completedFocusSessions % FOCUS_SESSIONS_PER_CYCLE == 0) {
            return longBreakTimeMs;
        }
        return shortBreakTimeMs;
    }

    private void updateTimerText() {
        int totalSeconds = (int) (timeLeftMs / 1000L);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void updateModeText() {
        if (!isAdded()) return;
        boolean isLongBreak = !isFocus && getCurrentBreakTimeMs() == longBreakTimeMs;
        tvMode.setText(isFocus ? "TẬP TRUNG" : (isLongBreak ? "NGHỈ DÀI" : "NGHỈ NGẮN"));
        int colorRes = isFocus ? android.R.color.holo_red_light : android.R.color.holo_green_light;
        tvMode.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    private void updateButtonStates() {
        long phaseDuration = isFocus ? focusTimeMs : getCurrentBreakTimeMs();
        boolean hasProgress = timeLeftMs > 0L && timeLeftMs < phaseDuration;
        boolean canResume = !isRunning && timeLeftMs > 0L && hasProgress;

        btnStart.setEnabled(!isRunning && !sessionInProgress && !hasProgress);
        btnPause.setEnabled(isRunning || canResume);
        btnPause.setText(isRunning ? "Tạm dừng" : "Tiếp tục");
        btnStop.setEnabled(isRunning || sessionInProgress || hasProgress);
        btnReset.setEnabled(!isRunning && hasProgress);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onTaskCheckChanged(Task task, boolean isChecked) {
        AppExecutors.diskIO().execute(() -> {
            task.setCompleted(isChecked);
            db.taskDao().updateTask(task);
            AppExecutors.mainThread(() -> {
                if (isAdded()) loadTasks();
            });
        });
    }

    @Override
    public void onDeleteClick(Task task) {
        final int taskId = task.getId();
        AppExecutors.diskIO().execute(() -> {
            db.taskDao().deleteTask(task);
            SessionManager.clearIfSelected(requireContext(), taskId);
            AppExecutors.mainThread(() -> {
                if (isAdded()) {
                    loadTasks();
                    updateSelectedTaskUI();
                }
            });
        });
    }

    @Override
    public void onTaskClick(Task task) {
        if (sessionInProgress) {
            Toast.makeText(requireContext(), "Cannot change task during session", Toast.LENGTH_SHORT).show();
            return;
        }
        SessionManager.setSelectedTaskId(requireContext(), task.getId());
        updateSelectedTaskUI();
        taskAdapter.notifyDataSetChanged();
    }

    @Override
    public void onTaskLongClick(Task task) {
        onTaskClick(task);
    }
}