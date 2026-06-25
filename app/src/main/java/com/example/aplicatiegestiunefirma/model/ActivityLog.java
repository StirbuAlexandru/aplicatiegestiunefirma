package com.example.aplicatiegestiunefirma.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activity_logs")
public class ActivityLog {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String userEmail;
    private String action;            // "CREATE", "UPDATE", "DELETE"
    private String entityType;        // "Angajat", "Factura", "Proiect", etc.
    private String entityDescription; // ex: "Ion Popescu", "Factura #2024-001"
    private String timestamp;         // "yyyy-MM-dd HH:mm:ss"

    public ActivityLog(String userEmail, String action, String entityType,
                       String entityDescription, String timestamp) {
        this.userEmail = userEmail;
        this.action = action;
        this.entityType = entityType;
        this.entityDescription = entityDescription;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityDescription() { return entityDescription; }
    public void setEntityDescription(String entityDescription) { this.entityDescription = entityDescription; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getActionLabel() {
        switch (action != null ? action : "") {
            case "CREATE": return "Adaugat";
            case "UPDATE": return "Modificat";
            case "DELETE": return "Sters";
            default: return action != null ? action : "";
        }
    }
}
