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
public interface DailyReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyReport report);

    @Update
    void update(DailyReport report);

    @Delete
    void delete(DailyReport report);

    @Query("SELECT * FROM daily_reports ORDER BY id DESC")
    LiveData<List<DailyReport>> getAllReports();

    @Query("SELECT * FROM daily_reports WHERE date = :date ORDER BY id DESC")
    LiveData<List<DailyReport>> getReportsByDate(String date);

    @Query("SELECT * FROM daily_reports WHERE date = :date ORDER BY id DESC")
    List<DailyReport> getReportsByDateDirect(String date);

    @Query("SELECT * FROM daily_reports WHERE date LIKE :month || '-%' ORDER BY date ASC")
    List<DailyReport> getReportsByMonthDirect(String month);

    @Query("SELECT * FROM daily_reports WHERE date LIKE :year || '-%' ORDER BY date ASC")
    List<DailyReport> getReportsByYearDirect(String year);

    @Query("SELECT * FROM daily_reports WHERE projectId = :projectId")
    List<DailyReport> getReportsByProjectDirect(int projectId);

    @Query("SELECT * FROM daily_reports WHERE employeeId = :employeeId")
    LiveData<List<DailyReport>> getReportsByEmployee(int employeeId);

    @Query("SELECT * FROM daily_reports WHERE employeeId = :employeeId AND date LIKE :month || '-%' ORDER BY date ASC")
    List<DailyReport> getReportsByEmployeeAndMonthDirect(int employeeId, String month);

    @Query("SELECT * FROM daily_reports WHERE employeeId = :employeeId ORDER BY date DESC")
    List<DailyReport> getReportsByEmployeeDirect(int employeeId);
}