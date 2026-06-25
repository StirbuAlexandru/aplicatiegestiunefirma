package com.example.aplicatiegestiunefirma;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.model.Invoice;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

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

public class FinanceActivity extends AppCompatActivity {

    private TextView tvEstimatedProfit, tvToReceive, tvToPay, tvRealIncome, tvRealExpense, tvPayroll;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finance);

        db = AppDatabase.getInstance(this);

        tvEstimatedProfit = findViewById(R.id.tvEstimatedProfit);
        tvToReceive = findViewById(R.id.tvToReceive);
        tvToPay = findViewById(R.id.tvToPay);
        tvRealIncome = findViewById(R.id.tvRealIncome);
        tvRealExpense = findViewById(R.id.tvRealExpense);
        tvPayroll = findViewById(R.id.tvFinancePayroll);

        loadFinancialData();
        loadProfitabilityChart();
    }

    private void loadFinancialData() {
        new Thread(() -> {
            // 1. Obține toate datele necesare
            List<Invoice> allInvoices = db.invoiceDao().getAllInvoicesDirect();
            List<Employee> allEmployees = db.employeeDao().getAllEmployeesDirect();
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            String currentMonth = sdf.format(Calendar.getInstance().getTime());
            List<DailyReport> monthReports = db.dailyReportDao().getReportsByMonthDirect(currentMonth);

            // 2. Calculează Salariile pe luna curentă
            Map<Integer, Double> hoursMap = new HashMap<>();
            for (DailyReport r : monthReports) {
                hoursMap.put(r.getEmployeeId(), hoursMap.getOrDefault(r.getEmployeeId(), 0.0) + r.getHoursWorked());
            }

            double totalPayroll = 0;
            for (Employee e : allEmployees) {
                double hours = hoursMap.getOrDefault(e.getId(), 0.0);
                totalPayroll += (hours * e.getSalaryPerHour());
            }

            // 3. Calculează Facturile
            double toReceive = 0; // Income Neplatit
            double toPayInvoices = 0; // Expense Neplatit
            double realIncome = 0; // Income Platit
            double realExpense = 0; // Expense Platit

            for (Invoice i : allInvoices) {
                if ("INCOME".equals(i.getType())) {
                    if (i.isPaid()) realIncome += i.getAmount();
                    else toReceive += i.getAmount();
                } else {
                    if (i.isPaid()) realExpense += i.getAmount();
                    else toPayInvoices += i.getAmount();
                }
            }

            // 4. Calcule Finale
            double totalToPay = toPayInvoices + totalPayroll;
            double estimatedProfit = (realIncome + toReceive) - (realExpense + toPayInvoices + totalPayroll);

            // 5. Update UI
            final double fEstimatedProfit = estimatedProfit;
            final double fToReceive = toReceive;
            final double fTotalToPay = totalToPay;
            final double fRealIncome = realIncome;
            final double fRealExpense = realExpense;
            final double fPayroll = totalPayroll;

            runOnUiThread(() -> {
                tvEstimatedProfit.setText(String.format(Locale.getDefault(), "%.2f RON", fEstimatedProfit));
                tvToReceive.setText(String.format(Locale.getDefault(), "%.2f RON", fToReceive));
                tvToPay.setText(String.format(Locale.getDefault(), "%.2f RON", fTotalToPay));
                tvRealIncome.setText(String.format(Locale.getDefault(), "%.2f RON", fRealIncome));
                tvRealExpense.setText(String.format(Locale.getDefault(), "%.2f RON", fRealExpense));
                tvPayroll.setText(String.format(Locale.getDefault(), "%.2f RON", fPayroll));
                
                // Schimbă culoarea profitului dacă e negativ
                if (fEstimatedProfit < 0) tvEstimatedProfit.setTextColor(0xFFE53935); // Red
                else tvEstimatedProfit.setTextColor(0xFF1565C0); // Blue
            });
        }).start();
    }

    private void loadProfitabilityChart() {
        new Thread(() -> {
            List<Invoice> allInvoices = db.invoiceDao().getAllInvoicesDirect();
            List<Employee> allEmployees = db.employeeDao().getAllEmployeesDirect();
            SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            Calendar cal = Calendar.getInstance();

            // Initialize last 6 months
            TreeMap<String, double[]> monthlyData = new TreeMap<>(); // [income, expense, payroll]
            for (int i = 5; i >= 0; i--) {
                Calendar c = (Calendar) cal.clone();
                c.add(Calendar.MONTH, -i);
                monthlyData.put(monthFmt.format(c.getTime()), new double[]{0, 0, 0});
            }

            // Invoices per month
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

            // Payroll per month
            for (String month : monthlyData.keySet()) {
                List<DailyReport> monthReports = db.dailyReportDao().getReportsByMonthDirect(month);
                Map<Integer, Double> hoursMap = new HashMap<>();
                Map<Integer, Set<String>> daysMap = new HashMap<>();
                for (DailyReport r : monthReports) {
                    hoursMap.put(r.getEmployeeId(), hoursMap.getOrDefault(r.getEmployeeId(), 0.0) + r.getHoursWorked());
                    if (!daysMap.containsKey(r.getEmployeeId())) daysMap.put(r.getEmployeeId(), new HashSet<>());
                    daysMap.get(r.getEmployeeId()).add(r.getDate());
                }
                double payroll = 0;
                for (Employee e : allEmployees) {
                    double hours = hoursMap.getOrDefault(e.getId(), 0.0);
                    int days = daysMap.containsKey(e.getId()) ? daysMap.get(e.getId()).size() : 0;
                    if ("HOURLY".equals(e.getPaymentType())) payroll += hours * e.getPaymentRate();
                    else if ("DAILY".equals(e.getPaymentType())) payroll += days * e.getPaymentRate();
                    else if ("FIXED".equals(e.getPaymentType()) && days > 0) payroll += e.getPaymentRate();
                }
                monthlyData.get(month)[2] = payroll;
            }

            // Build chart entries
            List<BarEntry> profitEntries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            int idx = 0;
            for (Map.Entry<String, double[]> entry : monthlyData.entrySet()) {
                double profit = entry.getValue()[0] - entry.getValue()[1] - entry.getValue()[2];
                profitEntries.add(new BarEntry(idx, (float) profit));
                labels.add(entry.getKey().substring(5)); // MM only
                idx++;
            }

            runOnUiThread(() -> {
                BarChart barChart = findViewById(R.id.barChartProfit);
                BarDataSet dataSet = new BarDataSet(profitEntries, "Profit Net (RON)");
                List<Integer> colors = new ArrayList<>();
                for (BarEntry e : profitEntries) {
                    colors.add(e.getY() >= 0 ? Color.parseColor("#4CAF50") : Color.parseColor("#E53935"));
                }
                dataSet.setColors(colors);
                dataSet.setValueTextSize(10f);

                barChart.setData(new BarData(dataSet));
                barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                barChart.getXAxis().setGranularity(1f);
                barChart.getDescription().setEnabled(false);
                barChart.animateY(1000);
                barChart.invalidate();
            });
        }).start();
    }
}
