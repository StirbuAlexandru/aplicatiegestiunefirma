package com.example.aplicatiegestiunefirma;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.ActivityLog;
import com.example.aplicatiegestiunefirma.model.AppDatabase;

import java.util.ArrayList;
import java.util.List;

public class ActivityLogActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView rvLogs;
    private LogAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_log);

        db = AppDatabase.getInstance(this);
        rvLogs = findViewById(R.id.rvActivityLogs);
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LogAdapter(new ArrayList<>());
        rvLogs.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClearLogs).setOnClickListener(v -> confirmClearLogs());

        loadLogs();
    }

    private void loadLogs() {
        new Thread(() -> {
            List<ActivityLog> logs = db.activityLogDao().getAllLogs();
            runOnUiThread(() -> adapter.setData(logs));
        }).start();
    }

    private void confirmClearLogs() {
        new AlertDialog.Builder(this)
                .setTitle("Sterge Jurnalul")
                .setMessage("Esti sigur ca vrei sa stergi toate inregistrarile?")
                .setPositiveButton("Da", (d, w) -> {
                    new Thread(() -> {
                        db.activityLogDao().deleteOldLogs("9999-99-99"); // Sterge tot
                        runOnUiThread(this::loadLogs);
                    }).start();
                })
                .setNegativeButton("Nu", null)
                .show();
    }

    static class LogAdapter extends RecyclerView.Adapter<LogAdapter.VH> {
        List<ActivityLog> list;
        LogAdapter(List<ActivityLog> list) { this.list = list; }
        void setData(List<ActivityLog> d) { list = d; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.two_line_list_item, parent, false);
            v.setPadding(32, 20, 32, 20);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ActivityLog log = list.get(position);
            holder.tv1.setText("[" + log.getActionLabel() + "] " + log.getEntityType() + ": " + log.getEntityDescription());
            holder.tv2.setText(log.getUserEmail() + "  -  " + log.getTimestamp());
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            VH(View v) { super(v); tv1 = v.findViewById(android.R.id.text1); tv2 = v.findViewById(android.R.id.text2); }
        }
    }
}
