package com.example.aplicatiegestiunefirma;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.widget.EditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmployeeActivity extends AppCompatActivity implements EmployeeAdapter.OnEmployeeClickListener {

    private AppDatabase db;
    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private FloatingActionButton fabAdd;
    private TextView tvTotalCount;
    private SharedPreferences sharedPreferences;
    
    private List<Employee> allEmployees = new ArrayList<>();
    private Uri selectedContractUri;
    private TextView tvContractStatusRef;

    private final ActivityResultLauncher<Intent> contractPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedContractUri = result.getData().getData();
                    getContentResolver().takePersistableUriPermission(selectedContractUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (tvContractStatusRef != null) {
                        tvContractStatusRef.setText("PDF selectat: " + selectedContractUri.getLastPathSegment());
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employees);

        db = AppDatabase.getInstance(this);
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        
        rvEmployees = findViewById(R.id.rvEmployees);
        fabAdd = findViewById(R.id.fabAddEmployee);
        tvTotalCount = findViewById(R.id.tvTotalCount);

        adapter = new EmployeeAdapter(this);
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        rvEmployees.setAdapter(adapter);

        loadLocalEmployees();
        loadEmployeesFromServer();

        fabAdd.setOnClickListener(v -> showAddEmployeeDialog());
        setupSearch();
        setupNavigation();
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEmployees(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterEmployees(String query) {
        if (query.isEmpty()) {
            adapter.setEmployees(allEmployees);
            tvTotalCount.setText("Total angajați: " + allEmployees.size());
        } else {
            String lower = query.toLowerCase();
            List<Employee> filtered = allEmployees.stream()
                    .filter(e -> e.getFullName().toLowerCase().contains(lower)
                            || (e.getPosition() != null && e.getPosition().toLowerCase().contains(lower))
                            || (e.getPhone() != null && e.getPhone().contains(lower)))
                    .collect(Collectors.toList());
            adapter.setEmployees(filtered);
            tvTotalCount.setText("Găsiți: " + filtered.size() + " din " + allEmployees.size());
        }
    }

    private void loadLocalEmployees() {
        db.employeeDao().getAllEmployees().observe(this, employees -> {
            if (employees != null) {
                allEmployees = employees;
                EditText etSearch = findViewById(R.id.etSearch);
                filterEmployees(etSearch.getText().toString());
            }
        });
    }

    private void loadEmployeesFromServer() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().getEmployees(token).enqueue(new Callback<List<Employee>>() {
            @Override
            public void onResponse(Call<List<Employee>> call, Response<List<Employee>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Employee> employees = response.body();
                    new Thread(() -> {
                        try {
                            Set<Integer> serverIds = new HashSet<>();
                            for (Employee e : employees) serverIds.add(e.getId());
                            for (Employee local : db.employeeDao().getAllEmployeesDirect()) {
                                if (!serverIds.contains(local.getId())) db.employeeDao().delete(local);
                            }
                            for (Employee e : employees) {
                                db.employeeDao().insert(e);
                            }
                        } catch (Exception ex) {}
                    }).start();
                }
            }
            @Override
            public void onFailure(Call<List<Employee>> call, Throwable t) {
                Toast.makeText(EmployeeActivity.this, "Offline - date locale", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, MainActivity.class)); finish(); return true; }
            else if (id == R.id.nav_projects) { startActivity(new Intent(this, ProjectActivity.class)); finish(); return true; }
            else if (id == R.id.nav_invoices) { startActivity(new Intent(this, InvoiceActivity.class)); finish(); return true; }
            else if (id == R.id.nav_reports) { startActivity(new Intent(this, ReportsActivity.class)); finish(); return true; }
            return false;
        });
    }

    @Override
    public void onEmployeeClick(Employee employee) {
        Intent intent = new Intent(this, EmployeeDetailActivity.class);
        intent.putExtra("employee_id", employee.getId());
        startActivity(intent);
    }

    @Override
    public void onEmployeeLongClick(Employee employee) {
        showEmployeeOptionsDialog(employee);
    }

    private void showEmployeeOptionsDialog(Employee employee) {
        List<String> optionsList = new ArrayList<>();
        if (employee.getContractPdfUri() != null) {
            optionsList.add("Vezi Contract PDF");
        }
        optionsList.add("Creare Cont Angajat");
        optionsList.add("Editare");
        optionsList.add("\u0218terge");

        String[] options = optionsList.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle(employee.getFullName())
                .setItems(options, (dialog, which) -> {
                    if (options[which].equals("Vezi Contract PDF")) {
                        openContractPdf(employee.getContractPdfUri());
                    } else if (options[which].equals("Creare Cont Angajat")) {
                        showCreateAccountDialog(employee);
                    } else if (options[which].equals("Editare")) {
                        showEditEmployeeDialog(employee);
                    } else if (options[which].equals("\u0218terge")) {
                        confirmDeleteEmployee(employee);
                    }
                })
                .show();
    }

    private void showCreateAccountDialog(Employee employee) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_employee_account, null);
        builder.setView(dialogView);

        TextView tvName = dialogView.findViewById(R.id.tvEmployeeNameLabel);
        com.google.android.material.textfield.TextInputEditText etEmail = dialogView.findViewById(R.id.etEmpAccountEmail);
        com.google.android.material.textfield.TextInputEditText etPassword = dialogView.findViewById(R.id.etEmpAccountPassword);
        com.google.android.material.textfield.TextInputEditText etPasswordConfirm = dialogView.findViewById(R.id.etEmpAccountPasswordConfirm);

        tvName.setText(employee.getFullName());

        builder.setTitle("Creare Cont Angajat")
                .setPositiveButton("Creeaz\u0103", null) // null so we handle manually
                .setNegativeButton("Anuleaz\u0103", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                String pwd = etPassword.getText() != null ? etPassword.getText().toString() : "";
                String pwdConfirm = etPasswordConfirm.getText() != null ? etPasswordConfirm.getText().toString() : "";

                if (email.isEmpty() || pwd.isEmpty()) {
                    Toast.makeText(this, "Completea\u021b\u0103 email-ul \u0219i parola", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!pwd.equals(pwdConfirm)) {
                    Toast.makeText(this, "Parolele nu coincid", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pwd.length() < 4) {
                    Toast.makeText(this, "Parola trebuie s\u0103 aib\u0103 minim 4 caractere", Toast.LENGTH_SHORT).show();
                    return;
                }

                String token = "Bearer " + sharedPreferences.getString("token", "");
                com.example.aplicatiegestiunefirma.network.ApiService.CreateEmployeeAccountRequest request =
                        new com.example.aplicatiegestiunefirma.network.ApiService.CreateEmployeeAccountRequest(email, pwd);

                RetrofitClient.getApiService().createEmployeeAccount(token, employee.getId(), request)
                        .enqueue(new Callback<okhttp3.ResponseBody>() {
                            @Override
                            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(EmployeeActivity.this,
                                            "Cont creat! Angajatul se poate autentifica cu: " + email,
                                            Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                } else {
                                    String msg = "Eroare la creare cont";
                                    if (response.code() == 400) msg = "Angajatul are deja un cont";
                                    Toast.makeText(EmployeeActivity.this, msg, Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                                Toast.makeText(EmployeeActivity.this, "Server offline", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        });
        dialog.show();
    }

    private void confirmDeleteEmployee(Employee employee) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmare ștergere")
                .setMessage("Ești sigur că vrei să ștergi angajatul " + employee.getFullName() + "?")
                .setPositiveButton("Șterge", (dialog, which) -> deleteEmployee(employee))
                .setNegativeButton("Anulează", null)
                .show();
    }

    private void deleteEmployee(Employee employee) {
        new Thread(() -> db.employeeDao().delete(employee)).start();
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().deleteEmployee(token, employee.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EmployeeActivity.this, "Sincronizat cu serverul", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(EmployeeActivity.this, "Ștergere locală - serverul nu răspunde", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openContractPdf(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Deschide Contract"));
        } catch (Exception e) {
            Toast.makeText(this, "Nu se poate deschide fișierul.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEditEmployeeDialog(Employee employee) {
        selectedContractUri = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_employee, null);
        builder.setView(dialogView);

        TextInputEditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        TextInputEditText etLastName = dialogView.findViewById(R.id.etLastName);
        TextInputEditText etPosition = dialogView.findViewById(R.id.etPosition);
        Spinner spinnerPaymentType = dialogView.findViewById(R.id.spinnerPaymentType);
        TextInputLayout tilPaymentRate = dialogView.findViewById(R.id.tilPaymentRate);
        TextInputEditText etPaymentRate = dialogView.findViewById(R.id.etPaymentRate);
        TextInputLayout tilPaymentDay = dialogView.findViewById(R.id.tilPaymentDay);
        TextInputEditText etPaymentDay = dialogView.findViewById(R.id.etPaymentDay);
        Button btnSelectContract = dialogView.findViewById(R.id.btnSelectContractPdf);
        tvContractStatusRef = dialogView.findViewById(R.id.tvContractPdfStatus);

        // Pre-fill fields
        etFirstName.setText(employee.getFirstName());
        etLastName.setText(employee.getLastName());
        etPosition.setText(employee.getPosition());
        etPaymentRate.setText(String.valueOf(employee.getPaymentRate()));
        etPaymentDay.setText(String.valueOf(employee.getPaymentDay()));

        int typeIndex = "HOURLY".equals(employee.getPaymentType()) ? 0 : ("DAILY".equals(employee.getPaymentType()) ? 1 : 2);
        spinnerPaymentType.setSelection(typeIndex);

        if (employee.getContractPdfUri() != null) {
            tvContractStatusRef.setText("Contract existent");
        }

        spinnerPaymentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    tilPaymentRate.setHint("Tarif per Oră (RON)");
                    tilPaymentDay.setVisibility(View.GONE);
                } else if (position == 1) {
                    tilPaymentRate.setHint("Tarif per Zi (RON)");
                    tilPaymentDay.setVisibility(View.GONE);
                } else {
                    tilPaymentRate.setHint("Salariu Lunar Fix (RON)");
                    tilPaymentDay.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSelectContract.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            contractPickerLauncher.launch(intent);
        });

        builder.setTitle("Editare Angajat")
                .setPositiveButton("Salvează", (dialog, which) -> {
                    employee.setFirstName(etFirstName.getText().toString());
                    employee.setLastName(etLastName.getText().toString());
                    employee.setPosition(etPosition.getText().toString());

                    int selType = spinnerPaymentType.getSelectedItemPosition();
                    employee.setPaymentType(selType == 0 ? "HOURLY" : (selType == 1 ? "DAILY" : "FIXED"));

                    try { employee.setPaymentRate(Double.parseDouble(etPaymentRate.getText().toString())); } catch (Exception e) {}
                    try { employee.setPaymentDay(Integer.parseInt(etPaymentDay.getText().toString())); } catch (Exception e) {}

                    if (selectedContractUri != null) {
                        String token = "Bearer " + sharedPreferences.getString("token", "");
                        com.example.aplicatiegestiunefirma.util.FileUploadHelper.uploadFile(
                                this, selectedContractUri, token,
                                new com.example.aplicatiegestiunefirma.util.FileUploadHelper.UploadCallback() {
                                    @Override public void onSuccess(String fileUrl) {
                                        employee.setContractPdfUri(fileUrl);
                                        finishEmployeeUpdate(employee);
                                    }
                                    @Override public void onFailure(String error) {
                                        employee.setContractPdfUri(selectedContractUri.toString());
                                        Toast.makeText(EmployeeActivity.this, "Contract neîncărcat pe server - server offline", Toast.LENGTH_SHORT).show();
                                        finishEmployeeUpdate(employee);
                                    }
                                });
                    } else {
                        finishEmployeeUpdate(employee);
                    }
                })
                .setNegativeButton("Anulează", null)
                .show();
    }

    private void finishEmployeeUpdate(Employee employee) {
        new Thread(() -> {
            db.employeeDao().update(employee);
            runOnUiThread(() -> Toast.makeText(this, "Angajat actualizat", Toast.LENGTH_SHORT).show());
        }).start();

        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().updateEmployee(token, employee.getId(), employee).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) Toast.makeText(EmployeeActivity.this, "Sincronizat cu serverul", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(EmployeeActivity.this, "Actualizat local - sincronizare ulterioară", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddEmployeeDialog() {
        selectedContractUri = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_employee, null);
        builder.setView(dialogView);

        TextInputEditText etFirstName = dialogView.findViewById(R.id.etFirstName);
        TextInputEditText etLastName = dialogView.findViewById(R.id.etLastName);
        TextInputEditText etPosition = dialogView.findViewById(R.id.etPosition);
        Spinner spinnerPaymentType = dialogView.findViewById(R.id.spinnerPaymentType);
        TextInputLayout tilPaymentRate = dialogView.findViewById(R.id.tilPaymentRate);
        TextInputEditText etPaymentRate = dialogView.findViewById(R.id.etPaymentRate);
        TextInputLayout tilPaymentDay = dialogView.findViewById(R.id.tilPaymentDay);
        TextInputEditText etPaymentDay = dialogView.findViewById(R.id.etPaymentDay);
        Button btnSelectContract = dialogView.findViewById(R.id.btnSelectContractPdf);
        tvContractStatusRef = dialogView.findViewById(R.id.tvContractPdfStatus);

        spinnerPaymentType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // Hourly
                    tilPaymentRate.setHint("Tarif per Oră (RON)");
                    tilPaymentDay.setVisibility(View.GONE);
                } else if (position == 1) { // Daily
                    tilPaymentRate.setHint("Tarif per Zi (RON)");
                    tilPaymentDay.setVisibility(View.GONE);
                } else { // Fixed/Monthly
                    tilPaymentRate.setHint("Salariu Lunar Fix (RON)");
                    tilPaymentDay.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSelectContract.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            contractPickerLauncher.launch(intent);
        });

        builder.setTitle("Adaugă Angajat")
                .setPositiveButton("Salvează", (dialog, which) -> {
                    String fName = etFirstName.getText().toString();
                    String lName = etLastName.getText().toString();
                    String pos = etPosition.getText().toString();
                    
                    int typeIndex = spinnerPaymentType.getSelectedItemPosition();
                    String paymentType = typeIndex == 0 ? "HOURLY" : (typeIndex == 1 ? "DAILY" : "FIXED");
                    
                    double rate = 0;
                    try { rate = Double.parseDouble(etPaymentRate.getText().toString()); } catch (Exception e) {}
                    
                    int day = 1;
                    try { day = Integer.parseInt(etPaymentDay.getText().toString()); } catch (Exception e) {}
                    
                    if (!fName.isEmpty()) {
                        Employee emp = new Employee(fName, lName, "", "", "", "", "", pos, paymentType, rate, day, null);
                        if (selectedContractUri != null) {
                            String token = "Bearer " + sharedPreferences.getString("token", "");
                            com.example.aplicatiegestiunefirma.util.FileUploadHelper.uploadFile(
                                    this, selectedContractUri, token,
                                    new com.example.aplicatiegestiunefirma.util.FileUploadHelper.UploadCallback() {
                                        @Override public void onSuccess(String fileUrl) {
                                            emp.setContractPdfUri(fileUrl);
                                            saveEmployee(emp);
                                        }
                                        @Override public void onFailure(String error) {
                                            emp.setContractPdfUri(selectedContractUri.toString());
                                            Toast.makeText(EmployeeActivity.this, "Contract neîncărcat pe server - server offline", Toast.LENGTH_SHORT).show();
                                            saveEmployee(emp);
                                        }
                                    });
                        } else {
                            saveEmployee(emp);
                        }
                    }
                })
                .setNegativeButton("Anulează", null)
                .show();
    }

    private void saveEmployee(Employee emp) {
        new Thread(() -> {
            db.employeeDao().insert(emp);
            runOnUiThread(() -> Toast.makeText(this, "Angajat salvat local", Toast.LENGTH_SHORT).show());
        }).start();

        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().addEmployee(token, emp).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    loadEmployeesFromServer();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(EmployeeActivity.this, "Salvat local - sincronizare ulterioară", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
