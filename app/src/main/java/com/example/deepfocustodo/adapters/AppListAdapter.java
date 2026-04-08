package com.example.deepfocustodo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.models.AppItem;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    private List<AppItem> appList = new ArrayList<>();
    private final OnAppToggleListener listener;

    public interface OnAppToggleListener {
        void onToggle(String packageName, boolean isBlocked);
    }

    public AppListAdapter(OnAppToggleListener listener) {
        this.listener = listener;
    }

    public void setAppList(List<AppItem> appList) {
        this.appList = appList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppItem app = appList.get(position);

        holder.tvAppName.setText(app.getAppName());
        holder.ivAppIcon.setImageDrawable(app.getIcon());

        // Gỡ bỏ listener cũ trước khi set trạng thái để tránh lỗi khi cuộn
        holder.switchBlockApp.setOnCheckedChangeListener(null);
        holder.switchBlockApp.setChecked(app.isBlocked());

        // Lắng nghe sự kiện khi Switch thay đổi
        holder.switchBlockApp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                AppItem currentApp = appList.get(pos);
                currentApp.setBlocked(isChecked);
                listener.onToggle(currentApp.getPackageName(), isChecked);
            }
        });

        // Lắng nghe sự kiện khi click vào toàn bộ dòng (row)
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                // Chỉ cần đảo ngược trạng thái của Switch.
                // Khi Switch đổi trạng thái, nó sẽ tự động kích hoạt OnCheckedChangeListener ở trên.
                boolean isCurrentlyChecked = holder.switchBlockApp.isChecked();
                holder.switchBlockApp.setChecked(!isCurrentlyChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName;
        Switch switchBlockApp;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            switchBlockApp = itemView.findViewById(R.id.switchBlockApp);
        }
    }
}
