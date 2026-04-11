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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.models.Task;
import com.example.deepfocustodo.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        ProgressBar pbTaskProgress;
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
            pbTaskProgress = itemView.findViewById(R.id.pbTaskProgress);
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
                case 3:
                    tvPriority.setText("Ưu tiên cao");
                    tvPriority.setTextColor(Color.parseColor("#FF5252"));
                    break;
                case 2:
                    tvPriority.setText("Ưu tiên trung bình");
                    tvPriority.setTextColor(Color.parseColor("#FFC107"));
                    break;
                default:
                    tvPriority.setText("Ưu tiên thấp");
                    tvPriority.setTextColor(Color.parseColor("#4CAF50"));
                    break;
            }

            tvSessions.setText(String.format(Locale.getDefault(), "Tiến độ: %d/%d phiên",
                    task.getCompletedSessions(), task.getEstimatedSessions()));

            if (task.getEstimatedSessions() > 0) {
                int progress = (task.getCompletedSessions() * 100) / task.getEstimatedSessions();
                pbTaskProgress.setVisibility(View.VISIBLE);
                pbTaskProgress.setProgress(Math.min(progress, 100));
            } else {
                pbTaskProgress.setVisibility(View.GONE);
            }

            if (task.isCompleted() && task.getCompletedAt() > 0L) {
                tvCompletedTime.setVisibility(View.VISIBLE);
                tvCompletedTime.setText("Xong lúc: " + completionFormat.format(new Date(task.getCompletedAt())));
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

            // Sử dụng MaterialColors.getColor với fallback an toàn
            Context context = itemView.getContext();
            
            // Lấy màu từ theme với fallback nếu attr không tồn tại
            int colorPrimary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.BLUE);
            int colorPrimaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, Color.LTGRAY);
            int colorSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, Color.WHITE);
            int colorOutline = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline, Color.GRAY);
            if (isSelected) {
                cardView.setStrokeWidth(6);
                cardView.setStrokeColor(colorPrimary);
                cardView.setCardBackgroundColor(colorPrimaryContainer);
            } else {
                cardView.setStrokeWidth(2);
                cardView.setStrokeColor(colorOutline);
                cardView.setCardBackgroundColor(colorSurface);
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
                tvTitle.setAlpha(0.6f);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setAlpha(1.0f);
            }
        }
    }
}
