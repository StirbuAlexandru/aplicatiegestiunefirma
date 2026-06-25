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
public interface EmployeeDao {
    // OnConflictStrategy.REPLACE repara crash-ul prin suprascrierea datelor vechi
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Employee employee);

    @Update
    void update(Employee employee);

    @Delete
    void delete(Employee employee);

    @Query("SELECT * FROM employees ORDER BY firstName ASC")
    LiveData<List<Employee>> getAllEmployees();

    @Query("SELECT * FROM employees")
    List<Employee> getAllEmployeesDirect();

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    Employee getEmployeeById(int id);
}
