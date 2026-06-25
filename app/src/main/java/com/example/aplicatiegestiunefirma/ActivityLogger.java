package com.example.aplicatiegestiunefirma;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.aplicatiegestiunefirma.model.ActivityLog;
import com.example.aplicatiegestiunefirma.model.AppDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Utilitar pentru jurnalizarea automata a actiunilor CRUD */
public class ActivityLogger {

    private static final SimpleDateFormat TS_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static void log(Context context, String action, String entityType, String entityDescription) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            String email = prefs.getString("user_email", "necunoscut");
            String timestamp = TS_FORMAT.format(new Date());
            ActivityLog log = new ActivityLog(email, action, entityType, entityDescription, timestamp);
            AppDatabase.getInstance(context).activityLogDao().insert(log);
        } catch (Exception ignored) {
            // Nu blocam niciodata fluxul principal din cauza logging-ului
        }
    }

    public static void logCreate(Context context, String entityType, String description) {
        log(context, "CREATE", entityType, description);
    }

    public static void logUpdate(Context context, String entityType, String description) {
        log(context, "UPDATE", entityType, description);
    }

    public static void logDelete(Context context, String entityType, String description) {
        log(context, "DELETE", entityType, description);
    }
}
