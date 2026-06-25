package com.example.aplicatiegestiunefirma.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface LeaveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Leave leave);

    @Update
    void update(Leave leave);

    @Delete
    void delete(Leave leave);

    @Query("SELECT * FROM leaves ORDER BY startDate DESC")
    List<Leave> getAllLeaves();

    @Query("SELECT * FROM leaves WHERE employeeId = :employeeId ORDER BY startDate DESC")
    List<Leave> getLeavesByEmployee(int employeeId);

    @Query("SELECT * FROM leaves WHERE status = 'PENDING' ORDER BY startDate DESC")
    List<Leave> getPendingLeaves();

    @Query("SELECT * FROM leaves WHERE employeeId = :employeeId AND status = 'APPROVED' AND startDate >= :startDate AND endDate <= :endDate")
    List<Leave> getApprovedLeavesInRange(int employeeId, String startDate, String endDate);

    @Query("DELETE FROM leaves WHERE id = :id")
    void deleteById(int id);
}
