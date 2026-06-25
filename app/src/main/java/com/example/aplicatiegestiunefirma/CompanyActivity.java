package com.example.aplicatiegestiunefirma;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Company;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.example.aplicatiegestiunefirma.util.ValidationUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CompanyActivity extends AppCompatActivity {

    private EditText etName, etCui, etRegCom, etAddress, etIban, etBank, etPhone, etEmail;
    private Button btnSave;
    private AppDatabase db;
    private Company currentCompany;
    private SharedPreferences sharedPreferences;

    private final ActivityResultLauncher<Intent> restorePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) restoreDatabase(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company);

        db = AppDatabase.getInstance(this);
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        etName = findViewById(R.id.etCompName);
        etCui = findViewById(R.id.etCompCui);
        etRegCom = findViewById(R.id.etCompRegCom);
        etAddress = findViewById(R.id.etCompAddress);
        etIban = findViewById(R.id.etCompIban);
        etBank = findViewById(R.id.etCompBank);
        etPhone = findViewById(R.id.etCompPhone);
        etEmail = findViewById(R.id.etCompEmail);
        btnSave = findViewById(R.id.btnSaveCompany);

        loadLocalData();
        loadCompanyFromServer();

        btnSave.setOnClickListener(v -> saveCompanyData());
        findViewById(R.id.btnBackupDb).setOnClickListener(v -> backupDatabase());
        findViewById(R.id.btnRestoreDb).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Restaurare Baza de Date")
                    .setMessage("Atenție! Toate datele curente vor fi înlocuite cu cele din backup. Continuă?")
                    .setPositiveButton("Da, restaurează", (d, w) -> {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("*/*");
                        restorePickerLauncher.launch(intent);
                    })
                    .setNegativeButton("Anulează", null)
                    .show();
        });
    }

    private void loadLocalData() {
        new Thread(() -> {
            List<Company> companies = db.companyDao().getAllCompaniesDirect();
            if (companies != null && !companies.isEmpty()) {
                currentCompany = companies.get(0);
                runOnUiThread(() -> populateFields(currentCompany));
            }
        }).start();
    }

    private void populateFields(Company company) {
        etName.setText(company.getName());
        etCui.setText(company.getTaxId());
        etRegCom.setText(company.getRegCom());
        etAddress.setText(company.getAddress());
        etIban.setText(company.getIban());
        etBank.setText(company.getBank());
        etPhone.setText(company.getPhone());
        etEmail.setText(company.getEmail());
    }

    private void loadCompanyFromServer() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().getCompany(token).enqueue(new Callback<Company>() {
            @Override
            public void onResponse(Call<Company> call, Response<Company> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Company server = response.body();
                    if (server.getName() == null || server.getName().isEmpty()) return;
                    new Thread(() -> {
                        List<Company> local = db.companyDao().getAllCompaniesDirect();
                        if (!local.isEmpty()) {
                            server.setId(local.get(0).getId());
                            db.companyDao().update(server);
                        } else {
                            db.companyDao().insert(server);
                        }
                        currentCompany = server;
                        runOnUiThread(() -> populateFields(server));
                    }).start();
                }
            }
            @Override
            public void onFailure(Call<Company> call, Throwable t) {
                Toast.makeText(CompanyActivity.this, "Offline - date locale", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveCompanyData() {
        String name = etName.getText().toString();
        String cui = etCui.getText().toString();
        String regCom = etRegCom.getText().toString();
        String address = etAddress.getText().toString();
        String iban = etIban.getText().toString();
        String bank = etBank.getText().toString();
        String phone = etPhone.getText().toString();
        String email = etEmail.getText().toString();

        if (name.isEmpty() || cui.isEmpty()) {
            Toast.makeText(this, "Numele și CUI sunt obligatorii", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.isEmpty() && !ValidationUtils.isValidEmail(email)) {
            Toast.makeText(this, "Format email invalid", Toast.LENGTH_SHORT).show();
            return;
        }

        Company companyToSave = new Company(name, address, cui, regCom, iban, bank, phone, email);
        
        // 1. Salvare in Cloud (FastAPI)
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().saveCompany(token, companyToSave).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    saveLocally(companyToSave);
                } else {
                    Toast.makeText(CompanyActivity.this, "Eroare la salvare pe server", Toast.LENGTH_SHORT).show();
                    // Optional: Salvam totusi local daca vrem sa permitem lucrul offline
                    saveLocally(companyToSave);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(CompanyActivity.this, "Eroare rețea: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                saveLocally(companyToSave);
            }
        });
    }

    private void saveLocally(Company company) {
        new Thread(() -> {
            if (currentCompany == null) {
                db.companyDao().insert(company);
            } else {
                company.setId(currentCompany.getId());
                db.companyDao().update(company);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Date salvate cu succes!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }).start();
    }

    private void backupDatabase() {
        try {
            File dbFile = getDatabasePath("company_database");
            File backupDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File backupFile = new File(backupDir, "backup_company_database.db");

            // Force checkpoint before copy
            db.getOpenHelper().getWritableDatabase();

            try (InputStream in = new FileInputStream(dbFile);
                 OutputStream out = new FileOutputStream(backupFile)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            Toast.makeText(this, "Backup salvat: " + backupFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Eroare la backup: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreDatabase(Uri uri) {
        try {
            File dbFile = getDatabasePath("company_database");

            // Close database
            db.close();
            AppDatabase.resetInstance();

            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(dbFile)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }

            Toast.makeText(this, "Baza de date restaurată! Repornire...", Toast.LENGTH_SHORT).show();

            // Restart app
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Eroare la restaurare: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Re-initialize database
            db = AppDatabase.getInstance(this);
        }
    }
}