package com.example.deepfocustodo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.models.FocusSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<FocusSession> sessions;
    private Map<Integer, String> taskNameMap;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public HistoryAdapter(List<FocusSession> sessions, Map<Integer, String> taskNameMap) {
        this.sessions = sessions;
        this.taskNameMap = taskNameMap;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        FocusSession session = sessions.get(position);

        String taskName = "Tập trung tự do";
        if (session.getTaskId() != null && taskNameMap.containsKey(session.getTaskId())) {
            taskName = taskNameMap.get(session.getTaskId());
        } else if (session.getTaskId() != null) {
            taskName = "Task đã bị xóa";
        }

        holder.tvTitle.setText(taskName);
        holder.tvDateTime.setText(dateFormat.format(new Date(session.getStartTime())));
        holder.tvDuration.setText("Thời lượng: " + session.getDurationMinutes() + " phút");
        holder.tvPoints.setText("+" + session.getPointsEarned() + " pts");
        holder.tvStatus.setText(session.getStatus());

        if ("COMPLETED".equals(session.getStatus())) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
        } else {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_failed);
        }
    }

    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDateTime, tvDuration, tvPoints, tvStatus;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHistoryTaskTitle);
            tvDateTime = itemView.findViewById(R.id.tvHistoryDateTime);
            tvDuration = itemView.findViewById(R.id.tvHistoryDuration);
            tvPoints = itemView.findViewById(R.id.tvHistoryPoints);
            tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
        }
    }
}