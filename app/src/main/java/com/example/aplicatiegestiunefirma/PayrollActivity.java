package com.example.aplicatiegestiunefirma;

import android.content.SharedPreferences;
import android.content.Intent;
import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Company;
import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PayrollActivity extends AppCompatActivity {

    private AppDatabase db;
    private SharedPreferences sharedPreferences;
    private RecyclerView rvPayroll;
    private PayrollAdapter adapter;
    private TextView tvGrandTotal, tvSelectedMonth;
    private Calendar selectedCal = Calendar.getInstance();
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private Company currentCompany;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payroll);

        db = AppDatabase.getInstance(this);
        sharedPreferences = getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);
        rvPayroll = findViewById(R.id.rvPayroll);
        tvGrandTotal = findViewById(R.id.tvGrandTotalPayroll);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);

        adapter = new PayrollAdapter();
        rvPayroll.setLayoutManager(new LinearLayoutManager(this));
        rvPayroll.setAdapter(adapter);

        new Thread(() -> {
            List<Company> companies = db.companyDao().getAllCompaniesDirect();
            if (companies != null && !companies.isEmpty()) {
                currentCompany = companies.get(0);
            }
        }).start();

        // Sync employees and reports from server, then calculate payroll
        syncFromServerThenLoadPayroll();

        findViewById(R.id.btnChangeMonth).setOnClickListener(v -> showMonthPicker());
        findViewById(R.id.btnExportPayrollPdf).setOnClickListener(v -> exportPayrollPdf());
        findViewById(R.id.btnExportPayrollCsv).setOnClickListener(v -> exportPayrollCsv());

        setupNavigation();
    }

    private void syncFromServerThenLoadPayroll() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        // Sync employees
        RetrofitClient.getApiService().getEmployees(token).enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null)
                    new Thread(() -> { for (Employee e : response.body()) db.employeeDao().insert(e); }).start();
                // Sync reports after employees
                syncReportsThenCalculate(token);
            }
            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {
                syncReportsThenCalculate(token);
            }
        });
    }

    private void syncReportsThenCalculate(String token) {
        RetrofitClient.getApiService().getReports(token, null).enqueue(new Callback<List<DailyReport>>() {
            @Override
            public void onResponse(Call<List<DailyReport>> call, Response<List<DailyReport>> response) {
                if (response.isSuccessful() && response.body() != null)
                    new Thread(() -> { for (DailyReport r : response.body()) db.dailyReportDao().insert(r); }).start();
                updateMonthDisplay();
            }
            @Override
            public void onFailure(Call<List<DailyReport>> call, Throwable t) {
                updateMonthDisplay();
            }
        });
    }

    private void updateMonthDisplay() {
        tvSelectedMonth.setText(monthYearFormat.format(selectedCal.getTime()));
        loadPayrollData();
    }

    private void showMonthPicker() {
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedCal.set(Calendar.YEAR, year);
            selectedCal.set(Calendar.MONTH, month);
            updateMonthDisplay();
        }, selectedCal.get(Calendar.YEAR), selectedCal.get(Calendar.MONTH), 1);
        dialog.show();
    }

    private void loadPayrollData() {
        String monthKey = dbMonthFormat.format(selectedCal.getTime());
        new Thread(() -> {
            List<Employee> allEmployees = db.employeeDao().getAllEmployeesDirect();
            List<DailyReport> monthReports = db.dailyReportDao().getReportsByMonthDirect(monthKey);
            
            // Map pentru orele totale per angajat
            Map<Integer, Double> totalHoursMap = new HashMap<>();
            // Set pentru a numara zilele unice lucrate per angajat
            Map<Integer, Set<String>> workedDaysMap = new HashMap<>();

            for (DailyReport r : monthReports) {
                int empId = r.getEmployeeId();
                totalHoursMap.put(empId, totalHoursMap.getOrDefault(empId, 0.0) + r.getHoursWorked());
                
                if (!workedDaysMap.containsKey(empId)) {
                    workedDaysMap.put(empId, new HashSet<>());
                }
                workedDaysMap.get(empId).add(r.getDate());
            }

            List<PayrollRecord> payrollList = new ArrayList<>();
            double grandTotal = 0;

            for (Employee e : allEmployees) {
                double hours = totalHoursMap.getOrDefault(e.getId(), 0.0);
                int daysWorked = workedDaysMap.containsKey(e.getId()) ? workedDaysMap.get(e.getId()).size() : 0;

                double totalPerEmp = 0;
                String details = "";
                double overtimeHours = 0;
                double overtimePay = 0;

                if ("HOURLY".equals(e.getPaymentType())) {
                    // Calcul ore suplimentare: grupare pe zile
                    Map<String, Double> dailyHours = new HashMap<>();
                    for (DailyReport r : monthReports) {
                        if (r.getEmployeeId() == e.getId()) {
                            dailyHours.put(r.getDate(),
                                    dailyHours.getOrDefault(r.getDate(), 0.0) + r.getHoursWorked());
                        }
                    }
                    double regularHours = 0;
                    for (double dayH : dailyHours.values()) {
                        if (dayH > 8.0) {
                            regularHours += 8.0;
                            overtimeHours += dayH - 8.0;
                        } else {
                            regularHours += dayH;
                        }
                    }
                    double regularPay = regularHours * e.getPaymentRate();
                    overtimePay = overtimeHours * e.getPaymentRate() * 1.5;
                    totalPerEmp = regularPay + overtimePay;
                    if (overtimeHours > 0) {
                        details = String.format(Locale.getDefault(),
                                "%.1f ore normale + %.1f ore extra (1.5x) x %.2f RON/h",
                                regularHours, overtimeHours, e.getPaymentRate());
                    } else {
                        details = String.format(Locale.getDefault(),
                                "%.1f ore x %.2f RON/h", hours, e.getPaymentRate());
                    }
                } else if ("DAILY".equals(e.getPaymentType())) {
                    Map<String, Double> dailyHoursPerEmp = new HashMap<>();
                    for (DailyReport r : monthReports) {
                        if (r.getEmployeeId() == e.getId()) {
                            dailyHoursPerEmp.put(r.getDate(),
                                    dailyHoursPerEmp.getOrDefault(r.getDate(), 0.0) + r.getHoursWorked());
                        }
                    }
                    int fullDays = 0, halfDays = 0;
                    for (double dayH : dailyHoursPerEmp.values()) {
                        if (dayH >= 6.0) fullDays++;
                        else halfDays++;
                    }
                    totalPerEmp = fullDays * e.getPaymentRate() + halfDays * (e.getPaymentRate() / 2.0);
                    if (halfDays > 0) {
                        details = String.format(Locale.getDefault(),
                                "%d zile întregi + %d zile scurte (<6h) x %.2f RON/zi",
                                fullDays, halfDays, e.getPaymentRate());
                    } else {
                        details = String.format(Locale.getDefault(),
                                "%d zile x %.2f RON/zi", fullDays, e.getPaymentRate());
                    }
                } else if ("FIXED".equals(e.getPaymentType())) {
                    if (daysWorked > 0) {
                        totalPerEmp = e.getPaymentRate();
                        details = "Salariu lunar fix";
                    }
                }

                if (totalPerEmp > 0) {
                    PayrollRecord rec = new PayrollRecord(e.getFullName(), details, totalPerEmp);
                    rec.overtimeHours = overtimeHours;
                    rec.overtimePay = overtimePay;
                    payrollList.add(rec);
                    grandTotal += totalPerEmp;
                }
            }

            final double finalTotal = grandTotal;
            runOnUiThread(() -> {
                adapter.setData(payrollList);
                tvGrandTotal.setText(String.format(Locale.getDefault(), "%.2f RON", finalTotal));
            });
        }).start();
    }

    private void exportPayrollPdf() {
        if (adapter.getItemCount() == 0) {
            Toast.makeText(this, "Nu sunt date de exportat!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        int pageNum = 1;
        int pageWidth = 595;
        int pageHeight = 842;
        int marginTop = 40;
        int marginBottom = 60; // space for signature
        int y = marginTop;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // === HEADER with company logo placeholder ===
        paint.setTextSize(10);
        paint.setColor(Color.GRAY);
        if (currentCompany != null) {
            // Logo placeholder (rectangle)
            Paint logoPaint = new Paint();
            logoPaint.setColor(Color.parseColor("#1565C0"));
            canvas.drawRect(40, y, 90, y + 50, logoPaint);
            logoPaint.setColor(Color.WHITE);
            logoPaint.setTextSize(12);
            logoPaint.setFakeBoldText(true);
            canvas.drawText("LOGO", 48, y + 30, logoPaint);

            paint.setColor(Color.BLACK);
            paint.setTextSize(14);
            paint.setFakeBoldText(true);
            canvas.drawText(currentCompany.getName(), 100, y + 18, paint);
            paint.setTextSize(10);
            paint.setFakeBoldText(false);
            canvas.drawText("CUI: " + currentCompany.getTaxId() + "  |  Reg.Com: " + currentCompany.getRegCom(), 100, y + 33, paint);
            canvas.drawText(currentCompany.getAddress() != null ? currentCompany.getAddress() : "", 100, y + 46, paint);
        }
        y += 65;

        // Title
        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#1565C0"));
        canvas.drawText("STAT DE PLATĂ - " + tvSelectedMonth.getText().toString().toUpperCase(), 40, y, paint);
        y += 30;

        // Table header
        paint.setStrokeWidth(2f);
        paint.setColor(Color.BLACK);
        canvas.drawLine(40, y, 555, y, paint);
        y += 25;
        paint.setTextSize(11);
        paint.setFakeBoldText(true);
        canvas.drawText("NUME ANGAJAT", 45, y, paint);
        canvas.drawText("DETALII CALCUL", 250, y, paint);
        canvas.drawText("TOTAL (RON)", 460, y, paint);
        paint.setStrokeWidth(1f);
        canvas.drawLine(40, y + 10, 555, y + 10, paint);
        y += 30;
        paint.setFakeBoldText(false);

        for (PayrollRecord r : adapter.records) {
            if (y > pageHeight - marginBottom - 60) {
                // Finish current page and start new one
                document.finishPage(page);
                pageNum++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = marginTop + 20;
                // Repeat header on new page
                paint.setTextSize(11);
                paint.setFakeBoldText(true);
                canvas.drawText("NUME ANGAJAT", 45, y, paint);
                canvas.drawText("DETALII CALCUL", 250, y, paint);
                canvas.drawText("TOTAL (RON)", 460, y, paint);
                canvas.drawLine(40, y + 10, 555, y + 10, paint);
                y += 30;
                paint.setFakeBoldText(false);
            }
            paint.setTextSize(11);
            canvas.drawText(r.name, 45, y, paint);
            canvas.drawText(r.details, 250, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", r.total), 460, y, paint);
            y += 25;
        }

        // Grand total
        y += 15;
        canvas.drawLine(350, y, 555, y, paint);
        y += 25;
        paint.setFakeBoldText(true);
        paint.setTextSize(14);
        canvas.drawText("TOTAL GENERAL:", 300, y, paint);
        canvas.drawText(tvGrandTotal.getText().toString(), 450, y, paint);

        // === SIGNATURE / STAMP placeholder ===
        y += 50;
        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        paint.setColor(Color.GRAY);
        canvas.drawText("Semnătura angajator: _______________________", 40, y, paint);
        canvas.drawText("Ștampila:", 350, y, paint);
        // Stamp circle placeholder
        Paint stampPaint = new Paint();
        stampPaint.setColor(Color.LTGRAY);
        stampPaint.setStyle(Paint.Style.STROKE);
        stampPaint.setStrokeWidth(2f);
        canvas.drawCircle(430, y + 30, 30, stampPaint);
        stampPaint.setTextSize(8);
        stampPaint.setStyle(Paint.Style.FILL);
        canvas.drawText("ȘTAMPILĂ", 410, y + 33, stampPaint);

        document.finishPage(page);
        String fileName = "Stat_Salarii_" + tvSelectedMonth.getText().toString().replace(" ", "_") + ".pdf";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
        
        try {
            document.writeTo(new FileOutputStream(file));
            runOnUiThread(() -> openPdf(file));
        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(this, "Eroare la generare PDF", Toast.LENGTH_SHORT).show());
        }
        document.close();
    }

    private void exportPayrollCsv() {
        if (adapter.getItemCount() == 0) {
            Toast.makeText(this, "Nu sunt date de exportat!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Stat_Salarii_" + tvSelectedMonth.getText().toString().replace(" ", "_") + ".csv";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Nume Angajat,Detalii Calcul,Total (RON)\n");
            for (PayrollRecord r : adapter.records) {
                writer.write(String.format("\"%s\",\"%s\",%.2f\n", r.name, r.details, r.total));
            }
            writer.write(String.format("\nTOTAL GENERAL,,\"%s\"\n", tvGrandTotal.getText().toString()));
            Toast.makeText(this, "CSV exportat!", Toast.LENGTH_SHORT).show();
            openCsv(file);
        } catch (IOException e) {
            Toast.makeText(this, "Eroare la export CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCsv(File file) {
        Uri path = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "text/csv");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Deschide CSV"));
        } catch (Exception e) {
            Toast.makeText(this, "CSV salvat: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private void openPdf(File file) {
        Uri path = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Deschide Stat Salarii"));
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_reports); // Folosim raport ca highlight
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, MainActivity.class)); finish(); return true; }
            else if (id == R.id.nav_projects) { startActivity(new Intent(this, ProjectActivity.class)); finish(); return true; }
            else if (id == R.id.nav_invoices) { startActivity(new Intent(this, InvoiceActivity.class)); finish(); return true; }
            else if (id == R.id.nav_reports) { startActivity(new Intent(this, ReportsActivity.class)); finish(); return true; }
            return true;
        });
    }

    static class PayrollRecord {
        String name, details;
        double total;
        double overtimeHours = 0;
        double overtimePay = 0;
        PayrollRecord(String name, String details, double total) {
            this.name = name; this.details = details; this.total = total;
        }
    }

    class PayrollAdapter extends RecyclerView.Adapter<PayrollAdapter.ViewHolder> {
        List<PayrollRecord> records = new ArrayList<>();
        void setData(List<PayrollRecord> data) { this.records = data; notifyDataSetChanged(); }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_payroll, p, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            PayrollRecord r = records.get(pos);
            h.tvName.setText(r.name);
            h.tvDetails.setText(r.details);
            h.tvTotal.setText(String.format(Locale.getDefault(), "%.2f RON", r.total));
            if (r.overtimeHours > 0) {
                h.tvOvertime.setVisibility(android.view.View.VISIBLE);
                h.tvOvertime.setText(String.format(Locale.getDefault(),
                        "Ore suplimentare: %.1fh = +%.2f RON (1.5x)",
                        r.overtimeHours, r.overtimePay));
            } else {
                h.tvOvertime.setVisibility(android.view.View.GONE);
            }
        }
        @Override public int getItemCount() { return records.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDetails, tvTotal, tvOvertime;
            ViewHolder(View v) { super(v);
                tvName = v.findViewById(R.id.tvPayrollEmpName);
                tvDetails = v.findViewById(R.id.tvPayrollDetails);
                tvTotal = v.findViewById(R.id.tvTotalPerEmp);
                tvOvertime = v.findViewById(R.id.tvOvertimeInfo);
            }
        }
    }
}
