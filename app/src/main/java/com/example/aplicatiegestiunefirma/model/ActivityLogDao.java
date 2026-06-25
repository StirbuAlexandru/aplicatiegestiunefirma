package com.example.aplicatiegestiunefirma.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ActivityLogDao {
    @Insert
    void insert(ActivityLog log);

    @Delete
    void delete(ActivityLog log);

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    List<ActivityLog> getAllLogs();

    @Query("SELECT * FROM activity_logs WHERE entityType = :entityType ORDER BY timestamp DESC")
    List<ActivityLog> getLogsByType(String entityType);

    @Query("SELECT * FROM activity_logs WHERE userEmail = :email ORDER BY timestamp DESC")
    List<ActivityLog> getLogsByUser(String email);

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    List<ActivityLog> getRecentLogs(int limit);

    @Query("DELETE FROM activity_logs WHERE timestamp < :cutoffDate")
    void deleteOldLogs(String cutoffDate);
}
