package com.example.aplicatiegestiunefirma;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeFullReportActivity extends AppCompatActivity {

    private AppDatabase db;
    private List<Employee> employees = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_full_report);

        db = AppDatabase.getInstance(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGenerateReport).setOnClickListener(v -> showGenerateDialog());

        new Thread(() -> {
            employees = db.employeeDao().getAllEmployeesDirect();
        }).start();
    }

    private void showGenerateDialog() {
        if (employees.isEmpty()) {
            Toast.makeText(this, "Nu exista angajati!", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_select_employee_period, null);
        Spinner spinnerEmp = dialogView.findViewById(R.id.spinnerEmployeeReport);
        TextInputEditText etStart = dialogView.findViewById(R.id.etReportPeriodStart);
        TextInputEditText etEnd = dialogView.findViewById(R.id.etReportPeriodEnd);

        List<String> names = new ArrayList<>();
        for (Employee e : employees) names.add(e.getFullName());
        spinnerEmp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        String thisMonth = sdf.format(new Date());
        etStart.setText(thisMonth + "-01");
        etEnd.setText(thisMonth + "-31");

        new AlertDialog.Builder(this)
                .setTitle("Genereaza Fisa Angajat")
                .setView(dialogView)
                .setPositiveButton("Genereaza PDF", (d, w) -> {
                    Employee emp = employees.get(spinnerEmp.getSelectedItemPosition());
                    String start = etStart.getText().toString();
                    String end = etEnd.getText().toString();
                    generateEmployeeReport(emp, start, end);
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    private void generateEmployeeReport(Employee emp, String startDate, String endDate) {
        new Thread(() -> {
            try {
                // Fetch all reports for employee in date range
                List<DailyReport> reports = new ArrayList<>();
                List<DailyReport> all = db.dailyReportDao().getReportsByEmployeeDirect(emp.getId());
                for (DailyReport r : all) {
                    if (r.getDate() != null && r.getDate().compareTo(startDate) >= 0 && r.getDate().compareTo(endDate) <= 0) {
                        reports.add(r);
                    }
                }

                PdfDocument document = new PdfDocument();
                Paint paint = new Paint();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                int y = 30;

                // Header
                paint.setColor(Color.parseColor("#6366F1"));
                canvas.drawRect(0, 0, 595, 90, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(22f);
                canvas.drawText("Fisa Angajat", 30, 40, paint);
                paint.setTextSize(12f);
                canvas.drawText(emp.getFullName() + " | " + emp.getPosition(), 30, 65, paint);
                paint.setTextSize(10f);
                canvas.drawText("Perioada: " + startDate + " - " + endDate, 30, 82, paint);
                y = 110;

                // Stats
                double totalHours = reports.stream().mapToDouble(DailyReport::getHoursWorked).sum();
                paint.setColor(Color.DKGRAY);
                paint.setTextSize(11f);
                canvas.drawText("Total ore lucrate: " + String.format("%.1f", totalHours), 30, y, paint); y += 20;
                canvas.drawText("Total rapoarte: " + reports.size(), 30, y, paint); y += 20;

                // Calculate salary
                double salary = 0;
                if ("HOURLY".equals(emp.getPaymentType())) {
                    salary = totalHours * emp.getPaymentRate();
                    canvas.drawText("Salariu: " + String.format("%.2f RON", salary), 30, y, paint); y += 20;
                } else if ("FIXED".equals(emp.getPaymentType()) && !reports.isEmpty()) {
                    salary = emp.getPaymentRate();
                    canvas.drawText("Salariu lunar fix: " + String.format("%.2f RON", salary), 30, y, paint); y += 20;
                }
                y += 10;

                // Table header
                paint.setColor(Color.parseColor("#E5E7EB"));
                canvas.drawRect(20, y - 5, 575, y + 14, paint);
                paint.setColor(Color.DKGRAY);
                paint.setTextSize(10f);
                canvas.drawText("Data", 25, y + 10, paint);
                canvas.drawText("Proiect", 120, y + 10, paint);
                canvas.drawText("Ore", 300, y + 10, paint);
                canvas.drawText("Descriere", 350, y + 10, paint);
                y += 24;

                for (DailyReport r : reports) {
                    if (y > 790) {
                        document.finishPage(page);
                        page = document.startPage(new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create());
                        canvas = page.getCanvas();
                        y = 30;
                    }
                    paint.setColor(Color.BLACK);
                    paint.setTextSize(9f);
                    canvas.drawText(r.getDate(), 25, y, paint);
                    String proj = r.getProjectName(); if (proj != null && proj.length() > 20) proj = proj.substring(0, 17) + "...";
                    canvas.drawText(proj != null ? proj : "-", 120, y, paint);
                    canvas.drawText(String.valueOf(r.getHoursWorked()), 300, y, paint);
                    String desc = r.getReportText(); if (desc != null && desc.length() > 25) desc = desc.substring(0, 22) + "...";
                    canvas.drawText(desc != null ? desc : "-", 350, y, paint);
                    if (r.hasLocation()) {
                        paint.setColor(Color.parseColor("#6366F1"));
                        paint.setTextSize(8f);
                        canvas.drawText("GPS: " + String.format("%.4f,%.4f", r.getLatitude(), r.getLongitude()), 25, y + 10, paint);
                        y += 10;
                    }
                    y += 16;

                    paint.setColor(Color.parseColor("#F3F4F6"));
                    canvas.drawLine(20, y, 575, y, paint);
                    y += 4;
                }

                document.finishPage(page);

                String fileName = "Fisa_" + emp.getLastName() + "_" + startDate.substring(0, 7) + ".pdf";
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
                FileOutputStream fos = new FileOutputStream(file);
                document.writeTo(fos);
                document.close(); fos.close();

                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
                runOnUiThread(() -> {
                    Toast.makeText(this, "PDF generat: " + fileName, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Deschide PDF"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
