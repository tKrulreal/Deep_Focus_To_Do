package com.example.deepfocustodo.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.deepfocustodo.models.Task;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insertTask(Task task);

    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE completed = :isCompleted ORDER BY createdAt DESC")
    List<Task> getTasksByStatus(boolean isCompleted);
}