package com.example.deepfocustodo.activities;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.adapters.HistoryAdapter;
import com.example.deepfocustodo.database.AppDatabase;
import com.example.deepfocustodo.models.FocusSession;
import com.example.deepfocustodo.models.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private AppDatabase db;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = AppDatabase.getInstance(this);
        rvHistory = findViewById(R.id.rvHistory);
        btnBack = findViewById(R.id.btnBackHistory);

        btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        loadHistory();
    }

    private void setupRecyclerView() {
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadHistory() {
        // Chạy trên thread hiện tại vì đã allowMainThreadQueries trong AppDatabase
        List<FocusSession> sessions = db.focusSessionDao().getAllHistory();
        List<Task> allTasks = db.taskDao().getAllTasks();
        
        // Tạo Map để tra cứu tên task nhanh hơn, tránh query trong Adapter
        Map<Integer, String> taskNameMap = new HashMap<>();
        for (Task task : allTasks) {
            taskNameMap.put(task.getId(), task.getTitle());
        }

        if (sessions != null) {
            adapter = new HistoryAdapter(sessions, taskNameMap);
            rvHistory.setAdapter(adapter);
        }
    }
}