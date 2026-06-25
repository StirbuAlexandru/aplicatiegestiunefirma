package com.example.aplicatiegestiunefirma.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "invoices")
public class Invoice {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String invoiceNumber;
    private int projectId;
    private String projectName;
    private double amount;
    private String date;
    private String dueDate;
    private boolean isPaid;
    private String type; // "INCOME" (Iesire/Vanzare) sau "EXPENSE" (Intrare/Achizitie)
    private String pdfUri; // Path or URI to the imported PDF file
    private double vatPercent = 19.0; // TVA implicit 19%
    private boolean isRecurring = false; // Factura recurenta
    private int recurringDay = 0; // Ziua din luna pentru generare automata (0 = nu recurenta)

    public Invoice(String invoiceNumber, int projectId, String projectName, double amount, String date, String dueDate, boolean isPaid, String type, String pdfUri) {
        this.invoiceNumber = invoiceNumber;
        this.projectId = projectId;
        this.projectName = projectName;
        this.amount = amount;
        this.date = date;
        this.dueDate = dueDate;
        this.isPaid = isPaid;
        this.type = type;
        this.pdfUri = pdfUri;
        this.vatPercent = 19.0;
        this.isRecurring = false;
        this.recurringDay = 0;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { isPaid = paid; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPdfUri() { return pdfUri; }
    public void setPdfUri(String pdfUri) { this.pdfUri = pdfUri; }
    public double getVatPercent() { return vatPercent; }
    public void setVatPercent(double vatPercent) { this.vatPercent = vatPercent; }
    public double getVatAmount() { return amount * vatPercent / 100.0; }
    public double getAmountWithVat() { return amount + getVatAmount(); }
    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }
    public int getRecurringDay() { return recurringDay; }
    public void setRecurringDay(int recurringDay) { this.recurringDay = recurringDay; }
}