package com.example.aplicatiegestiunefirma.util;

import android.util.Patterns;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 4;
    }

    public static boolean isPositiveAmount(String amountStr) {
        try {
            double val = Double.parseDouble(amountStr);
            return val >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isValidDate(String date) {
        // Basic YYYY-MM-DD format check
        return date != null && date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    /**
     * Sets error on TextInputLayout parent if available, otherwise on EditText directly.
     * Returns false to indicate validation failure.
     */
    public static boolean setError(EditText editText, String message) {
        if (editText.getParent() != null && editText.getParent().getParent() instanceof TextInputLayout) {
            TextInputLayout til = (TextInputLayout) editText.getParent().getParent();
            til.setError(message);
            til.setErrorEnabled(true);
        } else {
            editText.setError(message);
        }
        editText.requestFocus();
        return false;
    }

    public static void clearError(EditText editText) {
        if (editText.getParent() != null && editText.getParent().getParent() instanceof TextInputLayout) {
            TextInputLayout til = (TextInputLayout) editText.getParent().getParent();
            til.setError(null);
            til.setErrorEnabled(false);
        } else {
            editText.setError(null);
        }
    }
}
