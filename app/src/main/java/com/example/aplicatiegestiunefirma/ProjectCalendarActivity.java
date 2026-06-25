package com.example.aplicatiegestiunefirma;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Project;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ProjectCalendarActivity extends AppCompatActivity {

    private AppDatabase db;
    private List<Project> allProjects = new ArrayList<>();
    private RecyclerView rvProjectsForDate, rvUpcomingDeadlines;
    private TextView tvSelectedDate, tvNoProjects;
    private CalendarView calendarView;
    private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat sdfDisplay = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_calendar);

        db = AppDatabase.getInstance(this);
        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoProjects = findViewById(R.id.tvNoProjects);
        rvProjectsForDate = findViewById(R.id.rvProjectsForDate);
        rvUpcomingDeadlines = findViewById(R.id.rvUpcomingDeadlines);

        rvProjectsForDate.setLayoutManager(new LinearLayoutManager(this));
        rvUpcomingDeadlines.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        db.projectDao().getAllProjects().observe(this, projects -> {
            allProjects = projects;
            loadUpcomingDeadlines();
            // Afisam proiectele pentru ziua de azi
            String today = sdfDate.format(Calendar.getInstance().getTime());
            showProjectsForDate(today);
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            tvSelectedDate.setText("Proiecte pentru: " + String.format("%02d.%02d.%04d", dayOfMonth, month + 1, year));
            showProjectsForDate(dateStr);
        });
    }

    private void showProjectsForDate(String dateStr) {
        List<Project> projectsForDate = allProjects.stream()
                .filter(p -> dateStr.equals(p.getDeadline()))
                .collect(Collectors.toList());

        rvProjectsForDate.setAdapter(new ProjectMiniAdapter(projectsForDate));

        if (projectsForDate.isEmpty()) {
            tvNoProjects.setVisibility(View.VISIBLE);
            rvProjectsForDate.setVisibility(View.GONE);
        } else {
            tvNoProjects.setVisibility(View.GONE);
            rvProjectsForDate.setVisibility(View.VISIBLE);
        }
    }

    private void loadUpcomingDeadlines() {
        Calendar today = Calendar.getInstance();
        Calendar plus30 = Calendar.getInstance();
        plus30.add(Calendar.DAY_OF_MONTH, 30);
        String todayStr = sdfDate.format(today.getTime());
        String plus30Str = sdfDate.format(plus30.getTime());

        List<Project> upcoming = allProjects.stream()
                .filter(p -> p.getDeadline() != null
                        && p.getDeadline().compareTo(todayStr) >= 0
                        && p.getDeadline().compareTo(plus30Str) <= 0)
                .sorted((a, b) -> a.getDeadline().compareTo(b.getDeadline()))
                .collect(Collectors.toList());

        rvUpcomingDeadlines.setAdapter(new ProjectMiniAdapter(upcoming));
    }

    // ===================== MINI ADAPTER =====================
    static class ProjectMiniAdapter extends RecyclerView.Adapter<ProjectMiniAdapter.VH> {
        private final List<Project> list;
        ProjectMiniAdapter(List<Project> list) { this.list = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.two_line_list_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Project p = list.get(position);
            holder.tv1.setText(p.getName());
            holder.tv2.setText("Deadline: " + (p.getDeadline() != null ? p.getDeadline() : "N/A")
                    + "  |  " + (p.getStatus() != null ? p.getStatus() : "In progres"));
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            VH(@NonNull View itemView) {
                super(itemView);
                tv1 = itemView.findViewById(android.R.id.text1);
                tv2 = itemView.findViewById(android.R.id.text2);
                tv1.setTextSize(14f);
                tv2.setTextSize(12f);
            }
        }
    }
}
