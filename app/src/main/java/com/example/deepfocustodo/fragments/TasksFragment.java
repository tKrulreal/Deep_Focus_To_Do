package com.example.deepfocustodo.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.adapters.TaskAdapter;
import com.example.deepfocustodo.database.AppDatabase;
import com.example.deepfocustodo.database.StatsRepository;
import com.example.deepfocustodo.models.Task;
import com.example.deepfocustodo.utils.SessionManager;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener, TabRefreshable {

    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private AppDatabase db;
    private StatsRepository statsRepository;
    private FloatingActionButton fabAddTask;
    private TextView tvTotalPointsHeader, tvEmptyTasks;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TasksFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getInstance(requireContext());
        statsRepository = new StatsRepository(requireContext());

        rvTasks = view.findViewById(R.id.rvTasks);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        tvTotalPointsHeader = view.findViewById(R.id.tvTotalPointsHeader);
        tvEmptyTasks = view.findViewById(R.id.tvEmptyTasks);

        setupRecyclerView();
        loadData();

        fabAddTask.setOnClickListener(v -> showTaskDialog(null));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onTabSelected() {
        if (!isAdded() || getView() == null) return;
        loadData();
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(this);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(adapter);
    }

    private void loadData() {
        if (!isAdded()) return;
        executor.execute(() -> {
            try {
                List<Task> tasks = db.taskDao().getAllTasks();
                int totalPoints = statsRepository.getTotalPoints();

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (adapter != null) adapter.setTasks(tasks, requireContext());
                        if (tvEmptyTasks != null) tvEmptyTasks.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
                        if (tvTotalPointsHeader != null) tvTotalPointsHeader.setText(String.format(java.util.Locale.getDefault(), "%d pts", totalPoints));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void showTaskDialog(@Nullable Task taskToEdit) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null);
        EditText etTitle = dialogView.findViewById(R.id.etTaskTitle);
        EditText etDesc = dialogView.findViewById(R.id.etTaskDesc);
        MaterialButtonToggleGroup togglePriority = dialogView.findViewById(R.id.togglePriority);
        NumberPicker npSessions = dialogView.findViewById(R.id.npSessions);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);

        npSessions.setMinValue(1);
        npSessions.setMaxValue(10);

        if (taskToEdit != null) {
            tvTitle.setText("Edit Task");
            etTitle.setText(taskToEdit.getTitle());
            etDesc.setText(taskToEdit.getDescription());
            npSessions.setValue(taskToEdit.getEstimatedSessions());
            int priority = taskToEdit.getPriority();
            if (priority == 3) togglePriority.check(R.id.btnHigh);
            else if (priority == 2) togglePriority.check(R.id.btnMedium);
            else togglePriority.check(R.id.btnLow);
        }

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();
                    int sessions = npSessions.getValue();
                    int priority = 1;
                    int checkedId = togglePriority.getCheckedButtonId();
                    if (checkedId == R.id.btnHigh) priority = 3;
                    else if (checkedId == R.id.btnMedium) priority = 2;

                    if (!title.isEmpty()) {
                        int finalPriority = priority;
                        executor.execute(() -> {
                            if (taskToEdit == null) {
                                db.taskDao().insertTask(new Task(title, desc, false, System.currentTimeMillis(), finalPriority, sessions));
                            } else {
                                taskToEdit.setTitle(title);
                                taskToEdit.setDescription(desc);
                                taskToEdit.setPriority(finalPriority);
                                taskToEdit.setEstimatedSessions(sessions);
                                taskToEdit.setUpdatedAt(System.currentTimeMillis());
                                db.taskDao().updateTask(taskToEdit);
                            }
                            if (getActivity() != null) getActivity().runOnUiThread(this::loadData);
                        });
                    } else {
                        Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onTaskCheckChanged(Task task, boolean isChecked) {
        executor.execute(() -> {
            task.setCompleted(isChecked);
            task.setUpdatedAt(System.currentTimeMillis());
            db.taskDao().updateTask(task);
            if (getActivity() != null) getActivity().runOnUiThread(this::loadData);
        });
    }

    @Override
    public void onDeleteClick(Task task) {
        final int taskIdToDelete = task.getId();
        executor.execute(() -> {
            db.taskDao().deleteTask(task);
            SessionManager.clearIfSelected(requireContext(), taskIdToDelete);

            if (isAdded() && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    loadData();
                    View view = getView();
                    if (view != null) {
                        Snackbar.make(view, "Task deleted", Snackbar.LENGTH_LONG)
                                .setAction("UNDO", v -> {
                                    executor.execute(() -> {
                                        db.taskDao().insertTask(task);
                                        if (isAdded() && getActivity() != null) {
                                            getActivity().runOnUiThread(this::loadData);
                                        }
                                    });
                                }).show();
                    }
                });
            }
        });
    }

    @Override
    public void onTaskClick(Task task) {
        showTaskDialog(task);
    }

    @Override
    public void onTaskLongClick(Task task) {
        if (task.isCompleted()) {
            Toast.makeText(requireContext(), "Cannot select a completed task", Toast.LENGTH_SHORT).show();
            return;
        }
        SessionManager.setSelectedTaskId(requireContext(), task.getId());
        loadData();
        Toast.makeText(requireContext(), "Task selected for focus", Toast.LENGTH_SHORT).show();
    }
}