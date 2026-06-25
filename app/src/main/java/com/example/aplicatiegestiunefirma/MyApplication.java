package com.example.aplicatiegestiunefirma;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Apply saved dark mode preference before any activity starts
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int nightMode = prefs.getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
