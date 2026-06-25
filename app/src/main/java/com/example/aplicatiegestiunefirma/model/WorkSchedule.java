package com.example.aplicatiegestiunefirma.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "work_schedules")
public class WorkSchedule {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int employeeId;
    private int dayOfWeek;   // 1=Luni, 2=Marti, 3=Miercuri, 4=Joi, 5=Vineri, 6=Sambata, 7=Duminica
    private String startTime; // "08:00"
    private String endTime;   // "17:00"
    private boolean isWorkDay;

    public WorkSchedule(int employeeId, int dayOfWeek, String startTime, String endTime, boolean isWorkDay) {
        this.employeeId = employeeId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isWorkDay = isWorkDay;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public boolean isWorkDay() { return isWorkDay; }
    public void setWorkDay(boolean workDay) { isWorkDay = workDay; }

    public String getDayName() {
        switch (dayOfWeek) {
            case 1: return "Luni";
            case 2: return "Marti";
            case 3: return "Miercuri";
            case 4: return "Joi";
            case 5: return "Vineri";
            case 6: return "Sambata";
            case 7: return "Duminica";
            default: return "Zi " + dayOfWeek;
        }
    }
}
