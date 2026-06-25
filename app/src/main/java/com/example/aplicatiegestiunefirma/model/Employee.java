package com.example.aplicatiegestiunefirma.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "employees")
public class Employee {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String firstName;
    private String lastName;
    private String cnp;
    private String idSeries;
    private String idNumber;
    private String phone;
    private String email;
    private String position;
    private String paymentType; // "HOURLY", "DAILY", "FIXED"
    private double paymentRate; // Rata per ora, per zi sau salariul fix lunar
    private int paymentDay;     // Ziua din luna pentru plata salariului fix (1-31)
    private double salaryPerHour; // Pastram pentru compatibilitate momentan
    private double netSalary;     
    private double grossSalary;   
    private String contractPdfUri;

    public Employee(String firstName, String lastName, String cnp, String idSeries, String idNumber, String phone, String email, String position, String paymentType, double paymentRate, int paymentDay, String contractPdfUri) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.cnp = cnp;
        this.idSeries = idSeries;
        this.idNumber = idNumber;
        this.phone = phone;
        this.email = email;
        this.position = position;
        this.paymentType = paymentType;
        this.paymentRate = paymentRate;
        this.paymentDay = paymentDay;
        this.contractPdfUri = contractPdfUri;
        // Mapam si vechiul camp pentru compatibilitate daca e pe ora
        if ("HOURLY".equals(paymentType)) {
            this.salaryPerHour = paymentRate;
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getCnp() { return cnp; }
    public void setCnp(String cnp) { this.cnp = cnp; }
    public String getIdSeries() { return idSeries; }
    public void setIdSeries(String idSeries) { this.idSeries = idSeries; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public double getPaymentRate() { return paymentRate; }
    public void setPaymentRate(double paymentRate) { this.paymentRate = paymentRate; }
    public int getPaymentDay() { return paymentDay; }
    public void setPaymentDay(int paymentDay) { this.paymentDay = paymentDay; }
    public double getSalaryPerHour() { return salaryPerHour; }
    public void setSalaryPerHour(double salaryPerHour) { this.salaryPerHour = salaryPerHour; }
    public double getNetSalary() { return netSalary; }
    public void setNetSalary(double netSalary) { this.netSalary = netSalary; }
    public double getGrossSalary() { return grossSalary; }
    public void setGrossSalary(double grossSalary) { this.grossSalary = grossSalary; }
    public String getContractPdfUri() { return contractPdfUri; }
    public void setContractPdfUri(String contractPdfUri) { this.contractPdfUri = contractPdfUri; }
    public String getFullName() { return firstName + " " + lastName; }
}