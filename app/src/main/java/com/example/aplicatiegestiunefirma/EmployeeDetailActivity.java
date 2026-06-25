package com.example.aplicatiegestiunefirma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeDetailActivity extends AppCompatActivity {

    private AppDatabase db;
    private SharedPreferences sharedPreferences;
    private int employeeId;

    private TextView tvName, tvPosition, tvSelectedMonth;
    private TextView tvTotalHoursValue, tvTotalDaysValue, tvTotalSalaryValue;
    private TextView tvPaymentTypeInfo, tvNoReports;
    private RecyclerView rvReports;
    private ImageView btnBack, btnPrevMonth, btnNextMonth;
    private MaterialButton btnViewContract;

    private Calendar selectedCal = Calendar.getInstance();
    private SimpleDateFormat displayMonthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("ro"));
    private SimpleDateFormat dbMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    private Employee currentEmployee;
    private EmpReportAdapter adapter;

    // For contract PDF selection in edit dialog
    private Uri selectedContractUri;
    private TextView tvContractStatusRef;

    private final ActivityResultLauncher<Intent> contractPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedContractUri = result.getData().getData();
                    getContentResolver().takePersistableUriPermission(
                            selectedContractUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (tvContractStatusRef != null) {
                        tvContractStatusRef.setText("PDF selectat: " + selectedContractUri.getLastPathSegment());
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_detail);

        db = AppDatabase.getInstance(this);
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        employeeId = getIntent().getIntExtra("employee_id", -1);

        tvName = findViewById(R.id.tvEmpDetailName);
        tvPosition = findViewById(R.id.tvEmpDetailPosition);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvTotalHoursValue = findViewById(R.id.tvTotalHoursValue);
        tvTotalDaysValue = findViewById(R.id.tvTotalDaysValue);
        tvTotalSalaryValue = findViewById(R.id.tvTotalSalaryValue);
        tvPaymentTypeInfo = findViewById(R.id.tvPaymentTypeInfo);
        tvNoReports = findViewById(R.id.tvNoReports);
        rvReports = findViewById(R.id.rvEmployeeReports);
        btnBack = findViewById(R.id.btnBack);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnViewContract = findViewById(R.id.btnViewContract);

        adapter = new EmpReportAdapter();
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        rvReports.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnChatEmployee).setOnClickListener(v -> {
            if (currentEmployee != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("employee_id", employeeId);
                intent.putExtra("employee_name", currentEmployee.getFullName());
                intent.putExtra("is_admin", true);
                startActivity(intent);
            }
        });

        // Action buttons
        findViewById(R.id.btnEditEmployee).setOnClickListener(v -> {
            if (currentEmployee != null) showEditEmployeeDialog(currentEmployee);
        });
        findViewById(R.id.btnCreateAccount).setOnClickListener(v -> {
            if (currentEmployee != null) showCreateAccountDialog(currentEmployee);
        });
        findViewById(R.id.btnDeleteEmployee).setOnClickListener(v -> {
            if (currentEmployee != null) confirmDeleteEmployee(currentEmployee);
        });
        btnViewContract.setOnClickListener(v -> {
            if (currentEmployee != null && currentEmployee.getContractPdfUri() != null)
                openContractPdf(currentEmployee.getContractPdfUri());
        });

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
        loadEmployeeInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh in case data changed
        if (currentEmployee != null) loadData();
    }

    private void updateMonthDisplay() {
        tvSelectedMonth.setText(displayMonthFormat.format(selectedCal.getTime()));
    }

    private void loadEmployeeInfo() {
        new Thread(() -> {
            currentEmployee = db.employeeDao().getEmployeeById(employeeId);
            if (currentEmployee == null) { runOnUiThread(this::finish); return; }
            runOnUiThread(() -> {
                tvName.setText(currentEmployee.getFullName());
                tvPosition.setText(currentEmployee.getPosition());
                updatePaymentTypeInfo();
                // Show/hide contract button
                if (currentEmployee.getContractPdfUri() != null) {
                    btnViewContract.setVisibility(View.VISIBLE);
                }
            });
            loadData();
        }).start();
    }

    private void updatePaymentTypeInfo() {
        if (currentEmployee == null) return;
        String info;
        switch (currentEmployee.getPaymentType()) {
            case "HOURLY": info = String.format(Locale.getDefault(), "Tip plata: Orar - %.2f RON/ora", currentEmployee.getPaymentRate()); break;
            case "DAILY":  info = String.format(Locale.getDefault(), "Tip plata: Zilnic - %.2f RON/zi", currentEmployee.getPaymentRate()); break;
            case "FIXED":  info = String.format(Locale.getDefault(), "Tip plata: Fix Lunar - %.2f RON/luna", currentEmployee.getPaymentRate()); break;
            default: info = "Tip plata: Necunoscut";
        }
        tvPaymentTypeInfo.setText(info);
    }

    private void loadData() {
        String monthKey = dbMonthFormat.format(selectedCal.getTime());
        new Thread(() -> {
            List<DailyReport> reports = db.dailyReportDao().getReportsByEmployeeAndMonthDirect(employeeId, monthKey);
            double totalHours = 0;
            Set<String> uniqueDays = new HashSet<>();
            for (DailyReport r : reports) { totalHours += r.getHoursWorked(); uniqueDays.add(r.getDate()); }
            int totalDays = uniqueDays.size();
            double salary = 0;
            if (currentEmployee != null) {
                switch (currentEmployee.getPaymentType()) {
                    case "HOURLY": salary = totalHours * currentEmployee.getPaymentRate(); break;
                    case "DAILY":  salary = totalDays * currentEmployee.getPaymentRate(); break;
                    case "FIXED":  if (totalDays > 0) salary = currentEmployee.getPaymentRate(); break;
                }
            }
            final double fH = totalHours, fS = salary; final int fD = totalDays;
            final List<DailyReport> fR = reports;
            runOnUiThread(() -> {
                tvTotalHoursValue.setText(String.format(Locale.getDefault(), "%.1f", fH));
                tvTotalDaysValue.setText(String.valueOf(fD));
                tvTotalSalaryValue.setText(String.format(Locale.getDefault(), "%.0f", fS));
                adapter.setReports(fR);
                tvNoReports.setVisibility(fR.isEmpty() ? View.VISIBLE : View.GONE);
                rvReports.setVisibility(fR.isEmpty() ? View.GONE : View.VISIBLE);
            });
        }).start();
    }

    // ==================== EDIT ====================
    private void showEditEmployeeDialog(Employee employee) {
        selectedContractUri = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_employee, null);
        builder.setView(dialogView);

        TextInputEditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        TextInputEditText etLastName  = dialogView.findViewById(R.id.etLastName);
        TextInputEditText etPosition  = dialogView.findViewById(R.id.etPosition);
        Spinner spinnerPaymentType    = dialogView.findViewById(R.id.spinnerPaymentType);
        TextInputLayout tilPaymentRate = dialogView.findViewById(R.id.tilPaymentRate);
        TextInputEditText etPaymentRate = dialogView.findViewById(R.id.etPaymentRate);
        TextInputLayout tilPaymentDay  = dialogView.findViewById(R.id.tilPaymentDay);
        TextInputEditText etPaymentDay = dialogView.findViewById(R.id.etPaymentDay);
        Button btnSelectContract       = dialogView.findViewById(R.id.btnSelectContractPdf);
        tvContractStatusRef            = dialogView.findViewById(R.id.tvContractPdfStatus);

        etFirstName.setText(employee.getFirstName());
        etLastName.setText(employee.getLastName());
        etPosition.setText(employee.getPosition());
        etPaymentRate.setText(String.valueOf(employee.getPaymentRate()));
        etPaymentDay.setText(String.valueOf(employee.getPaymentDay()));

        int typeIndex = "HOURLY".equals(employee.getPaymentType()) ? 0 : ("DAILY".equals(employee.getPaymentType()) ? 1 : 2);
        spinnerPaymentType.setSelection(typeIndex);

        if (employee.getContractPdfUri() != null) tvContractStatusRef.setText("Contract existent");

        spinnerPaymentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { tilPaymentRate.setHint("Tarif per Ora (RON)"); tilPaymentDay.setVisibility(View.GONE); }
                else if (position == 1) { tilPaymentRate.setHint("Tarif per Zi (RON)"); tilPaymentDay.setVisibility(View.GONE); }
                else { tilPaymentRate.setHint("Salariu Lunar Fix (RON)"); tilPaymentDay.setVisibility(View.VISIBLE); }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSelectContract.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            contractPickerLauncher.launch(intent);
        });

        builder.setTitle("Editare Angajat")
                .setPositiveButton("Salveaza", (dialog, which) -> {
                    employee.setFirstName(etFirstName.getText().toString());
                    employee.setLastName(etLastName.getText().toString());
                    employee.setPosition(etPosition.getText().toString());
                    int sel = spinnerPaymentType.getSelectedItemPosition();
                    employee.setPaymentType(sel == 0 ? "HOURLY" : (sel == 1 ? "DAILY" : "FIXED"));
                    try { employee.setPaymentRate(Double.parseDouble(etPaymentRate.getText().toString())); } catch (Exception ignored) {}
                    try { employee.setPaymentDay(Integer.parseInt(etPaymentDay.getText().toString())); } catch (Exception ignored) {}
                    if (selectedContractUri != null) employee.setContractPdfUri(selectedContractUri.toString());

                    new Thread(() -> {
                        db.employeeDao().update(employee);
                        currentEmployee = employee;
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Angajat actualizat", Toast.LENGTH_SHORT).show();
                            tvName.setText(employee.getFullName());
                            tvPosition.setText(employee.getPosition());
                            updatePaymentTypeInfo();
                            if (employee.getContractPdfUri() != null) btnViewContract.setVisibility(View.VISIBLE);
                        });
                    }).start();

                    String token = "Bearer " + sharedPreferences.getString("token", "");
                    RetrofitClient.getApiService().updateEmployee(token, employee.getId(), employee)
                            .enqueue(new Callback<ResponseBody>() {
                                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
                                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                            });
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    // ==================== DELETE ====================
    private void confirmDeleteEmployee(Employee employee) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmare stergere")
                .setMessage("Stergi angajatul " + employee.getFullName() + "?")
                .setPositiveButton("Sterge", (dialog, which) -> deleteEmployee(employee))
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    private void deleteEmployee(Employee employee) {
        new Thread(() -> {
            db.employeeDao().delete(employee);
            runOnUiThread(() -> {
                Toast.makeText(this, "Angajat sters", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        }).start();
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().deleteEmployee(token, employee.getId())
                .enqueue(new Callback<ResponseBody>() {
                    @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
    }

    // ==================== CREATE ACCOUNT ====================
    private void showCreateAccountDialog(Employee employee) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_employee_account, null);
        builder.setView(dialogView);

        TextView tvEmpName = dialogView.findViewById(R.id.tvEmployeeNameLabel);
        TextInputEditText etEmail = dialogView.findViewById(R.id.etEmpAccountEmail);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etEmpAccountPassword);
        TextInputEditText etPasswordConfirm = dialogView.findViewById(R.id.etEmpAccountPasswordConfirm);
        tvEmpName.setText(employee.getFullName());

        builder.setTitle("Creare Cont Angajat")
                .setPositiveButton("Creeaza", null)
                .setNegativeButton("Anuleaza", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String pwd   = etPassword.getText() != null ? etPassword.getText().toString() : "";
            String pwdC  = etPasswordConfirm.getText() != null ? etPasswordConfirm.getText().toString() : "";
            if (email.isEmpty() || pwd.isEmpty()) { Toast.makeText(this, "Completeaza email si parola", Toast.LENGTH_SHORT).show(); return; }
            if (!pwd.equals(pwdC)) { Toast.makeText(this, "Parolele nu coincid", Toast.LENGTH_SHORT).show(); return; }
            if (pwd.length() < 4)  { Toast.makeText(this, "Parola: minim 4 caractere", Toast.LENGTH_SHORT).show(); return; }

            String token = "Bearer " + sharedPreferences.getString("token", "");
            com.example.aplicatiegestiunefirma.network.ApiService.CreateEmployeeAccountRequest req =
                    new com.example.aplicatiegestiunefirma.network.ApiService.CreateEmployeeAccountRequest(email, pwd);
            RetrofitClient.getApiService().createEmployeeAccount(token, employee.getId(), req)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(EmployeeDetailActivity.this, "Cont creat! Email: " + email, Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(EmployeeDetailActivity.this,
                                        response.code() == 400 ? "Angajatul are deja cont" : "Eroare la creare cont",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(EmployeeDetailActivity.this, "Server offline", Toast.LENGTH_SHORT).show();
                        }
                    });
        }));
        dialog.show();
    }

    // ==================== CONTRACT PDF ====================
    private void openContractPdf(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Deschide Contract"));
        } catch (Exception e) {
            Toast.makeText(this, "Nu se poate deschide fisierul.", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== INNER ADAPTER ====================
    static class EmpReportAdapter extends RecyclerView.Adapter<EmpReportAdapter.VH> {
        private List<DailyReport> reports = new ArrayList<>();
        void setReports(List<DailyReport> r) { reports = r; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DailyReport r = reports.get(position);
            holder.tvDate.setText(r.getDate());
            holder.tvEmployee.setText(r.getProjectName());
            holder.tvProject.setText("Proiect: " + r.getProjectName());
            holder.tvDescription.setText(r.getReportText());
            holder.tvHours.setText(String.format(Locale.getDefault(), "%.1f ore", r.getHoursWorked()));
        }
        @Override public int getItemCount() { return reports.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvEmployee, tvProject, tvDescription, tvHours;
            VH(View v) { super(v);
                tvDate = v.findViewById(R.id.tvReportDate);
                tvEmployee = v.findViewById(R.id.tvReportEmployee);
                tvProject = v.findViewById(R.id.tvReportProject);
                tvDescription = v.findViewById(R.id.tvReportDescription);
                tvHours = v.findViewById(R.id.tvReportHours);
            }
        }
    }
}

