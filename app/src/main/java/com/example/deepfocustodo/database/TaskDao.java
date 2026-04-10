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

    @Query("SELECT * FROM tasks WHERE id = :id")
    Task getTaskById(int id);

    @Query("SELECT * FROM tasks ORDER BY priority DESC, createdAt DESC")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE completed = :isCompleted ORDER BY priority DESC, createdAt DESC")
    List<Task> getTasksByStatus(boolean isCompleted);

    @Query("UPDATE tasks SET completedSessions = completedSessions + 1, updatedAt = :timestamp WHERE id = :taskId")
    void incrementCompletedSessions(int taskId, long timestamp);

    @Query("UPDATE tasks SET completed = :completed, updatedAt = :timestamp WHERE id = :taskId")
    void updateTaskStatus(int taskId, boolean completed, long timestamp);
}