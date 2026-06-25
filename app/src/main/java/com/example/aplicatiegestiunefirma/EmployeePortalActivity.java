package com.example.aplicatiegestiunefirma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.model.Project;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeePortalActivity extends AppCompatActivity {

    private AppDatabase db;
    private SharedPreferences sharedPreferences;
    private RecyclerView rvReports;
    private ReportAdapter adapter;
    private TextView tvWelcome, tvPosition, tvHoursMonth, tvReportsMonth, tvNoReports;
    private ExtendedFloatingActionButton fabAdd;

    private int employeeId;
    private String employeeName = "";
    private List<Project> projectList = new ArrayList<>();
    private List<DailyReport> myReports = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_portal);

        db = AppDatabase.getInstance(this);
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        employeeId = sharedPreferences.getInt("employee_id", -1);

        tvWelcome = findViewById(R.id.tvWelcomeEmployee);
        tvPosition = findViewById(R.id.tvEmployeePosition);
        tvHoursMonth = findViewById(R.id.tvHoursMonth);
        tvReportsMonth = findViewById(R.id.tvReportsMonth);
        tvNoReports = findViewById(R.id.tvNoReports);
        fabAdd = findViewById(R.id.fabAddReport);
        rvReports = findViewById(R.id.rvEmployeeReports);

        adapter = new ReportAdapter();
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        rvReports.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            if (projectList.isEmpty()) {
                // Projects not loaded yet - fetch first, then open dialog
                String token = "Bearer " + sharedPreferences.getString("token", "");
                Toast.makeText(this, "Se încarcă proiectele...", Toast.LENGTH_SHORT).show();
                RetrofitClient.getApiService().getEmployeeProjects(token).enqueue(new Callback<List<Project>>() {
                    @Override
                    public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                        if (response.isSuccessful() && response.body() != null)
                            projectList = response.body();
                        showAddReportDialog();
                    }
                    @Override
                    public void onFailure(Call<List<Project>> call, Throwable t) {
                        showAddReportDialog();
                    }
                });
            } else {
                showAddReportDialog();
            }
        });

        findViewById(R.id.btnLogoutEmployee).setOnClickListener(v -> showLogoutDialog());

        findViewById(R.id.btnChatAdmin).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("employee_id", employeeId);
            intent.putExtra("employee_name", employeeName);
            intent.putExtra("is_admin", false);
            startActivity(intent);
        });

        loadEmployeeInfo();
        loadProjectsForSelection();
        loadMyReports();
    }

    private void loadEmployeeInfo() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().getEmployeeInfo(token).enqueue(new Callback<Employee>() {
            @Override
            public void onResponse(Call<Employee> call, Response<Employee> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Employee emp = response.body();
                    employeeName = emp.getFirstName() + " " + emp.getLastName();
                    runOnUiThread(() -> {
                        tvWelcome.setText("Bun\u0103 ziua, " + emp.getFirstName() + "!");
                        tvPosition.setText(emp.getPosition() != null ? emp.getPosition() : "Angajat");
                    });
                }
            }
            @Override
            public void onFailure(Call<Employee> call, Throwable t) {
                // Offline - use local if available
                new Thread(() -> {
                    Employee emp = db.employeeDao().getEmployeeById(employeeId);
                    if (emp != null) {
                        employeeName = emp.getFirstName() + " " + emp.getLastName();
                        runOnUiThread(() -> {
                            tvWelcome.setText("Bun\u0103 ziua, " + emp.getFirstName() + "!");
                            tvPosition.setText(emp.getPosition() != null ? emp.getPosition() : "Angajat");
                        });
                    }
                }).start();
            }
        });
    }

    private void loadProjectsForSelection() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().getEmployeeProjects(token).enqueue(new Callback<List<Project>>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    projectList = response.body();
                    // Also cache locally
                    new Thread(() -> {
                        for (Project p : projectList) db.projectDao().insert(p);
                    }).start();
                }
            }
            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {
                new Thread(() -> {
                    projectList = db.projectDao().getAllProjectsDirect();
                }).start();
            }
        });
    }

    private void loadMyReports() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().getEmployeeOwnReports(token).enqueue(new Callback<List<DailyReport>>() {
            @Override
            public void onResponse(Call<List<DailyReport>> call, Response<List<DailyReport>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    myReports = response.body();
                    // Cache locally
                    new Thread(() -> {
                        for (DailyReport r : myReports) db.dailyReportDao().insert(r);
                    }).start();
                    runOnUiThread(() -> updateReportsList());
                }
            }
            @Override
            public void onFailure(Call<List<DailyReport>> call, Throwable t) {
                // Load from local DB on background thread
                new Thread(() -> {
                    List<DailyReport> localReports = db.dailyReportDao().getReportsByEmployeeDirect(employeeId);
                    myReports = localReports != null ? localReports : new ArrayList<>();
                    runOnUiThread(() -> updateReportsList());
                }).start();
            }
        });
    }

    private void updateReportsList() {
        adapter.setReports(myReports);
        tvNoReports.setVisibility(myReports.isEmpty() ? View.VISIBLE : View.GONE);

        // Calculate current month stats
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        double totalHours = 0;
        int reportCount = 0;
        for (DailyReport r : myReports) {
            if (r.getDate() != null && r.getDate().startsWith(currentMonth)) {
                totalHours += r.getHoursWorked();
                reportCount++;
            }
        }
        final double finalHours = totalHours;
        final int finalCount = reportCount;
        tvHoursMonth.setText(String.format(Locale.getDefault(), "%.0f", finalHours));
        tvReportsMonth.setText(String.valueOf(finalCount));
    }

    private void showAddReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_report, null);
        builder.setView(dialogView);

        TextInputEditText etDate = dialogView.findViewById(R.id.etReportDate);
        TextInputEditText etDesc = dialogView.findViewById(R.id.etReportText);
        TextInputEditText etHours = dialogView.findViewById(R.id.etReportHours);
        Spinner spinnerProject = dialogView.findViewById(R.id.spinnerProjectsReport);
        // Hide employee spinner - it's fixed for portal users
        View employeeSpinnerView = dialogView.findViewById(R.id.spinnerEmployees);
        if (employeeSpinnerView != null) employeeSpinnerView.setVisibility(View.GONE);

        // Pre-fill today's date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        etDate.setText(today);

        // Populate projects spinner
        List<String> projectNames = new ArrayList<>();
        projectNames.add("-- Selectează proiect --");
        for (Project p : projectList) projectNames.add(p.getName());
        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, projectNames);
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProject.setAdapter(projectAdapter);

        builder.setTitle("Adaug\u0103 Raport")
                .setPositiveButton("Trimite", (dialog, which) -> {
                    String date = etDate.getText() != null ? etDate.getText().toString().trim() : today;
                    String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                    double hours = 0;
                    try { hours = Double.parseDouble(etHours.getText().toString()); } catch (Exception ignored) {}

                    int projectPos = spinnerProject.getSelectedItemPosition();
                    int projectId = 0;
                    String projectName = "";
                    if (projectPos > 0 && projectPos <= projectList.size()) {
                        Project selected = projectList.get(projectPos - 1);
                        projectId = selected.getId();
                        projectName = selected.getName();
                    }

                    if (desc.isEmpty()) {
                        Toast.makeText(this, "Descrie activitatea", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    submitReport(date, desc, hours, projectId, projectName);
                })
                .setNegativeButton("Anuleaz\u0103", null)
                .show();
    }

    private void submitReport(String date, String desc, double hours, int projectId, String projectName) {
        DailyReport report = new DailyReport(
                employeeId,
                employeeName.isEmpty() ? "Angajat" : employeeName,
                projectId,
                projectName,
                date,
                desc,
                hours
        );

        // Save locally first
        new Thread(() -> db.dailyReportDao().insert(report)).start();

        // Sync to server
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().addEmployeeOwnReport(token, report).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EmployeePortalActivity.this, "Raport trimis cu succes!", Toast.LENGTH_SHORT).show();
                    loadMyReports(); // refresh
                } else {
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : "cod " + response.code();
                        Toast.makeText(EmployeePortalActivity.this, "Eroare server: " + err, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(EmployeePortalActivity.this, "Eroare server: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(EmployeePortalActivity.this, "Salvat local - server offline", Toast.LENGTH_SHORT).show();
                // Still refresh from local
                myReports.add(0, report);
                updateReportsList();
            }
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Deconectare")
                .setMessage("E\u0219ti sigur c\u0103 vrei s\u0103 te deconectezi?")
                .setPositiveButton("Da", (d, w) -> {
                    sharedPreferences.edit().clear().apply();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Nu", null)
                .show();
    }
}
