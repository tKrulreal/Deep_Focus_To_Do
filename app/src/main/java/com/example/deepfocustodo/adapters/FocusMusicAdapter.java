package com.example.deepfocustodo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.models.FocusPlaylist;

import java.util.List;

public class FocusMusicAdapter extends RecyclerView.Adapter<FocusMusicAdapter.PlaylistViewHolder> {

    public interface OnPlaylistClickListener {
        void onPlaylistClick(FocusPlaylist playlist);
    }

    private final List<FocusPlaylist> playlists;
    private final OnPlaylistClickListener listener;
    private String selectedPlaylistId;

    public FocusMusicAdapter(List<FocusPlaylist> playlists, String selectedPlaylistId, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.selectedPlaylistId = selectedPlaylistId;
        this.listener = listener;
    }

    public void setSelectedPlaylistId(String selectedPlaylistId) {
        int oldIndex = findIndexById(this.selectedPlaylistId);
        this.selectedPlaylistId = selectedPlaylistId;
        int newIndex = findIndexById(selectedPlaylistId);
        if (oldIndex >= 0) {
            notifyItemChanged(oldIndex);
        }
        if (newIndex >= 0 && newIndex != oldIndex) {
            notifyItemChanged(newIndex);
        }
    }

    private int findIndexById(String playlistId) {
        if (playlistId == null) {
            return -1;
        }
        for (int i = 0; i < playlists.size(); i++) {
            if (playlistId.equals(playlists.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_focus_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        FocusPlaylist playlist = playlists.get(position);
        boolean isSelected = playlist.getId().equals(selectedPlaylistId);

        holder.txtTitle.setText(playlist.getTitle());
        holder.txtSubtitle.setText(playlist.getSubtitle());
        holder.iconBackground.setBackgroundResource(playlist.getIconBackgroundRes());
        holder.btnAction.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        holder.btnAction.setImageResource(android.R.drawable.ic_media_play);
        holder.btnAction.setContentDescription(holder.itemView.getContext().getString(R.string.focus_music_play));

        View.OnClickListener clickListener = v -> listener.onPlaylistClick(playlist);
        holder.itemView.setOnClickListener(clickListener);
        holder.btnAction.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final View iconBackground;
        private final TextView txtTitle;
        private final TextView txtSubtitle;
        private final ImageButton btnAction;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBackground = itemView.findViewById(R.id.viewPlaylistIconBg);
            txtTitle = itemView.findViewById(R.id.txtPlaylistTitle);
            txtSubtitle = itemView.findViewById(R.id.txtPlaylistSubtitle);
            btnAction = itemView.findViewById(R.id.btnPlaylistAction);
        }
    }
}

