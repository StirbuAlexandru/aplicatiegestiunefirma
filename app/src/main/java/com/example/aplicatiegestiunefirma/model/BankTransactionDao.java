package com.example.aplicatiegestiunefirma.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BankTransactionDao {
    @Insert
    void insert(BankTransaction transaction);

    @Insert
    void insertAll(List<BankTransaction> transactions);

    @Update
    void update(BankTransaction transaction);

    @Delete
    void delete(BankTransaction transaction);

    @Query("SELECT * FROM bank_transactions ORDER BY date DESC")
    List<BankTransaction> getAllTransactions();

    @Query("SELECT * FROM bank_transactions WHERE isMatched = 0 ORDER BY date DESC")
    List<BankTransaction> getUnmatchedTransactions();

    @Query("SELECT * FROM bank_transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<BankTransaction> getTransactionsInRange(String startDate, String endDate);

    @Query("DELETE FROM bank_transactions")
    void deleteAll();
}
