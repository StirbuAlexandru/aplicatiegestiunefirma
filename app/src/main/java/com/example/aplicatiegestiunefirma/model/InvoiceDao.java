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
public interface InvoiceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Invoice invoice);

    @Update
    void update(Invoice invoice);

    @Delete
    void delete(Invoice invoice);

    @Query("SELECT * FROM invoices ORDER BY id DESC")
    LiveData<List<Invoice>> getAllInvoices();

    @Query("SELECT * FROM invoices WHERE isPaid = 0")
    List<Invoice> getUnpaidInvoicesDirect();

    @Query("SELECT * FROM invoices WHERE projectId = :projectId")
    List<Invoice> getInvoicesByProjectDirect(int projectId);

    @Query("SELECT * FROM invoices ORDER BY id DESC LIMIT 1")
    LiveData<Invoice> getLatestInvoice();

    @Query("SELECT * FROM invoices")
    List<Invoice> getAllInvoicesDirect();
}