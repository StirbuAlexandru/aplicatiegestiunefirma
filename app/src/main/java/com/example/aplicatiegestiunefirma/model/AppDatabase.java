package com.example.aplicatiegestiunefirma.model;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
    entities = {
        Employee.class, Project.class, Company.class, Invoice.class, DailyReport.class,
        Leave.class, ChatMessage.class, WorkSchedule.class, ActivityLog.class, BankTransaction.class
    },
    version = 20,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    public static void resetInstance() {
        instance = null;
    }

    public abstract EmployeeDao employeeDao();
    public abstract ProjectDao projectDao();
    public abstract CompanyDao companyDao();
    public abstract InvoiceDao invoiceDao();
    public abstract DailyReportDao dailyReportDao();
    public abstract LeaveDao leaveDao();
    public abstract ChatMessageDao chatMessageDao();
    public abstract WorkScheduleDao workScheduleDao();
    public abstract ActivityLogDao activityLogDao();
    public abstract BankTransactionDao bankTransactionDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "company_database")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}