package com.example.aplicatiegestiunefirma.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_reports")
public class DailyReport {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int employeeId;
    private String employeeName;
    private int projectId;
    private String projectName;
    private String date;
    private String reportText;
    private double hoursWorked;
    private String photoUri;   // URI fotografie atașată (poate fi null)
    private double latitude;   // Coordonata GPS latitudine (0 = negeolocalizat)
    private double longitude;  // Coordonata GPS longitudine

    public DailyReport(int employeeId, String employeeName, int projectId, String projectName, String date, String reportText, double hoursWorked) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.projectId = projectId;
        this.projectName = projectName;
        this.date = date;
        this.reportText = reportText;
        this.hoursWorked = hoursWorked;
        this.photoUri = null;
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getReportText() { return reportText; }
    public void setReportText(String reportText) { this.reportText = reportText; }
    public double getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(double hoursWorked) { this.hoursWorked = hoursWorked; }
    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public boolean hasPhoto() { return photoUri != null && !photoUri.isEmpty(); }
    public boolean hasLocation() { return latitude != 0.0 || longitude != 0.0; }
}