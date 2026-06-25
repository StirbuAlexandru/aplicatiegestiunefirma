package com.example.aplicatiegestiunefirma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.network.ApiService;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.example.aplicatiegestiunefirma.util.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnBiometric;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private TextView tvGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        if (sharedPreferences.contains("token")) {
            boolean biometricEnabled = sharedPreferences.getBoolean("biometric_enabled", false);
            if (biometricEnabled && isBiometricAvailable()) {
                // Afisam ecranul de login cu promptul biometric
                setContentView(R.layout.activity_login);
                etEmail = findViewById(R.id.etEmail);
                etPassword = findViewById(R.id.etPassword);
                btnLogin = findViewById(R.id.btnLogin);
                btnBiometric = findViewById(R.id.btnBiometric);
                progressBar = findViewById(R.id.progressBar);
                tvGoToRegister = findViewById(R.id.tvGoToRegister);
                btnBiometric.setVisibility(View.VISIBLE);
                btnLogin.setOnClickListener(v -> performLogin());
                btnBiometric.setOnClickListener(v -> showBiometricPrompt(false));
                tvGoToRegister.setOnClickListener(v ->
                        startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
                // Afisam automat promptul biometric
                showBiometricPrompt(true);
            } else {
                String role = sharedPreferences.getString("user_role", "admin");
                if ("employee".equals(role)) {
                    startActivity(new Intent(this, EmployeePortalActivity.class));
                } else {
                    goToMainActivity();
                }
                finish();
            }
            return;
        }

        setContentView(R.layout.activity_login);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnBiometric = findViewById(R.id.btnBiometric);
        progressBar = findViewById(R.id.progressBar);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        // Afisam butonul biometric daca e disponibil
        if (isBiometricAvailable() && sharedPreferences.getBoolean("biometric_enabled", false)) {
            btnBiometric.setVisibility(View.VISIBLE);
            btnBiometric.setOnClickListener(v -> showBiometricPrompt(false));
        }

        btnLogin.setOnClickListener(v -> performLogin());
        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private boolean isBiometricAvailable() {
        BiometricManager bm = BiometricManager.from(this);
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometricPrompt(boolean autoLaunch) {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        String role = sharedPreferences.getString("user_role", "admin");
                        if ("employee".equals(role)) {
                            startActivity(new Intent(LoginActivity.this, EmployeePortalActivity.class));
                        } else {
                            goToMainActivity();
                        }
                        finish();
                    }
                    @Override
                    public void onAuthenticationError(int errorCode, @androidx.annotation.NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (!autoLaunch) {
                            Toast.makeText(LoginActivity.this, "Autentificare biometrica esuata: " + errString, Toast.LENGTH_SHORT).show();
                        }
                        // Fallback la login normal
                    }
                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(LoginActivity.this, "Amprenta nerecunoscuta", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autentificare Biometrica")
                .setSubtitle("Foloseste amprenta sau Face ID pentru a te conecta")
                .setNegativeButtonText("Foloseste parola")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void offerBiometricSetup() {
        if (!isBiometricAvailable()) return;
        if (sharedPreferences.getBoolean("biometric_enabled", false)) return; // deja activat
        if (sharedPreferences.getBoolean("biometric_declined", false)) return; // a refuzat deja

        new AlertDialog.Builder(this)
                .setTitle("Activare Amprenta")
                .setMessage("Doresti sa activezi autentificarea cu amprenta pentru data viitoare?")
                .setPositiveButton("Da, activeaza", (d, w) -> {
                    sharedPreferences.edit().putBoolean("biometric_enabled", true).apply();
                    Toast.makeText(this, "Amprenta activata!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Nu acum", (d, w) ->
                        sharedPreferences.edit().putBoolean("biometric_declined", true).apply())
                .show();
    }

    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Introdu email-ul si parola", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            ValidationUtils.setError(etEmail, "Format email invalid");
            return;
        }
        ValidationUtils.clearError(etEmail);

        if (!ValidationUtils.isValidPassword(password)) {
            ValidationUtils.setError(etPassword, "Parola trebuie sa aiba minim 4 caractere");
            return;
        }
        ValidationUtils.clearError(etPassword);

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        ApiService.LoginRequest request = new ApiService.LoginRequest(email, password);
        RetrofitClient.getApiService().login(request).enqueue(new Callback<ApiService.LoginResponse>() {
            @Override
            public void onResponse(Call<ApiService.LoginResponse> call, Response<ApiService.LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new Thread(() -> {
                        AppDatabase.getInstance(LoginActivity.this).clearAllTables();
                        runOnUiThread(() -> {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("token", response.body().access_token);
                            editor.putInt("firma_id", response.body().firma_id);
                            editor.putLong("token_created_at", System.currentTimeMillis());
                            String role = response.body().role != null ? response.body().role : "admin";
                            editor.putString("user_role", role);
                            int empId = response.body().employee_id != null ? response.body().employee_id : -1;
                            editor.putInt("employee_id", empId);
                            // Salvam email-ul pentru jurnal activitate si alte functii
                            editor.putString("user_email", email);
                            editor.apply();

                            progressBar.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);

                            // Oferim activarea amprentei dupa primul login reusit
                            offerBiometricSetup();

                            if ("employee".equals(role)) {
                                startActivity(new Intent(LoginActivity.this, EmployeePortalActivity.class));
                            } else {
                                goToMainActivity();
                            }
                            finish();
                        });
                    }).start();
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Email sau parola incorecta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Eroare conexiune: Server Offline", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}
