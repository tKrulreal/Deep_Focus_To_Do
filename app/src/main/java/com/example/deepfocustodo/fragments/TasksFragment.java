package com.example.deepfocustodo.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.adapters.TaskAdapter;
import com.example.deepfocustodo.database.AppDatabase;
import com.example.deepfocustodo.models.FocusSession;
import com.example.deepfocustodo.models.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private RecyclerView rvTasks;
    private TaskAdapter adapter;
    private AppDatabase db;
    private FloatingActionButton fabAddTask;

    public TasksFragment() {
        // Required empty public constructor
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
        rvTasks = view.findViewById(R.id.rvTasks);
        fabAddTask = view.findViewById(R.id.fabAddTask);

        setupRecyclerView();
        loadTasks();

        fabAddTask.setOnClickListener(v -> showAddTaskDialog());
        
        // TEST MODE: Nhấn giữ nút FAB để tạo dữ liệu lịch sử giả
        fabAddTask.setOnLongClickListener(v -> {
            createMockData();
            return true;
        });
    }

    private void createMockData() {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            // Tạo 1 phiên hoàn thành 25p
            db.focusSessionDao().insertSession(new FocusSession(
                    null, now - 3600000, now - 2100000, 25, 250, "COMPLETED"));
            // Tạo 1 phiên thất bại 10p
            db.focusSessionDao().insertSession(new FocusSession(
                    null, now - 1800000, now - 1200000, 10, 0, "FAILED"));
            
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Đã tạo dữ liệu giả! Hãy vào History để kiểm tra.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(this);
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(adapter);
    }

    private void loadTasks() {
        List<Task> tasks = db.taskDao().getAllTasks();
        adapter.setTasks(tasks);
    }

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null);
        EditText etTitle = dialogView.findViewById(R.id.etTaskTitle);
        EditText etDesc = dialogView.findViewById(R.id.etTaskDesc);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add New Task")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();

                    if (!title.isEmpty()) {
                        Task newTask = new Task(title, desc, false, System.currentTimeMillis());
                        db.taskDao().insertTask(newTask);
                        loadTasks();
                    } else {
                        Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onTaskCheckChanged(Task task, boolean isChecked) {
        task.setCompleted(isChecked);
        db.taskDao().updateTask(task);
        loadTasks();
    }

    @Override
    public void onDeleteClick(Task task) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.taskDao().deleteTask(task);
                    loadTasks();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}