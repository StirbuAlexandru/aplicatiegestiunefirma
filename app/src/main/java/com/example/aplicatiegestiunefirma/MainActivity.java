package com.example.aplicatiegestiunefirma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Company;
import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.model.Invoice;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvWelcome, tvDate, tvStatPayroll, tvStatInvoices, tvStatIncome, tvStatProfit;
    private TextView tvTrendPayroll, tvTrendInvoices, tvTrendIncome, tvTrendProfit;
    private ImageView ivSyncStatus;
    private SharedPreferences sharedPreferences;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        
        if (!sharedPreferences.contains("token")) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Check token expiry (30 days)
        long tokenCreated = sharedPreferences.getLong("token_created_at", 0);
        long daysSinceCreation = (System.currentTimeMillis() - tokenCreated) / (1000 * 60 * 60 * 24);
        if (tokenCreated > 0 && daysSinceCreation >= 28) {
            // Token nearing expiry (28 of 30 days) - force re-login
            Toast.makeText(this, "Sesiunea a expirat. Te rugăm să te autentifici din nou.", Toast.LENGTH_LONG).show();
            sharedPreferences.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        db = AppDatabase.getInstance(this);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvDate = findViewById(R.id.tvDate);
        tvStatPayroll = findViewById(R.id.tvStatPayroll);
        tvStatInvoices = findViewById(R.id.tvStatInvoices);
        tvStatIncome = findViewById(R.id.tvStatIncome);
        tvStatProfit = findViewById(R.id.tvStatProfit);
        tvTrendPayroll = findViewById(R.id.tvTrendPayroll);
        tvTrendInvoices = findViewById(R.id.tvTrendInvoices);
        tvTrendIncome = findViewById(R.id.tvTrendIncome);
        tvTrendProfit = findViewById(R.id.tvTrendProfit);
        ivSyncStatus = findViewById(R.id.ivSyncStatus);

        SimpleDateFormat df = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
        tvDate.setText(df.format(Calendar.getInstance().getTime()));

        setupMenuCards();
        loadStatistics();
        loadCharts();
        checkCompanyAndSync();
        scheduleInvoiceNotifications();

        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutConfirmation());
        setupDarkModeButton();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_projects) { startActivity(new Intent(this, ProjectActivity.class)); return true; }
            else if (id == R.id.nav_invoices) { startActivity(new Intent(this, InvoiceActivity.class)); return true; }
            else if (id == R.id.nav_reports) { startActivity(new Intent(this, ReportsActivity.class)); return true; }
            return true;
        });
    }

    private void setupDarkModeButton() {
        ImageView ivDarkModeIcon = findViewById(R.id.ivDarkModeIcon);
        int currentMode = sharedPreferences.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        updateDarkModeIcon(ivDarkModeIcon, currentMode);

        findViewById(R.id.btnDarkMode).setOnClickListener(v -> {
            int mode = sharedPreferences.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            // Cycle: FOLLOW_SYSTEM -> NIGHT -> DAY -> FOLLOW_SYSTEM
            int newMode;
            String label;
            if (mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
                newMode = AppCompatDelegate.MODE_NIGHT_YES;
                label = "Dark mode activat";
            } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
                newMode = AppCompatDelegate.MODE_NIGHT_NO;
                label = "Light mode activat";
            } else {
                newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                label = "Urmeaza setarile sistemului";
            }
            sharedPreferences.edit().putInt("night_mode", newMode).apply();
            AppCompatDelegate.setDefaultNightMode(newMode);
            Toast.makeText(this, label, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateDarkModeIcon(ImageView iv, int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            iv.setImageResource(R.drawable.ic_dark_mode);
        } else if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            iv.setImageResource(android.R.drawable.ic_menu_day);
        } else {
            iv.setImageResource(R.drawable.ic_dark_mode);
        }
    }

    private void setupMenuCards() {
        setupCard(R.id.cardEmployees, "Angajați", R.drawable.ic_person, EmployeeActivity.class);
        setupCard(R.id.cardProjects, "Lucrări", R.drawable.ic_nav_projects, ProjectActivity.class);
        setupCard(R.id.cardInvoices, "Facturi", R.drawable.ic_nav_invoices, InvoiceActivity.class);
        setupCard(R.id.cardReports, "Rapoarte", R.drawable.ic_nav_reports, ReportsActivity.class);
        setupCard(R.id.cardPayroll, "Salarii", R.drawable.ic_payroll, PayrollActivity.class);
        setupCard(R.id.cardCompany, "Finanțe", R.drawable.ic_finance, FinanceActivity.class);
        // New tools section
        setupCard(R.id.cardLeaves, "Concedii", R.drawable.ic_calendar, LeaveActivity.class);
        setupCard(R.id.cardCalendar, "Calendar", R.drawable.ic_calendar, ProjectCalendarActivity.class);
        setupCard(R.id.cardBankRec, "Reconciliere", R.drawable.ic_nav_invoices, BankReconciliationActivity.class);
        setupCard(R.id.cardActivityLog, "Jurnal", R.drawable.ic_nav_reports, ActivityLogActivity.class);
        setupCard(R.id.cardWorkSchedule, "Orar Ture", R.drawable.ic_nav_projects, WorkScheduleActivity.class);
        setupCard(R.id.cardFullReport, "Raport PDF", R.drawable.ic_person, EmployeeFullReportActivity.class);

        // PIN Settings button
        findViewById(R.id.btnPinSettings).setOnClickListener(v ->
                startActivity(new Intent(this, PinSetupActivity.class)));
    }

    private void setupCard(int cardId, String title, int iconRes, Class<?> targetActivity) {
        View card = findViewById(cardId);
        TextView tvTitle = card.findViewById(R.id.tvMenuTitle);
        ImageView ivIcon = card.findViewById(R.id.ivMenuIcon);
        
        tvTitle.setText(title);
        ivIcon.setImageResource(iconRes);
        
        card.setOnClickListener(v -> startActivity(new Intent(this, targetActivity)));
    }

    private void loadStatistics() {
        new Thread(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            String currentMonth = sdf.format(cal.getTime());
            cal.add(Calendar.MONTH, -1);
            String prevMonth = sdf.format(cal.getTime());
            
            List<DailyReport> reports = db.dailyReportDao().getReportsByMonthDirect(currentMonth);
            List<DailyReport> prevReports = db.dailyReportDao().getReportsByMonthDirect(prevMonth);
            List<Employee> employees = db.employeeDao().getAllEmployeesDirect();
            
            // Current month payroll
            Map<Integer, Double> hoursMap = new HashMap<>();
            Map<Integer, Set<String>> daysMap = new HashMap<>();
            for (DailyReport r : reports) {
                hoursMap.put(r.getEmployeeId(), hoursMap.getOrDefault(r.getEmployeeId(), 0.0) + r.getHoursWorked());
                if (!daysMap.containsKey(r.getEmployeeId())) daysMap.put(r.getEmployeeId(), new HashSet<>());
                daysMap.get(r.getEmployeeId()).add(r.getDate());
            }

            double totalPayroll = 0;
            for (Employee e : employees) {
                double hours = hoursMap.getOrDefault(e.getId(), 0.0);
                int days = daysMap.containsKey(e.getId()) ? daysMap.get(e.getId()).size() : 0;
                if ("HOURLY".equals(e.getPaymentType())) totalPayroll += hours * e.getPaymentRate();
                else if ("DAILY".equals(e.getPaymentType())) totalPayroll += days * e.getPaymentRate();
                else if ("FIXED".equals(e.getPaymentType()) && days > 0) totalPayroll += e.getPaymentRate();
            }

            // Previous month payroll
            Map<Integer, Double> prevHoursMap = new HashMap<>();
            Map<Integer, Set<String>> prevDaysMap = new HashMap<>();
            for (DailyReport r : prevReports) {
                prevHoursMap.put(r.getEmployeeId(), prevHoursMap.getOrDefault(r.getEmployeeId(), 0.0) + r.getHoursWorked());
                if (!prevDaysMap.containsKey(r.getEmployeeId())) prevDaysMap.put(r.getEmployeeId(), new HashSet<>());
                prevDaysMap.get(r.getEmployeeId()).add(r.getDate());
            }
            double prevPayroll = 0;
            for (Employee e : employees) {
                double hours = prevHoursMap.getOrDefault(e.getId(), 0.0);
                int days = prevDaysMap.containsKey(e.getId()) ? prevDaysMap.get(e.getId()).size() : 0;
                if ("HOURLY".equals(e.getPaymentType())) prevPayroll += hours * e.getPaymentRate();
                else if ("DAILY".equals(e.getPaymentType())) prevPayroll += days * e.getPaymentRate();
                else if ("FIXED".equals(e.getPaymentType()) && days > 0) prevPayroll += e.getPaymentRate();
            }

            // Invoices
            List<Invoice> allInvoices = db.invoiceDao().getAllInvoicesDirect();
            double totalUnpaid = 0;
            double currentIncome = 0, prevIncome = 0;
            double currentExpense = 0, prevExpense = 0;
            for (Invoice i : allInvoices) {
                if (!i.isPaid() && "INCOME".equals(i.getType())) totalUnpaid += i.getAmount();
                if (i.getDate() != null && i.getDate().length() >= 7) {
                    String iMonth = i.getDate().substring(0, 7);
                    if (currentMonth.equals(iMonth)) {
                        if ("INCOME".equals(i.getType())) currentIncome += i.getAmount();
                        else currentExpense += i.getAmount();
                    } else if (prevMonth.equals(iMonth)) {
                        if ("INCOME".equals(i.getType())) prevIncome += i.getAmount();
                        else prevExpense += i.getAmount();
                    }
                }
            }

            double currentProfit = currentIncome - currentExpense - totalPayroll;
            double prevProfit = prevIncome - prevExpense - prevPayroll;

            final double fPayroll = totalPayroll, fPrevPayroll = prevPayroll;
            final double fUnpaid = totalUnpaid;
            final double fIncome = currentIncome, fPrevIncome = prevIncome;
            final double fProfit = currentProfit, fPrevProfit = prevProfit;

            runOnUiThread(() -> {
                tvStatPayroll.setText(String.format(Locale.getDefault(), "%.0f RON", fPayroll));
                tvStatInvoices.setText(String.format(Locale.getDefault(), "%.0f RON", fUnpaid));
                tvStatIncome.setText(String.format(Locale.getDefault(), "%.0f RON", fIncome));
                tvStatProfit.setText(String.format(Locale.getDefault(), "%.0f RON", fProfit));
                if (fProfit < 0) tvStatProfit.setTextColor(0xFFE53935);

                setTrend(tvTrendPayroll, fPayroll, fPrevPayroll, true);
                setTrend(tvTrendInvoices, fUnpaid, 0, true); // no prev comparison for unpaid
                setTrend(tvTrendIncome, fIncome, fPrevIncome, false);
                setTrend(tvTrendProfit, fProfit, fPrevProfit, false);
            });
        }).start();
    }

    private void setTrend(TextView tvTrend, double current, double previous, boolean lowerIsBetter) {
        if (previous == 0 && current == 0) {
            tvTrend.setText("");
            return;
        }
        if (previous == 0) {
            tvTrend.setText("\u25B2"); // up arrow
            tvTrend.setTextColor(lowerIsBetter ? 0xFFE53935 : 0xFF4CAF50);
            return;
        }
        double change = ((current - previous) / previous) * 100;
        if (Math.abs(change) < 0.5) {
            tvTrend.setText("\u2194"); // sideways
            tvTrend.setTextColor(0xFF9E9E9E);
        } else if (change > 0) {
            tvTrend.setText(String.format(Locale.getDefault(), "\u25B2%.0f%%", change));
            tvTrend.setTextColor(lowerIsBetter ? 0xFFE53935 : 0xFF4CAF50);
        } else {
            tvTrend.setText(String.format(Locale.getDefault(), "\u25BC%.0f%%", Math.abs(change)));
            tvTrend.setTextColor(lowerIsBetter ? 0xFF4CAF50 : 0xFFE53935);
        }
    }

    private void checkCompanyAndSync() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        ivSyncStatus.setImageResource(android.R.drawable.presence_away);
        RetrofitClient.getApiService().getCompany(token).enqueue(new Callback<Company>() {
            @Override
            public void onResponse(Call<Company> call, Response<Company> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Company server = response.body();
                    runOnUiThread(() -> {
                        tvWelcome.setText("Bună, " + server.getName() + "!");
                        ivSyncStatus.setImageResource(android.R.drawable.presence_online);
                    });
                    if (server.getName() != null && !server.getName().isEmpty()) {
                        new Thread(() -> {
                            List<Company> local = db.companyDao().getAllCompaniesDirect();
                            if (!local.isEmpty()) {
                                server.setId(local.get(0).getId());
                                db.companyDao().update(server);
                            } else {
                                db.companyDao().insert(server);
                            }
                        }).start();
                    }
                } else {
                    loadLocalCompanyName();
                    runOnUiThread(() -> ivSyncStatus.setImageResource(android.R.drawable.presence_offline));
                }
            }
            @Override
            public void onFailure(Call<Company> call, Throwable t) {
                loadLocalCompanyName();
                runOnUiThread(() -> ivSyncStatus.setImageResource(android.R.drawable.presence_offline));
            }
        });
    }

    private void loadLocalCompanyName() {
        new Thread(() -> {
            List<Company> companies = db.companyDao().getAllCompaniesDirect();
            if (!companies.isEmpty()) {
                runOnUiThread(() -> tvWelcome.setText("Bună, " + companies.get(0).getName() + "!"));
            }
        }).start();
    }

    private void scheduleInvoiceNotifications() {
        PeriodicWorkRequest notifRequest = new PeriodicWorkRequest.Builder(
                InvoiceNotificationWorker.class, 24, java.util.concurrent.TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "invoice_due_check",
                ExistingPeriodicWorkPolicy.KEEP,
                notifRequest);

        // Worker pentru generarea automata a facturilor recurente
        PeriodicWorkRequest recurringRequest = new PeriodicWorkRequest.Builder(
                RecurringInvoiceWorker.class, 24, java.util.concurrent.TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "recurring_invoice_generator",
                ExistingPeriodicWorkPolicy.KEEP,
                recurringRequest);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Deconectare")
                .setMessage("Ești sigur că vrei să te deconectezi?")
                .setPositiveButton("Da", (d, w) -> performLogout())
                .setNegativeButton("Anulează", null)
                .show();
    }

    private void performLogout() {
        sharedPreferences.edit().clear().apply();
        WorkManager.getInstance(this).cancelUniqueWork("invoice_due_check");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadCharts() {
        new Thread(() -> {
            // LINE CHART - Income vs Expenses by month (last 6 months)
            List<Invoice> allInvoices = db.invoiceDao().getAllInvoicesDirect();
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

            TreeMap<String, double[]> monthlyData = new TreeMap<>();
            // Initialize last 6 months
            for (int i = 5; i >= 0; i--) {
                Calendar c = (Calendar) cal.clone();
                c.add(Calendar.MONTH, -i);
                monthlyData.put(monthFmt.format(c.getTime()), new double[]{0, 0});
            }

            for (Invoice inv : allInvoices) {
                if (inv.getDate() != null && inv.getDate().length() >= 7) {
                    String month = inv.getDate().substring(0, 7);
                    if (monthlyData.containsKey(month)) {
                        double[] vals = monthlyData.get(month);
                        if ("INCOME".equals(inv.getType())) vals[0] += inv.getAmount();
                        else vals[1] += inv.getAmount();
                    }
                }
            }

            List<Entry> incomeEntries = new ArrayList<>();
            List<Entry> expenseEntries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            int idx = 0;
            for (Map.Entry<String, double[]> entry : monthlyData.entrySet()) {
                incomeEntries.add(new Entry(idx, (float) entry.getValue()[0]));
                expenseEntries.add(new Entry(idx, (float) entry.getValue()[1]));
                labels.add(entry.getKey().substring(5)); // MM only
                idx++;
            }

            // PIE CHART - Salary distribution for current month
            String currentMonth = monthFmt.format(cal.getTime());
            List<Employee> employees = db.employeeDao().getAllEmployeesDirect();
            List<DailyReport> monthReports = db.dailyReportDao().getReportsByMonthDirect(currentMonth);

            Map<Integer, Double> hoursMap = new HashMap<>();
            Map<Integer, Set<String>> daysMap = new HashMap<>();
            for (DailyReport r : monthReports) {
                hoursMap.put(r.getEmployeeId(), hoursMap.getOrDefault(r.getEmployeeId(), 0.0) + r.getHoursWorked());
                if (!daysMap.containsKey(r.getEmployeeId())) daysMap.put(r.getEmployeeId(), new HashSet<>());
                daysMap.get(r.getEmployeeId()).add(r.getDate());
            }

            List<PieEntry> pieEntries = new ArrayList<>();
            for (Employee e : employees) {
                double salary = 0;
                double hours = hoursMap.getOrDefault(e.getId(), 0.0);
                int days = daysMap.containsKey(e.getId()) ? daysMap.get(e.getId()).size() : 0;
                if ("HOURLY".equals(e.getPaymentType())) salary = hours * e.getPaymentRate();
                else if ("DAILY".equals(e.getPaymentType())) salary = days * e.getPaymentRate();
                else if ("FIXED".equals(e.getPaymentType()) && days > 0) salary = e.getPaymentRate();
                if (salary > 0) pieEntries.add(new PieEntry((float) salary, e.getFullName()));
            }

            runOnUiThread(() -> {
                // Setup Line Chart
                LineChart lineChart = findViewById(R.id.lineChart);
                LineDataSet incomeSet = new LineDataSet(incomeEntries, "Venituri");
                incomeSet.setColor(Color.parseColor("#4CAF50"));
                incomeSet.setCircleColor(Color.parseColor("#4CAF50"));
                incomeSet.setLineWidth(2f);

                LineDataSet expenseSet = new LineDataSet(expenseEntries, "Cheltuieli");
                expenseSet.setColor(Color.parseColor("#E53935"));
                expenseSet.setCircleColor(Color.parseColor("#E53935"));
                expenseSet.setLineWidth(2f);

                lineChart.setData(new LineData(incomeSet, expenseSet));
                lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                lineChart.getXAxis().setGranularity(1f);
                lineChart.getDescription().setEnabled(false);
                lineChart.animateX(1000);
                lineChart.invalidate();

                // Setup Pie Chart
                PieChart pieChart = findViewById(R.id.pieChart);
                if (!pieEntries.isEmpty()) {
                    PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
                    pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                    pieDataSet.setValueTextSize(10f);
                    pieDataSet.setValueTextColor(Color.WHITE);
                    pieChart.setData(new PieData(pieDataSet));
                    pieChart.setUsePercentValues(true);
                }
                pieChart.getDescription().setEnabled(false);
                pieChart.setCenterText("Salarii");
                pieChart.animateY(1000);
                pieChart.invalidate();
            });
        }).start();
    }
}
