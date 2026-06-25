package com.example.aplicatiegestiunefirma;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.model.Project;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmployeeHoursReportActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView rvEmployeeHours;
    private TextView tvSelectedMonth, tvNoData;
    private EmpHoursAdapter adapter;
    private Calendar selectedCal = Calendar.getInstance();
    private SimpleDateFormat displayMonthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("ro"));
    private SimpleDateFormat dbMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_hours_report);

        db = AppDatabase.getInstance(this);

        rvEmployeeHours = findViewById(R.id.rvEmployeeHours);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvNoData = findViewById(R.id.tvNoData);
        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnPrevMonth = findViewById(R.id.btnPrevMonth);
        ImageView btnNextMonth = findViewById(R.id.btnNextMonth);

        adapter = new EmpHoursAdapter();
        rvEmployeeHours.setLayoutManager(new LinearLayoutManager(this));
        rvEmployeeHours.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnPrevMonth.setOnClickListener(v -> {
            selectedCal.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadData();
        });
        btnNextMonth.setOnClickListener(v -> {
            selectedCal.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadData();
        });

        updateMonthDisplay();
        loadData();
    }

    private void updateMonthDisplay() {
        tvSelectedMonth.setText(displayMonthFormat.format(selectedCal.getTime()));
    }

    private void loadData() {
        String monthKey = dbMonthFormat.format(selectedCal.getTime());
        new Thread(() -> {
            List<Employee> employees = db.employeeDao().getAllEmployeesDirect();
            List<DailyReport> reports = db.dailyReportDao().getReportsByMonthDirect(monthKey);

            // Build project name map
            List<Project> projects = db.projectDao().getAllProjectsDirect();
            Map<Integer, String> projectNameMap = new HashMap<>();
            for (Project p : projects) {
                projectNameMap.put(p.getId(), p.getName());
            }

            // Build data: employee -> (project -> hours)
            Map<Integer, Map<String, Double>> empProjectHours = new LinkedHashMap<>();
            Map<Integer, Double> empTotalHours = new HashMap<>();

            for (DailyReport r : reports) {
                int empId = r.getEmployeeId();
                String projName = r.getProjectName() != null ? r.getProjectName() : "Necunoscut";

                if (!empProjectHours.containsKey(empId)) {
                    empProjectHours.put(empId, new LinkedHashMap<>());
                }
                Map<String, Double> projMap = empProjectHours.get(empId);
                projMap.put(projName, projMap.getOrDefault(projName, 0.0) + r.getHoursWorked());
                empTotalHours.put(empId, empTotalHours.getOrDefault(empId, 0.0) + r.getHoursWorked());
            }

            // Build display items
            List<EmpHoursItem> items = new ArrayList<>();
            for (Employee e : employees) {
                double total = empTotalHours.getOrDefault(e.getId(), 0.0);
                Map<String, Double> breakdown = empProjectHours.get(e.getId());
                if (breakdown != null && !breakdown.isEmpty()) {
                    items.add(new EmpHoursItem(e.getFullName(), total, breakdown));
                }
            }

            runOnUiThread(() -> {
                adapter.setItems(items);
                if (items.isEmpty()) {
                    tvNoData.setVisibility(View.VISIBLE);
                    rvEmployeeHours.setVisibility(View.GONE);
                } else {
                    tvNoData.setVisibility(View.GONE);
                    rvEmployeeHours.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    // Data class
    static class EmpHoursItem {
        String name;
        double totalHours;
        Map<String, Double> projectHours;

        EmpHoursItem(String name, double totalHours, Map<String, Double> projectHours) {
            this.name = name;
            this.totalHours = totalHours;
            this.projectHours = projectHours;
        }
    }

    // Adapter
    class EmpHoursAdapter extends RecyclerView.Adapter<EmpHoursAdapter.VH> {
        private List<EmpHoursItem> items = new ArrayList<>();

        void setItems(List<EmpHoursItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee_hours, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            EmpHoursItem item = items.get(position);
            holder.tvEmpName.setText(item.name);
            holder.tvTotalHours.setText(String.format(Locale.getDefault(), "%.1fh total", item.totalHours));

            holder.llProjectBreakdown.removeAllViews();
            for (Map.Entry<String, Double> entry : item.projectHours.entrySet()) {
                TextView tv = new TextView(holder.itemView.getContext());
                tv.setText(String.format(Locale.getDefault(), "  • %s: %.1fh", entry.getKey(), entry.getValue()));
                tv.setTextSize(13);
                tv.setTextColor(getResources().getColor(R.color.text_secondary, null));
                tv.setPadding(0, 4, 0, 4);
                holder.llProjectBreakdown.addView(tv);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvEmpName, tvTotalHours;
            LinearLayout llProjectBreakdown;

            VH(View v) {
                super(v);
                tvEmpName = v.findViewById(R.id.tvEmpName);
                tvTotalHours = v.findViewById(R.id.tvTotalHours);
                llProjectBreakdown = v.findViewById(R.id.llProjectBreakdown);
            }
        }
    }
}
