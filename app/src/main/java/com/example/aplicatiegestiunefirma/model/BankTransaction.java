package com.example.aplicatiegestiunefirma.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bank_transactions")
public class BankTransaction {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String date;          // "yyyy-MM-dd"
    private String description;
    private double amount;
    private String type;          // "CREDIT" (intrare) sau "DEBIT" (iesire)
    private boolean isMatched;    // a fost potrivita cu o factura
    private int matchedInvoiceId; // id-ul facturii potrivite (0 = nepotrivita)

    public BankTransaction(String date, String description, double amount, String type,
                           boolean isMatched, int matchedInvoiceId) {
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.isMatched = isMatched;
        this.matchedInvoiceId = matchedInvoiceId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isMatched() { return isMatched; }
    public void setMatched(boolean matched) { isMatched = matched; }
    public int getMatchedInvoiceId() { return matchedInvoiceId; }
    public void setMatchedInvoiceId(int matchedInvoiceId) { this.matchedInvoiceId = matchedInvoiceId; }
}
