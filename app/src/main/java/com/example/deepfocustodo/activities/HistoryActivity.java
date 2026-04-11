package com.example.deepfocustodo.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.adapters.HistoryAdapter;
import com.example.deepfocustodo.database.AppDatabase;
import com.example.deepfocustodo.models.FocusSession;
import com.example.deepfocustodo.models.Task;
import com.example.deepfocustodo.utils.AppExecutors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryItemClickListener {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private AppDatabase db;
    private View layoutEmpty;
    private ImageButton btnClearAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = AppDatabase.getInstance(this);
        rvHistory = findViewById(R.id.rvHistory);
        layoutEmpty = findViewById(R.id.layoutEmptyHistory);
        btnClearAll = findViewById(R.id.btnClearAllHistory);

        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());

        btnClearAll.setOnClickListener(v -> confirmClearAllHistory());

        setupRecyclerView();
        loadHistory();
    }

    private void setupRecyclerView() {
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadHistory() {
        AppExecutors.diskIO().execute(() -> {
            try {
                List<FocusSession> sessions = db.focusSessionDao().getAllHistory();
                List<Task> allTasks = db.taskDao().getAllTasks();

                Map<Integer, String> taskNameMap = new HashMap<>();
                for (Task task : allTasks) {
                    taskNameMap.put(task.getId(), task.getTitle());
                }

                AppExecutors.mainThread(() -> {
                    if (sessions == null || sessions.isEmpty()) {
                        rvHistory.setVisibility(View.GONE);
                        if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                        btnClearAll.setVisibility(View.GONE);
                    } else {
                        rvHistory.setVisibility(View.VISIBLE);
                        if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
                        btnClearAll.setVisibility(View.VISIBLE);
                        if (adapter == null) {
                            adapter = new HistoryAdapter(sessions, taskNameMap, this);
                            rvHistory.setAdapter(adapter);
                        } else {
                            adapter.setSessions(sessions, taskNameMap);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void confirmClearAllHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa toàn bộ lịch sử")
                .setMessage("Bạn có chắc chắn muốn xóa tất cả lịch sử tập trung không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa tất cả", (dialog, which) -> {
                    AppExecutors.diskIO().execute(() -> {
                        db.focusSessionDao().clearAllHistory();
                        AppExecutors.mainThread(() -> {
                            loadHistory();
                            Toast.makeText(this, "Đã xóa toàn bộ lịch sử", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onDeleteItem(FocusSession session) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa lịch sử")
                .setMessage("Bạn có muốn xóa phiên tập trung này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    AppExecutors.diskIO().execute(() -> {
                        db.focusSessionDao().deleteSession(session);
                        AppExecutors.mainThread(() -> {
                            loadHistory();
                            Toast.makeText(this, "Đã xóa phiên tập trung", Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}