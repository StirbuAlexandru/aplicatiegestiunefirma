package com.example.aplicatiegestiunefirma;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/** Ecran pentru setarea/schimbarea PIN-ului de securitate */
public class PinSetupActivity extends AppCompatActivity {

    private TextView tvPinDisplay;
    private StringBuilder pinInput = new StringBuilder();
    private boolean isConfirming = false;
    private String firstPin = "";
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        tvPinDisplay = findViewById(R.id.tvPinDisplay);
        TextView tvPinTitle = findViewById(R.id.tvPinTitle);
        tvPinTitle.setText("Seteaza PIN (4 cifre)");

        // Butoane numerice
        int[] btnIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        String[] digits = {"0","1","2","3","4","5","6","7","8","9"};
        for (int i = 0; i < btnIds.length; i++) {
            final String digit = digits[i];
            Button btn = findViewById(btnIds[i]);
            btn.setOnClickListener(v -> addDigit(digit));
        }

        findViewById(R.id.btnPinDelete).setOnClickListener(v -> deleteDigit());
        findViewById(R.id.btnPinCancel).setOnClickListener(v -> finish());
    }

    private void addDigit(String digit) {
        if (pinInput.length() >= 4) return;
        pinInput.append(digit);
        updateDisplay();
        if (pinInput.length() == 4) {
            if (!isConfirming) {
                firstPin = pinInput.toString();
                pinInput.setLength(0);
                isConfirming = true;
                ((TextView) findViewById(R.id.tvPinTitle)).setText("Confirma PIN-ul");
                updateDisplay();
            } else {
                if (pinInput.toString().equals(firstPin)) {
                    prefs.edit()
                            .putString("pin_code", firstPin)
                            .putBoolean("pin_enabled", true)
                            .apply();
                    Toast.makeText(this, "PIN setat cu succes!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "PIN-urile nu coincid. Incearca din nou.", Toast.LENGTH_SHORT).show();
                    pinInput.setLength(0);
                    firstPin = "";
                    isConfirming = false;
                    ((TextView) findViewById(R.id.tvPinTitle)).setText("Seteaza PIN (4 cifre)");
                    updateDisplay();
                }
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
