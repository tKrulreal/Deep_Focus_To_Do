package com.example.deepfocustodo.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.activities.HistoryActivity;

public class HomeFragment extends Fragment {

    private Button btnStart;
    private Button btnPause;
    private Button btnReset;
    private Button btnViewHistory;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnStart = view.findViewById(R.id.btnStart);
        btnPause = view.findViewById(R.id.btnPause);
        btnReset = view.findViewById(R.id.btnReset);
        btnViewHistory = view.findViewById(R.id.btnViewHistory);

        btnStart.setOnClickListener(v -> {
            // Placeholder cho nhóm viên 2 phát triển timer
        });

        btnPause.setOnClickListener(v -> {
            // Placeholder
        });

        btnReset.setOnClickListener(v -> {
            // Placeholder
        });

        btnViewHistory.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), HistoryActivity.class);
            startActivity(intent);
        });
    }
}