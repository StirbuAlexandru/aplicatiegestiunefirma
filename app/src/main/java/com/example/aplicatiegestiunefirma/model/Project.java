package com.example.aplicatiegestiunefirma.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "projects")
public class Project {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String clientName;
    private String description;
    private double budget;
    private String deadline;
    private String status; // e.g., "In Progress", "Completed"
    private String notes;

    public Project(String name, String clientName, String description, double budget, String deadline, String status) {
        this.name = name;
        this.clientName = clientName;
        this.description = description;
        this.budget = budget;
        this.deadline = deadline;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}