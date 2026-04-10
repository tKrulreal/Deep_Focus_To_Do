package com.example.deepfocustodo.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
    private OnHistoryItemClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public interface OnHistoryItemClickListener {
        void onDeleteItem(FocusSession session);
    }

    public HistoryAdapter(List<FocusSession> sessions, Map<Integer, String> taskNameMap, OnHistoryItemClickListener listener) {
        this.sessions = sessions;
        this.taskNameMap = taskNameMap;
        this.listener = listener;
    }

    public void setSessions(List<FocusSession> sessions, Map<Integer, String> taskNameMap) {
        this.sessions = sessions;
        this.taskNameMap = taskNameMap;
        notifyDataSetChanged();
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
        if (session.getTaskId() != null) {
            if (taskNameMap != null && taskNameMap.containsKey(session.getTaskId())) {
                taskName = taskNameMap.get(session.getTaskId());
            } else {
                taskName = "Task đã xóa";
            }
        }

        holder.tvTitle.setText(taskName);
        holder.tvDateTime.setText(dateFormat.format(new Date(session.getStartTime())));
        
        // Use actualDuration (it was changed from durationMinutes in model)
        int duration = session.getActualDuration();
        holder.tvDuration.setText(String.format(Locale.getDefault(), "Thời lượng: %d phút", duration));
        
        holder.tvPoints.setText("+" + session.getPointsEarned() + " pts");
        
        String statusText = session.getStatus();
        holder.tvStatus.setText(statusText);

        // Styling based on status
        if ("COMPLETED".equals(statusText)) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
            holder.tvPoints.setVisibility(View.VISIBLE);
        } else {
            holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // Red
            holder.tvPoints.setVisibility(View.GONE);
        }
        
        // Show type (Focus/Break)
        String typeLabel = "FOCUS".equals(session.getType()) ? "Phiên tập trung" : "Giải lao";
        if (holder.tvType != null) {
            holder.tvType.setText(typeLabel);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteItem(session);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDateTime, tvDuration, tvPoints, tvStatus, tvType;
        ImageButton btnDelete;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHistoryTaskTitle);
            tvDateTime = itemView.findViewById(R.id.tvHistoryDateTime);
            tvDuration = itemView.findViewById(R.id.tvHistoryDuration);
            tvPoints = itemView.findViewById(R.id.tvHistoryPoints);
            tvStatus = itemView.findViewById(R.id.tvHistoryStatus);
            tvType = itemView.findViewById(R.id.tvHistoryType);
            btnDelete = itemView.findViewById(R.id.btnDeleteHistoryItem);
        }
    }
}