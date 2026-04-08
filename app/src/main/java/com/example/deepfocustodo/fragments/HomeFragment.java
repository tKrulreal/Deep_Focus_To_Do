package com.example.deepfocustodo.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.example.deepfocustodo.utils.PreferenceHelper;
import com.example.deepfocustodo.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class HomeFragment extends Fragment implements TaskAdapter.OnTaskClickListener, TabRefreshable {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2001;
    private static final int FOCUS_SESSIONS_PER_CYCLE = 4;

    private Button btnStart, btnPause, btnStop, btnResume, btnReset, btnViewHistory;
    private TextView tvTimer, tvMode;
    private RecyclerView recyclerTasks;

    private AppDatabase db;
    private PreferenceHelper preferenceHelper;
    private TaskAdapter taskAdapter;

    private long timeLeftMs;
    private boolean isRunning;
    private boolean isFocus = true;
    private boolean sessionInProgress;
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

            updateUI();
        }
    };

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable android.view.ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnStart = view.findViewById(R.id.btnStart);
        btnPause = view.findViewById(R.id.btnPause);
        btnStop = view.findViewById(R.id.btnStop);
        btnResume = view.findViewById(R.id.btnResume);
//        btnReset = view.findViewById(R.id.btnReset);
        btnViewHistory = view.findViewById(R.id.btnViewHistory);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvMode = view.findViewById(R.id.tvMode);
        recyclerTasks = view.findViewById(R.id.recyclerTasks);

        db = AppDatabase.getInstance(requireContext());
        preferenceHelper = new PreferenceHelper(requireContext());

        setupTaskList();
        loadDurations();
        loadStateFromService();
        updateUI();
        setupButtonLogic();

        btnViewHistory.setOnClickListener(v -> startActivity(new Intent(requireContext(), HistoryActivity.class)));
        requestNotificationPermissionIfNeeded();
    }

    private void setupButtonLogic() {
        btnStart.setOnClickListener(v -> dispatchServiceAction(PomodoroService.ACTION_START));
        btnPause.setOnClickListener(v -> dispatchServiceAction(PomodoroService.ACTION_PAUSE));
        btnResume.setOnClickListener(v -> dispatchServiceAction(PomodoroService.ACTION_RESUME));
        
        // Nút dừng hiện Dialog xác nhận
        btnStop.setOnClickListener(v -> showStopConfirmationDialog());

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> dispatchServiceAction(PomodoroService.ACTION_RESET));
        }
    }

    private void showStopConfirmationDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác nhận dừng")
                .setMessage("Bạn muốn dừng Pomodoro này lại phải không?")
                .setNegativeButton("Hủy", (d, which) -> d.dismiss())
                .setPositiveButton("Dừng lại", (d, which) -> {
                    dispatchServiceAction(PomodoroService.ACTION_STOP);
                })
                .create();

        dialog.show();

        // Đổi màu nút "Dừng lại" sang đỏ
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
        }
    }

    private void updateUI() {
        updateTimerText();
        updateModeText();
        updateButtonStates();
    }

    private void updateButtonStates() {
        btnStart.setVisibility(View.GONE);
        btnPause.setVisibility(View.GONE);
        btnResume.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);

        if (isRunning) {
            btnPause.setVisibility(View.VISIBLE);
        } else if (sessionInProgress) {
            btnResume.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.VISIBLE);
        } else {
            btnStart.setVisibility(View.VISIBLE);
        }
    }

    private void dispatchServiceAction(String action) {
        Intent intent = new Intent(requireContext(), PomodoroService.class);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && 
           (PomodoroService.ACTION_START.equals(action) || PomodoroService.ACTION_RESUME.equals(action))) {
            ContextCompat.startForegroundService(requireContext(), intent);
        } else {
            requireContext().startService(intent);
        }
    }

    private void updateTimerText() {
        int totalSeconds = (int) (timeLeftMs / 1000L);
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60));
    }

    private void updateModeText() {
        tvMode.setText(isFocus ? "TẬP TRUNG" : "NGHỈ NGƠI");
        tvMode.setTextColor(ContextCompat.getColor(requireContext(), 
            isFocus ? android.R.color.holo_red_light : android.R.color.holo_green_light));
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(PomodoroService.ACTION_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(requireContext(), timerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
        receiverRegistered = true;
        loadTasks();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (receiverRegistered) {
            requireContext().unregisterReceiver(timerReceiver);
            receiverRegistered = false;
        }
    }

    private void loadStateFromService() {
        PomodoroService.TimerState state = PomodoroService.readState(requireContext());
        timeLeftMs = state.timeLeftMs;
        isRunning = state.isRunning;
        isFocus = state.isFocus;
        sessionInProgress = state.sessionInProgress;
    }

    private void setupTaskList() {
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        taskAdapter = new TaskAdapter(this);
        recyclerTasks.setAdapter(taskAdapter);
    }

    private void loadTasks() {
        taskAdapter.setTasks(db.taskDao().getTasksByStatus(false));
    }

    private void loadDurations() {}

    @Override
    public void onTaskCheckChanged(Task task, boolean isChecked) {
        task.setCompleted(isChecked);
        db.taskDao().updateTask(task);
        loadTasks();
    }

    @Override
    public void onDeleteClick(Task task) {
        db.taskDao().deleteTask(task);
        loadTasks();
    }

    @Override
    public void onTaskClick(Task task) {
        SessionManager.setSelectedTaskId(task.getId());
        Toast.makeText(requireContext(), "Đã chọn task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTabSelected() { loadTasks(); }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }
}
