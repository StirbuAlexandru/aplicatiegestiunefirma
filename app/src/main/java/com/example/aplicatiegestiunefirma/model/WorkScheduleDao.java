package com.example.aplicatiegestiunefirma.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface WorkScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WorkSchedule schedule);

    @Update
    void update(WorkSchedule schedule);

    @Delete
    void delete(WorkSchedule schedule);

    @Query("SELECT * FROM work_schedules WHERE employeeId = :employeeId ORDER BY dayOfWeek ASC")
    List<WorkSchedule> getScheduleForEmployee(int employeeId);

    @Query("SELECT * FROM work_schedules")
    List<WorkSchedule> getAllSchedulesDirect();

    @Query("DELETE FROM work_schedules WHERE employeeId = :employeeId")
    void deleteAllForEmployee(int employeeId);

    @Query("SELECT * FROM work_schedules WHERE employeeId = :employeeId AND dayOfWeek = :dayOfWeek LIMIT 1")
    WorkSchedule getScheduleForDay(int employeeId, int dayOfWeek);

    @Query("DELETE FROM work_schedules WHERE id = :id")
    void deleteById(int id);
}
