package com.example.aplicatiegestiunefirma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Ecran de blocare cu PIN - apare cand PIN-ul este activ */
public class PinLockActivity extends AppCompatActivity {

    private TextView tvPinDisplay;
    private StringBuilder pinInput = new StringBuilder();
    private SharedPreferences prefs;
    private int attempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_lock);

        prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        tvPinDisplay = findViewById(R.id.tvPinDisplay);

        int[] btnIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        String[] digits = {"0","1","2","3","4","5","6","7","8","9"};
        for (int i = 0; i < btnIds.length; i++) {
            final String digit = digits[i];
            Button btn = findViewById(btnIds[i]);
            btn.setOnClickListener(v -> addDigit(digit));
        }

        findViewById(R.id.btnPinDelete).setOnClickListener(v -> deleteDigit());

        // Buton logout de urgenta
        Button btnLogout = findViewById(R.id.btnEmergencyLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                prefs.edit().clear().apply();
                startActivity(new Intent(this, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            });
        }
    }

    @Override
    public void onBackPressed() {
        // Blocam butonul BACK pe ecranul PIN
    }

    private void addDigit(String digit) {
        if (pinInput.length() >= 4) return;
        pinInput.append(digit);
        updateDisplay();
        if (pinInput.length() == 4) {
            checkPin();
        }
    }

    private void checkPin() {
        String savedPin = prefs.getString("pin_code", "");
        if (pinInput.toString().equals(savedPin)) {
            // PIN corect - navigate to appropriate activity
            String role = prefs.getString("user_role", "admin");
            Intent intent;
            if ("employee".equals(role)) {
                intent = new Intent(this, EmployeePortalActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            attempts++;
            pinInput.setLength(0);
            updateDisplay();
            if (attempts >= 5) {
                Toast.makeText(this, "Prea multe incercari! Deconectare...", Toast.LENGTH_LONG).show();
                prefs.edit().clear().apply();
                startActivity(new Intent(this, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            } else {
                Toast.makeText(this, "PIN gresit! (" + attempts + "/5)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteDigit() {
        if (pinInput.length() > 0) {
            pinInput.deleteCharAt(pinInput.length() - 1);
            updateDisplay();
        }
    }

    private void updateDisplay() {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            dots.append(i < pinInput.length() ? "●" : "○");
            if (i < 3) dots.append("  ");
        }
        tvPinDisplay.setText(dots.toString());
    }
}
