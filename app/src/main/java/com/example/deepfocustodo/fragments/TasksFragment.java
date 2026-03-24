package com.example.deepfocustodo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.deepfocustodo.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TasksFragment extends Fragment {

    private FloatingActionButton fabAddTask;

    public TasksFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fabAddTask = view.findViewById(R.id.fabAddTask);
        fabAddTask.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Màn hình thêm task sẽ được phát triển sau", Toast.LENGTH_SHORT).show()
        );
    }
}