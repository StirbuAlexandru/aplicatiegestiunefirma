package com.example.aplicatiegestiunefirma.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CompanyDao {
    @Insert
    void insert(Company company);

    @Update
    void update(Company company);

    @Delete
    void delete(Company company);

    @Query("SELECT * FROM companies")
    LiveData<List<Company>> getAllCompanies();

    @Query("SELECT * FROM companies")
    List<Company> getAllCompaniesDirect();
}