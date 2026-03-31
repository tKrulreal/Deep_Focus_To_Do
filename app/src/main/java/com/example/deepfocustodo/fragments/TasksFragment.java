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
import com.example.deepfocustodo.models.Task;
import com.example.deepfocustodo.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener, TabRefreshable {

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
    }

    @Override
    public void onTabSelected() {
        if (!isAdded() || getView() == null) {
            return;
        }
        loadTasks();
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

    private void showEditTaskDialog(Task task) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_task, null);
        EditText etTitle = dialogView.findViewById(R.id.etTaskTitle);
        EditText etDesc = dialogView.findViewById(R.id.etTaskDesc);

        etTitle.setText(task.getTitle());
        etDesc.setText(task.getDescription());

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Task")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDesc.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    task.setTitle(title);
                    task.setDescription(desc);
                    db.taskDao().updateTask(task);
                    loadTasks();
                })
                .setNeutralButton("Assign", (dialog, which) -> {
                    SessionManager.setSelectedTaskId(task.getId());
                    Toast.makeText(requireContext(), "Đã chọn task cho phiên tập trung", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onTaskClick(Task task) {
        showEditTaskDialog(task);
    }
}