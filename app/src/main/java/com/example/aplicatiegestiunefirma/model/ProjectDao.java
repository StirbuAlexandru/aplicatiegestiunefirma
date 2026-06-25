package com.example.aplicatiegestiunefirma.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Project project);

    @Update
    void update(Project project);

    @Delete
    void delete(Project project);

    @Query("SELECT * FROM projects")
    LiveData<List<Project>> getAllProjects();

    @Query("SELECT * FROM projects")
    List<Project> getAllProjectsDirect();

    @Query("SELECT * FROM projects ORDER BY id DESC LIMIT 1")
    LiveData<Project> getLatestProject();

    @Query("SELECT * FROM projects WHERE id = :projectId")
    Project getProjectById(int projectId);
}