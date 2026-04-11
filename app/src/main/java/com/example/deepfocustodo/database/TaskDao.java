package com.example.deepfocustodo.database;

import androidx.lifecycle.LiveData;
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
    LiveData<List<Task>> getTasksByStatusLiveData(boolean isCompleted);

    @Query("SELECT * FROM tasks WHERE completed = :isCompleted ORDER BY createdAt DESC")
    List<Task> getTasksByStatus(boolean isCompleted);

    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 1")
    int getCompletedTaskCount();

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    Task getTaskById(int id);

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    LiveData<Task> getTaskByIdLiveData(int id);

    @Query("UPDATE tasks SET completedSessions = completedSessions + 1 WHERE id = :taskId")
    void incrementCompletedSessions(int taskId);

    @Query("UPDATE tasks SET completed = :isCompleted, completedAt = :completedAt WHERE id = :taskId")
    void updateTaskStatus(int taskId, boolean isCompleted, long completedAt);
}