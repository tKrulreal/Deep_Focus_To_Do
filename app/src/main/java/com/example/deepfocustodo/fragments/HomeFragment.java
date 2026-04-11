package com.example.deepfocustodo.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.activities.HistoryActivity;
import com.example.deepfocustodo.adapters.TaskAdapter;
import com.example.deepfocustodo.database.AppDatabase;
import com.example.deepfocustodo.models.Task;
import com.example.deepfocustodo.services.PomodoroService;
import com.example.deepfocustodo.utils.AppExecutors;
import com.example.deepfocustodo.utils.PreferenceHelper;
import com.example.deepfocustodo.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements TabRefreshable {

    private static final String TAG = "HomeFragment";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2001;
    private static final int FOCUS_SESSIONS_PER_CYCLE = 4;

    private Button btnStart;
    private Button btnPause;
    private Button btnStop;
    private Button btnViewHistory;
    private ImageButton btnCancelTask;
    private TextView tvTimer;
    private TextView tvMode;
    private TextView tvHomePoints;
    private TextView tvCurrentTaskName;
    private RecyclerView recyclerTasks;
    private CardView cardSelectedTask;

    private AppDatabase db;
    private PreferenceHelper preferenceHelper;
    private TaskAdapter taskAdapter;

    private long focusTimeMs;
    private long shortBreakTimeMs;
    private long longBreakTimeMs;

    private long timeLeftMs;
    private boolean isRunning;
    private boolean isFocus = true;
    private boolean sessionInProgress;
    private int completedFocusSessions;

    private boolean receiverRegistered;

    private final TaskAdapter.OnTaskClickListener taskClickListener = new TaskAdapter.OnTaskClickListener() {
        @Override
        public void onTaskCheckChanged(Task task, boolean isChecked) {
            HomeFragment.this.onTaskCheckChanged(task, isChecked);
        }

        @Override
        public void onDeleteClick(Task task) {
            HomeFragment.this.onDeleteClick(task);
        }

        @Override
        public void onTaskClick(Task task) {
            HomeFragment.this.onTaskClick(task);
        }

        @Override
        public void onTaskLongClick(Task task) {
            HomeFragment.this.onTaskLongClick(task);
        }
    };

    private final BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!PomodoroService.ACTION_STATE.equals(intent.getAction())) {
                return;
            }

            boolean previousIsFocus = isFocus;
            int previousCompletedFocus = completedFocusSessions;

            timeLeftMs = intent.getLongExtra(PomodoroService.EXTRA_TIME_LEFT, timeLeftMs);
            isRunning = intent.getBooleanExtra(PomodoroService.EXTRA_IS_RUNNING, isRunning);
            isFocus = intent.getBooleanExtra(PomodoroService.EXTRA_IS_FOCUS, isFocus);
            completedFocusSessions = intent.getIntExtra(PomodoroService.EXTRA_COMPLETED_FOCUS, completedFocusSessions);
            sessionInProgress = intent.getBooleanExtra(PomodoroService.EXTRA_SESSION_IN_PROGRESS, sessionInProgress);

            boolean justCompletedFocus = previousIsFocus && !isFocus && completedFocusSessions > previousCompletedFocus;
            if (justCompletedFocus) {
                loadHeaderInfo();
            }

            updateTimerText();
            updateModeText();
            updateButtonStates();
        }
    };

    private final BroadcastReceiver goalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SessionManager.ACTION_TASK_REACHED_GOAL.equals(intent.getAction())) {
                int taskId = intent.getIntExtra(SessionManager.EXTRA_TASK_ID, -1);
                if (taskId != -1) {
                    showTaskGoalReachedDialog(taskId);
                }
            }
        }
    };

    public HomeFragment() {
    }

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull android.view.LayoutInflater inflater,
                                          @Nullable android.view.ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnStart = view.findViewById(R.id.btnStart);
        btnPause = view.findViewById(R.id.btnPause);
        btnStop = view.findViewById(R.id.btnStop);
        btnViewHistory = view.findViewById(R.id.btnViewHistory);
        btnCancelTask = view.findViewById(R.id.btnCancelTask);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvMode = view.findViewById(R.id.tvMode);
        tvHomePoints = view.findViewById(R.id.tvHomePoints);
        tvCurrentTaskName = view.findViewById(R.id.tvCurrentTaskName);
        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        cardSelectedTask = view.findViewById(R.id.cardSelectedTask);

        db = AppDatabase.getInstance(requireContext());
        preferenceHelper = new PreferenceHelper(requireContext());

        requestNotificationPermissionIfNeeded();
        loadDurations();
        loadStateFromService();

        setupTaskList();
        loadTasks();
        loadHeaderInfo();
        updateTimerText();
        updateModeText();
        updateButtonStates();

        btnStart.setOnClickListener(v -> handleStartClick());
        btnPause.setOnClickListener(v -> dispatchServiceAction(isRunning ? PomodoroService.ACTION_PAUSE : PomodoroService.ACTION_RESUME));
        btnStop.setOnClickListener(v -> dispatchServiceAction(PomodoroService.ACTION_STOP));
        btnViewHistory.setOnClickListener(v -> startActivity(new Intent(requireContext(), HistoryActivity.class)));
        btnCancelTask.setOnClickListener(v -> {
            SessionManager.clearSelectedTask(requireContext());
            loadTasks();
            loadHeaderInfo();
        });
    }

    private void handleStartClick() {
        Integer selectedTaskId = SessionManager.getSelectedTaskId(requireContext());
        boolean hasIncompleteTasks = taskAdapter != null && taskAdapter.getItemCount() > 0;

        if (selectedTaskId == null && hasIncompleteTasks) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Bắt đầu tập trung")
                    .setMessage("Bạn chưa chọn nhiệm vụ cho phiên này. Bạn có muốn chọn một nhiệm vụ từ danh sách không?")
                    .setIcon(R.drawable.baseline_info_outline_24)
                    .setNeutralButton("Để sau", (dialog, which) -> {
                        Log.w(TAG, "User started session without task");
                        dispatchServiceAction(PomodoroService.ACTION_START);
                    })
                    .setNegativeButton("Bắt đầu luôn", (dialog, which) -> {
                        dispatchServiceAction(PomodoroService.ACTION_START);
                    })
                    .setPositiveButton("Chọn nhiệm vụ", (dialog, which) -> {
                        if (recyclerTasks != null) {
                            recyclerTasks.smoothScrollToPosition(0);
                            Toast.makeText(requireContext(), "Vui lòng chọn một nhiệm vụ dưới đây", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .show();
        } else {
            dispatchServiceAction(PomodoroService.ACTION_START);
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
    }

    @Override
    public void onTabSelected() {
        if (!isAdded() || getView() == null) {
            return;
        }

        loadDurations();
        loadStateFromService();
        loadTasks();
        loadHeaderInfo();
        updateTimerText();
        updateModeText();
        updateButtonStates();
    }

    private void registerTimerReceiver() {
        if (receiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(PomodoroService.ACTION_STATE);
        ContextCompat.registerReceiver(requireContext(), timerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        IntentFilter goalFilter = new IntentFilter(SessionManager.ACTION_TASK_REACHED_GOAL);
        ContextCompat.registerReceiver(requireContext(), goalReceiver, goalFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        receiverRegistered = true;
    }

    private void unregisterTimerReceiver() {
        if (!receiverRegistered) {
            return;
        }
        requireContext().unregisterReceiver(timerReceiver);
        requireContext().unregisterReceiver(goalReceiver);
        receiverRegistered = false;
    }

    private void dispatchServiceAction(@Nullable String action) {
        Intent intent = new Intent(requireContext(), PomodoroService.class);
        if (action != null) {
            intent.setAction(action);
        }

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
        }
    }

    private void setupTaskList() {
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        taskAdapter = new TaskAdapter(taskClickListener);
        recyclerTasks.setAdapter(taskAdapter);
    }

    private void loadTasks() {
        AppExecutors.diskIO().execute(() -> {
            List<Task> tasks = db.taskDao().getTasksByStatus(false);
            if (!isAdded()) {
                return;
            }
            AppExecutors.mainThread(() -> {
                if (!isAdded()) {
                    return;
                }
                taskAdapter.setTasks(tasks, requireContext());
            });
        });
    }

    private void loadHeaderInfo() {
        Context appContext = requireContext().getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            Integer totalPoints = db.focusSessionDao().getTotalPoints();
            Integer selectedTaskId = SessionManager.getSelectedTaskId(appContext);
            Task selectedTask = selectedTaskId != null ? db.taskDao().getTaskById(selectedTaskId) : null;

            AppExecutors.mainThread(() -> {
                if (!isAdded()) {
                    return;
                }
                tvHomePoints.setText(String.format(Locale.getDefault(), "Points: %d", totalPoints != null ? totalPoints : 0));

                if (selectedTask != null && !selectedTask.isCompleted()) {
                    cardSelectedTask.setVisibility(android.view.View.VISIBLE);
                    tvCurrentTaskName.setText(selectedTask.getTitle());
                } else {
                    cardSelectedTask.setVisibility(android.view.View.GONE);
                }
            });
        });
    }

    private void loadDurations() {
        int focusMinutes = Math.max(1, preferenceHelper.getFocusTime());
        int breakMinutes = Math.max(1, preferenceHelper.getBreakTime());
        int longBreakTimeMinutes = Math.max(1, preferenceHelper.getLongBreakTime());

        focusTimeMs = focusMinutes * 60L * 1000L;
        shortBreakTimeMs = breakMinutes * 60L * 1000L;
        longBreakTimeMs = longBreakTimeMinutes * 60L * 1000L;
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
        boolean isLongBreak = !isFocus && getCurrentBreakTimeMs() == longBreakTimeMs;
        tvMode.setText(isFocus ? "TẬP TRUNG" : (isLongBreak ? "NGHỈ DÀI" : "NGHỈ NGẮN"));
        int colorRes = isFocus ? android.R.color.holo_red_light : android.R.color.holo_green_light;
        tvMode.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    private void updateButtonStates() {
        long phaseDuration = isFocus ? focusTimeMs : getCurrentBreakTimeMs();
        boolean hasProgress = timeLeftMs > 0L && timeLeftMs < phaseDuration;
        boolean canResume = !isRunning && sessionInProgress && hasProgress;

        if (btnCancelTask != null) {
            btnCancelTask.setVisibility((isRunning || canResume) ? View.GONE : View.VISIBLE);
        }

        if (isRunning) {
            btnStart.setVisibility(android.view.View.GONE);
            btnPause.setVisibility(android.view.View.VISIBLE);
            btnStop.setVisibility(android.view.View.VISIBLE);
            btnPause.setText("Tạm dừng");
            btnPause.setEnabled(true);
            btnStop.setEnabled(true);
            return;
        }

        if (canResume) {
            btnStart.setVisibility(android.view.View.GONE);
            btnPause.setVisibility(android.view.View.VISIBLE);
            btnStop.setVisibility(android.view.View.VISIBLE);
            btnPause.setText("Tiếp tục");
            btnPause.setEnabled(true);
            btnStop.setEnabled(true);
            return;
        }

        btnStart.setVisibility(android.view.View.VISIBLE);
        btnPause.setVisibility(android.view.View.GONE);
        btnStop.setVisibility(android.view.View.GONE);
        btnStart.setEnabled(true);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
    }

    private void onTaskCheckChanged(Task task, boolean isChecked) {
        Context appContext = requireContext().getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            long now = System.currentTimeMillis();
            task.setCompleted(isChecked);
            task.setUpdatedAt(now);
            task.setCompletedAt(isChecked ? now : 0L);
            db.taskDao().updateTask(task);
            if (isChecked) {
                SessionManager.clearIfSelected(appContext, task.getId());
            }
            loadTasks();
            loadHeaderInfo();
        });
    }

    private void onDeleteClick(Task task) {
        Context appContext = requireContext().getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            db.taskDao().deleteTask(task);
            Integer selected = SessionManager.getSelectedTaskId(appContext);
            if (selected != null && selected == task.getId()) {
                SessionManager.clearSelectedTask(appContext);
            }
            loadTasks();
            loadHeaderInfo();
        });
    }

    private void onTaskClick(Task task) {
        if (isSessionActive()) {
            Toast.makeText(requireContext(), "Vui lòng ấn Dừng (Stop) phiên hiện tại để chọn nhiệm vụ khác!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (task.isCompleted()) {
            Toast.makeText(requireContext(), "Không thể chọn task đã hoàn thành", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer currentSelectedTaskId = SessionManager.getSelectedTaskId(requireContext());
        if (currentSelectedTaskId != null && currentSelectedTaskId.equals(task.getId())) {
            SessionManager.clearSelectedTask(requireContext());
            Toast.makeText(requireContext(), "Đã bỏ chọn task", Toast.LENGTH_SHORT).show();
        } else {
            SessionManager.setSelectedTaskId(requireContext(), task.getId());
            Toast.makeText(requireContext(), "Đã chọn task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
        }

        loadTasks();
        loadHeaderInfo();
    }

    private void onTaskLongClick(Task task) {
        if (isSessionActive()) {
            Toast.makeText(requireContext(), "Vui lòng ấn Dừng (Stop) phiên hiện tại trước khi hủy chọn!", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer selected = SessionManager.getSelectedTaskId(requireContext());
        if (selected != null && selected.equals(task.getId())) {
            SessionManager.clearSelectedTask(requireContext());
            Toast.makeText(requireContext(), "Đã bỏ chọn task", Toast.LENGTH_SHORT).show();
            loadTasks();
            loadHeaderInfo();
        }
    }

    private boolean isSessionActive() {
        long phaseDuration = isFocus ? focusTimeMs : getCurrentBreakTimeMs();
        boolean hasProgress = timeLeftMs > 0L && timeLeftMs < phaseDuration;
        boolean canResume = !isRunning && sessionInProgress && hasProgress;
        return isRunning || canResume;
    }

    private void showTaskGoalReachedDialog(int taskId) {
        AppExecutors.diskIO().execute(() -> {
            Task task = db.taskDao().getTaskById(taskId);
            if (task == null) return;

            AppExecutors.mainThread(() -> {
                if (!isAdded()) return;

                String message = String.format(Locale.getDefault(),
                        "Bạn đã hoàn thành %d/%d phiên của [%s]. Hoàn thành luôn nhiệm vụ này không?",
                        task.getCompletedSessions(), task.getEstimatedSessions(), task.getTitle());

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Mục tiêu đã đạt!")
                        .setMessage(message)
                        .setPositiveButton("CÓ", (dialog, which) -> markTaskAsCompleted(task))
                        .setNegativeButton("KHÔNG", (dialog, which) -> dialog.dismiss())
                        .setCancelable(false)
                        .show();
            });
        });
    }

    private void markTaskAsCompleted(Task task) {
        AppExecutors.diskIO().execute(() -> {
            long now = System.currentTimeMillis();
            task.setCompleted(true);
            task.setUpdatedAt(now);
            task.setCompletedAt(now);
            db.taskDao().updateTask(task);

            SessionManager.clearIfSelected(requireContext().getApplicationContext(), task.getId());

            AppExecutors.mainThread(() -> {
                loadTasks();
                loadHeaderInfo();
                Toast.makeText(requireContext(), "Chúc mừng! Nhiệm vụ đã hoàn thành.", Toast.LENGTH_SHORT).show();
            });
        });
    }
}