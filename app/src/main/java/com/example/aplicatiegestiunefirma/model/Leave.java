package com.example.aplicatiegestiunefirma.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "leaves")
public class Leave {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int employeeId;
    private String employeeName;
    private String startDate;   // format "yyyy-MM-dd"
    private String endDate;     // format "yyyy-MM-dd"
    private String type;        // "CO" (Odihna), "CM" (Medical), "CFP" (Fara Plata), "EVENT"
    private String reason;
    private String status;      // "PENDING", "APPROVED", "REJECTED"
    private int workingDays;    // numar zile lucratoare

    public Leave(int employeeId, String employeeName, String startDate, String endDate,
                 String type, String reason, String status, int workingDays) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.reason = reason;
        this.status = status;
        this.workingDays = workingDays;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getWorkingDays() { return workingDays; }
    public void setWorkingDays(int workingDays) { this.workingDays = workingDays; }

    public String getTypeLabel() {
        switch (type != null ? type : "") {
            case "CO": return "Concediu Odihna";
            case "CM": return "Concediu Medical";
            case "CFP": return "Fara Plata";
            case "EVENT": return "Eveniment";
            default: return type != null ? type : "";
        }
    }

    public String getStatusLabel() {
        switch (status != null ? status : "") {
            case "APPROVED": return "Aprobat";
            case "REJECTED": return "Respins";
            default: return "In asteptare";
        }
    }
}
