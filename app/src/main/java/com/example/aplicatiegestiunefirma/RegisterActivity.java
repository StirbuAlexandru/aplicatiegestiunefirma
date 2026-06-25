package com.example.aplicatiegestiunefirma;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aplicatiegestiunefirma.network.ApiService;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.example.aplicatiegestiunefirma.util.ValidationUtils;
import com.google.android.material.textfield.TextInputEditText;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword, etFirmaId;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        etFirmaId = findViewById(R.id.etRegFirmaId);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.regProgressBar);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnRegister.setOnClickListener(v -> performRegister());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void performRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String companyName = etFirmaId.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || companyName.isEmpty()) {
            Toast.makeText(this, "Toate câmpurile sunt obligatorii", Toast.LENGTH_SHORT).show();
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
        btnRegister.setEnabled(false);

        ApiService.RegisterRequest request = new ApiService.RegisterRequest(email, password, companyName);
        RetrofitClient.getApiService().register(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Cont creat cu succes!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Eroare la inregistrare (posibil email deja folosit)", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Eroare retea: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
