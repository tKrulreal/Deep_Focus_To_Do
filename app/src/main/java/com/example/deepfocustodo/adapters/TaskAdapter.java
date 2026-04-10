package com.example.deepfocustodo.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.models.Task;
import com.example.deepfocustodo.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList = new ArrayList<>();
    private final OnTaskClickListener listener;
    private Integer selectedTaskId = null;

    public interface OnTaskClickListener {
        void onTaskCheckChanged(Task task, boolean isChecked);
        void onDeleteClick(Task task);
        void onTaskClick(Task task);
        void onTaskLongClick(Task task);
    }

    public TaskAdapter(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks, Context context) {
        this.taskList = tasks != null ? tasks : new ArrayList<>();
        this.selectedTaskId = SessionManager.getSelectedTaskId(context);
        notifyDataSetChanged();
    }
    
    public List<Task> getTasks() {
        return taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        boolean isSelected = selectedTaskId != null && selectedTaskId.equals(task.getId());
        holder.bind(task, isSelected, listener);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        CheckBox cbCompleted;
        TextView tvTitle, tvDesc, tvPriority, tvSessions, tvCompletedTime;
        ImageButton btnDelete;
        LinearLayout layoutInfo;
        private final SimpleDateFormat completionFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDesc = itemView.findViewById(R.id.tvTaskDesc);
            tvPriority = itemView.findViewById(R.id.tvPriority);
            tvSessions = itemView.findViewById(R.id.tvSessions);
            tvCompletedTime = itemView.findViewById(R.id.tvCompletedTime);
            btnDelete = itemView.findViewById(R.id.btnDeleteTask);
            layoutInfo = itemView.findViewById(R.id.layoutTaskInfo);
        }

        public void bind(Task task, boolean isSelected, OnTaskClickListener listener) {
            tvTitle.setText(task.getTitle());
            tvDesc.setText(task.getDescription());

            if (task.getDescription() == null || task.getDescription().isEmpty()) {
                tvDesc.setVisibility(View.GONE);
            } else {
                tvDesc.setVisibility(View.VISIBLE);
            }

            switch (task.getPriority()) {
                case 3: // High
                    tvPriority.setText("High Priority");
                    tvPriority.setTextColor(Color.parseColor("#FF5252"));
                    break;
                case 2: // Medium
                    tvPriority.setText("Medium Priority");
                    tvPriority.setTextColor(Color.parseColor("#FFC107"));
                    break;
                default: // Low
                    tvPriority.setText("Low Priority");
                    tvPriority.setTextColor(Color.parseColor("#4CAF50"));
                    break;
            }

            tvSessions.setText(String.format(Locale.getDefault(), "Can: %d | Thuc te: %d phien",
                    task.getEstimatedSessions(), task.getCompletedSessions()));

            if (task.isCompleted() && task.getCompletedAt() > 0L) {
                tvCompletedTime.setVisibility(View.VISIBLE);
                tvCompletedTime.setText("Hoan thanh luc: " + completionFormat.format(new Date(task.getCompletedAt())));
            } else {
                tvCompletedTime.setVisibility(View.GONE);
            }

            cbCompleted.setOnCheckedChangeListener(null);
            cbCompleted.setChecked(task.isCompleted());
            updateStrikethrough(task.isCompleted());

            cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateStrikethrough(isChecked);
                if (listener != null) listener.onTaskCheckChanged(task, isChecked);
            });

            if (isSelected) {
                cardView.setStrokeWidth(4);
                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.purple_500));
                cardView.setCardBackgroundColor(Color.parseColor("#F3E5F5"));
            } else {
                cardView.setStrokeWidth(0);
                cardView.setCardBackgroundColor(Color.WHITE);
            }

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(task);
            });

            layoutInfo.setOnClickListener(v -> {
                if (listener != null) listener.onTaskClick(task);
            });

            layoutInfo.setOnLongClickListener(v -> {
                if (listener != null) listener.onTaskLongClick(task);
                return true;
            });
        }

        private void updateStrikethrough(boolean isCompleted) {
            if (isCompleted) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setAlpha(0.5f);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setAlpha(1.0f);
            }
        }
    }
}