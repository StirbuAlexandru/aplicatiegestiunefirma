package com.example.aplicatiegestiunefirma;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.model.Invoice;
import com.example.aplicatiegestiunefirma.model.Project;

import java.util.List;

public class ProjectDetailActivity extends AppCompatActivity {

    private TextView tvName, tvClient, tvTotalIncome, tvTotalExpenses, tvTotalLabor, tvNetProfit, tvTotalHours;
    private EditText etProjectNotes;
    private AppDatabase db;
    private int projectId;
    private Project currentProject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_detail);

        db = AppDatabase.getInstance(this);
        projectId = getIntent().getIntExtra("PROJECT_ID", -1);

        tvName = findViewById(R.id.tvDetProjectName);
        tvClient = findViewById(R.id.tvDetClient);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvTotalLabor = findViewById(R.id.tvTotalLabor);
        tvNetProfit = findViewById(R.id.tvNetProfit);
        tvTotalHours = findViewById(R.id.tvTotalHours);
        etProjectNotes = findViewById(R.id.etProjectNotes);

        findViewById(R.id.btnSaveNotes).setOnClickListener(v -> saveNotes());

        loadProjectDetails();
    }

    private void loadProjectDetails() {
        new Thread(() -> {
            currentProject = db.projectDao().getProjectById(projectId);
            if (currentProject != null) {
                runOnUiThread(() -> {
                    tvName.setText(currentProject.getName());
                    tvClient.setText("Client: " + currentProject.getClientName());
                    if (currentProject.getNotes() != null) {
                        etProjectNotes.setText(currentProject.getNotes());
                    }
                });
            }

            // Calcul Venituri si Cheltuieli Materiale
            List<Invoice> invoices = db.invoiceDao().getInvoicesByProjectDirect(projectId);
            double income = 0;
            double expenses = 0;
            for (Invoice i : invoices) {
                if ("INCOME".equals(i.getType())) income += i.getAmount();
                else expenses += i.getAmount();
            }

            // Calcul Manopera (Salarii)
            List<DailyReport> reports = db.dailyReportDao().getReportsByProjectDirect(projectId);
            double laborCost = 0;
            double totalHours = 0;
            for (DailyReport r : reports) {
                totalHours += r.getHoursWorked();
                Employee emp = db.employeeDao().getEmployeeById(r.getEmployeeId());
                if (emp != null) {
                    laborCost += r.getHoursWorked() * emp.getSalaryPerHour();
                }
            }

            double netProfit = income - expenses - laborCost;

            final double finalIncome = income;
            final double finalExpenses = expenses;
            final double finalLabor = laborCost;
            final double finalProfit = netProfit;
            final double finalHours = totalHours;

            runOnUiThread(() -> {
                tvTotalIncome.setText(String.format("%.2f RON", finalIncome));
                tvTotalExpenses.setText(String.format("%.2f RON", finalExpenses));
                tvTotalLabor.setText(String.format("%.2f RON", finalLabor));
                tvNetProfit.setText(String.format("%.2f RON", finalProfit));
                tvTotalHours.setText("Total Ore Lucrate: " + finalHours);
                
                if (finalProfit >= 0) tvNetProfit.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                else tvNetProfit.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            });
        }).start();
    }

    private void saveNotes() {
        if (currentProject == null) return;
        String notes = etProjectNotes.getText().toString().trim();
        currentProject.setNotes(notes);
        new Thread(() -> {
            db.projectDao().update(currentProject);
            runOnUiThread(() -> Toast.makeText(this, "Notițe salvate!", Toast.LENGTH_SHORT).show());
        }).start();
    }
}