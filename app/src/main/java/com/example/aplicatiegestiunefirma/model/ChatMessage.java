package com.example.aplicatiegestiunefirma.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    private int id;
    @SerializedName("employeeId")
    private int employeeId;
    @SerializedName("employeeName")
    private String employeeName;
    @SerializedName("message")
    private String message;
    @SerializedName("timestamp")
    private String timestamp;
    @SerializedName("isFromAdmin")
    private boolean isFromAdmin;
    @SerializedName("isRead")
    private boolean isRead;

    public ChatMessage(int employeeId, String employeeName, String message,
                       String timestamp, boolean isFromAdmin, boolean isRead) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.message = message;
        this.timestamp = timestamp;
        this.isFromAdmin = isFromAdmin;
        this.isRead = isRead;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public boolean isFromAdmin() { return isFromAdmin; }
    public void setFromAdmin(boolean fromAdmin) { isFromAdmin = fromAdmin; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
