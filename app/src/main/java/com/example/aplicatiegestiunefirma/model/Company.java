package com.example.aplicatiegestiunefirma.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "companies")
public class Company {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String address;
    private String taxId; // CUI
    private String regCom; // Nr. Reg. Com.
    private String iban;
    private String bank;
    private String phone;
    private String email;

    public Company(String name, String address, String taxId, String regCom, String iban, String bank, String phone, String email) {
        this.name = name;
        this.address = address;
        this.taxId = taxId;
        this.regCom = regCom;
        this.iban = iban;
        this.bank = bank;
        this.phone = phone;
        this.email = email;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }
    public String getRegCom() { return regCom; }
    public void setRegCom(String regCom) { this.regCom = regCom; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public String getBank() { return bank; }
    public void setBank(String bank) { this.bank = bank; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}