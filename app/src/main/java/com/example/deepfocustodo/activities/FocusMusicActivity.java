package com.example.deepfocustodo.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.adapters.FocusMusicAdapter;
import com.example.deepfocustodo.models.FocusPlaylist;
import com.example.deepfocustodo.services.PomodoroService;
import com.example.deepfocustodo.utils.PreferenceHelper;

import java.util.ArrayList;
import java.util.List;

public class FocusMusicActivity extends AppCompatActivity implements FocusMusicAdapter.OnPlaylistClickListener {

    private PreferenceHelper preferenceHelper;
    private FocusMusicAdapter adapter;
    private List<FocusPlaylist> playlists;
    private TextView txtNowPlaying;
    private ImageButton btnSelectNowPlaying;
    private MediaPlayer previewPlayer;
    private String selectedPlaylistId;
    private View viewNowPlayingIconBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_music);

        preferenceHelper = new PreferenceHelper(this);

        playlists = createPlaylists();
        txtNowPlaying = findViewById(R.id.txtNowPlayingTitle);
        viewNowPlayingIconBg = findViewById(R.id.viewNowPlayingIconBg);

        RecyclerView rvPlaylists = findViewById(R.id.rvFocusPlaylists);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));

        selectedPlaylistId = preferenceHelper.getSelectedPlaylistId();
        adapter = new FocusMusicAdapter(playlists, selectedPlaylistId, this);
        rvPlaylists.setAdapter(adapter);

        ImageButton btnBack = findViewById(R.id.btnBackFocusMusic);
        btnBack.setOnClickListener(v -> finish());

        btnSelectNowPlaying = findViewById(R.id.btnSelectNowPlaying);
        btnSelectNowPlaying.setOnClickListener(v -> togglePreviewForSelected());

        updateNowPlayingTitle(selectedPlaylistId);
        updateNowPlayingButton(false);
    }

    @Override
    public void onPlaylistClick(FocusPlaylist playlist) {
        if (!playlist.getId().equals(selectedPlaylistId)) {
            stopPreview();
        }
        selectedPlaylistId = playlist.getId();
        preferenceHelper.setSelectedPlaylistId(selectedPlaylistId);
        adapter.setSelectedPlaylistId(selectedPlaylistId);
        updateNowPlayingTitle(selectedPlaylistId);
        applySelectedTrack();
    }

    private void togglePreviewForSelected() {
        FocusPlaylist selected = findPlaylistById(selectedPlaylistId);
        if (selected == null) {
            return;
        }

        if (previewPlayer != null && previewPlayer.isPlaying()) {
            stopPreview();
            return;
        }

        startPreview(selected.getRawResId());
    }

    private void startPreview(int rawResId) {
        stopPreview();
        previewPlayer = MediaPlayer.create(this, rawResId);
        if (previewPlayer == null) {
            updateNowPlayingButton(false);
            return;
        }

        previewPlayer.setLooping(true);
        previewPlayer.setOnCompletionListener(mp -> updateNowPlayingButton(false));
        previewPlayer.setOnErrorListener((mp, what, extra) -> {
            stopPreview();
            return true;
        });
        previewPlayer.start();
        updateNowPlayingButton(true);
    }

    private void stopPreview() {
        if (previewPlayer != null) {
            previewPlayer.setOnCompletionListener(null);
            previewPlayer.setOnErrorListener(null);
            previewPlayer.release();
            previewPlayer = null;
        }
        updateNowPlayingButton(false);
    }

    private void updateNowPlayingButton(boolean isPlaying) {
        if (btnSelectNowPlaying == null) {
            return;
        }
        btnSelectNowPlaying.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        btnSelectNowPlaying.setContentDescription(getString(isPlaying ? R.string.focus_music_pause_preview : R.string.focus_music_play_preview));
    }

    private FocusPlaylist findPlaylistById(String playlistId) {
        if (playlistId == null) {
            return null;
        }
        for (FocusPlaylist playlist : playlists) {
            if (playlist.getId().equals(playlistId)) {
                return playlist;
            }
        }
        return null;
    }

    private void updateNowPlayingTitle(String selectedPlaylistId) {
        String selectedTitle = getString(R.string.focus_music_default_track);
        int iconRes = R.drawable.bg_music_icon_lofi;
        for (FocusPlaylist playlist : playlists) {
            if (playlist.getId().equals(selectedPlaylistId)) {
                selectedTitle = playlist.getTitle();
                iconRes = playlist.getIconBackgroundRes();
                break;
            }
        }
        txtNowPlaying.setText(selectedTitle);
        if (viewNowPlayingIconBg != null) {
            viewNowPlayingIconBg.setBackgroundResource(iconRes);
        }
    }

    private void applySelectedTrack() {
        Intent intent = new Intent(this, PomodoroService.class);
        intent.setAction(PomodoroService.ACTION_APPLY_SETTINGS);
        startService(intent);
    }

    private List<FocusPlaylist> createPlaylists() {
        List<FocusPlaylist> items = new ArrayList<>();
        items.add(new FocusPlaylist("lofi_beats", "Lo-Fi Beats", "16 tracks - 48m", R.drawable.bg_music_icon_lofi, R.raw.lofi1));
        items.add(new FocusPlaylist("classical_focus", "Classical Focus", "16 tracks - 52m", R.drawable.bg_music_icon_classical, R.raw.lofi2));
        items.add(new FocusPlaylist("nature_sounds", "Nature Sounds", "16 tracks - 51m", R.drawable.bg_music_icon_nature, R.raw.lofi3));
        items.add(new FocusPlaylist("ambient_study", "Ambient Study", "16 tracks - 49m", R.drawable.bg_music_icon_ambient, R.raw.lofi4));
        return items;
    }

    @Override
    protected void onStop() {
        stopPreview();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopPreview();
        super.onDestroy();
    }
}

