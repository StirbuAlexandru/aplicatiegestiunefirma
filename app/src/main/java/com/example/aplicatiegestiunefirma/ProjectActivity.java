package com.example.aplicatiegestiunefirma;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Project;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProjectActivity extends AppCompatActivity implements ProjectAdapter.OnProjectClickListener {

    private AppDatabase db;
    private RecyclerView rvProjects;
    private ProjectAdapter adapter;
    private FloatingActionButton fabAdd;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        db = AppDatabase.getInstance(this);
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        rvProjects = findViewById(R.id.rvProjects);
        fabAdd = findViewById(R.id.fabAddProject);

        adapter = new ProjectAdapter(this);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));
        rvProjects.setAdapter(adapter);

        // Încarcă datele locale imediat
        loadLocalProjects();
        // Apoi încearcă de pe server
        loadProjectsFromServer();

        fabAdd.setOnClickListener(v -> showAddProjectDialog());

        setupNavigation();
    }

    private void loadLocalProjects() {
        db.projectDao().getAllProjects().observe(this, projects -> {
            if (projects != null) {
                adapter.setProjects(projects);
            }
        });
    }

    private void loadProjectsFromServer() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().getProjects(token).enqueue(new Callback<List<Project>>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Project> projects = response.body();
                    adapter.setProjects(projects);

                    new Thread(() -> {
                        Set<Integer> serverIds = new HashSet<>();
                        for (Project p : projects) serverIds.add(p.getId());
                        for (Project local : db.projectDao().getAllProjectsDirect()) {
                            if (!serverIds.contains(local.getId())) db.projectDao().delete(local);
                        }
                        for (Project p : projects) {
                            db.projectDao().insert(p);
                        }
                    }).start();
                }
            }

            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {
                Toast.makeText(ProjectActivity.this, "Offline - date locale", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_projects);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_invoices) {
                startActivity(new Intent(this, InvoiceActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    @Override
    public void onProjectClick(Project project) {
        Intent intent = new Intent(this, ProjectDetailActivity.class);
        intent.putExtra("PROJECT_ID", project.getId());
        startActivity(intent);
    }

    @Override
    public void onProjectLongClick(Project project) {
        new AlertDialog.Builder(this)
                .setTitle(project.getName())
                .setItems(new String[]{"Editare", "Șterge"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditProjectDialog(project);
                    } else {
                        confirmDeleteProject(project);
                    }
                })
                .show();
    }

    private void confirmDeleteProject(Project project) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmare ștergere")
                .setMessage("Ești sigur că vrei să ștergi proiectul " + project.getName() + "?")
                .setPositiveButton("Șterge", (dialog, which) -> {
                    new Thread(() -> db.projectDao().delete(project)).start();
                    Toast.makeText(this, "Proiect șters", Toast.LENGTH_SHORT).show();
                    // Sync delete to server
                    String token = "Bearer " + sharedPreferences.getString("token", "");
                    RetrofitClient.getApiService().deleteProject(token, project.getId()).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) Toast.makeText(ProjectActivity.this, "Sincronizat cu serverul", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(ProjectActivity.this, "Ștergere locală - serverul nu răspunde", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Anulează", null)
                .show();
    }

    private void showEditProjectDialog(Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_project, null);
        builder.setView(dialogView);

        TextInputEditText etName = dialogView.findViewById(R.id.etProjectName);
        TextInputEditText etClient = dialogView.findViewById(R.id.etClientName);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);
        TextInputEditText etBudget = dialogView.findViewById(R.id.etBudget);
        TextInputEditText etDeadline = dialogView.findViewById(R.id.etDeadline);

        // Pre-fill fields
        etName.setText(project.getName());
        etClient.setText(project.getClientName());
        etDescription.setText(project.getDescription());
        etBudget.setText(String.valueOf(project.getBudget()));
        etDeadline.setText(project.getDeadline());

        // Calendar picker for deadline
        etDeadline.setOnClickListener(v -> showDatePicker(etDeadline));

        builder.setTitle("Editare Proiect")
                .setPositiveButton("Salvează", (dialog, which) -> {
                    project.setName(etName.getText().toString());
                    project.setClientName(etClient.getText().toString());
                    project.setDescription(etDescription.getText().toString());
                    project.setDeadline(etDeadline.getText().toString());
                    try { project.setBudget(Double.parseDouble(etBudget.getText().toString())); } catch (Exception e) {}

                    new Thread(() -> {
                        db.projectDao().update(project);
                        runOnUiThread(() -> Toast.makeText(this, "Proiect actualizat", Toast.LENGTH_SHORT).show());
                    }).start();

                    // Sync to server
                    String token = "Bearer " + sharedPreferences.getString("token", "");
                    RetrofitClient.getApiService().updateProject(token, project.getId(), project).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) Toast.makeText(ProjectActivity.this, "Sincronizat cu serverul", Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(ProjectActivity.this, "Actualizat local - sincronizare ulterioară", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Anulează", null)
                .show();
    }

    private void showAddProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_project, null);
        builder.setView(dialogView);

        TextInputEditText etName = dialogView.findViewById(R.id.etProjectName);
        TextInputEditText etClient = dialogView.findViewById(R.id.etClientName);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);
        TextInputEditText etBudget = dialogView.findViewById(R.id.etBudget);
        TextInputEditText etDeadline = dialogView.findViewById(R.id.etDeadline);

        // Calendar picker for deadline
        etDeadline.setOnClickListener(v -> showDatePicker(etDeadline));

        builder.setTitle("Adaugă Proiect")
                .setPositiveButton("Salvează", (dialog, which) -> {
                    String name = etName.getText().toString();
                    String client = etClient.getText().toString();
                    String description = etDescription.getText().toString();
                    String budgetStr = etBudget.getText().toString();
                    String deadline = etDeadline.getText().toString();

                    double budget = 0;
                    try {
                        budget = Double.parseDouble(budgetStr);
                    } catch (NumberFormatException e) {
                        budget = 0;
                    }

                    if (!name.isEmpty()) {
                        Project newProject = new Project(name, client, description, budget, deadline, "În desfășurare");
                        saveProject(newProject);
                    }
                })
                .setNegativeButton("Anulează", null)
                .show();
    }

    // ── Date Picker helper ────────────────────────────────────────────────────
    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        // Pre-fill with existing value if valid
        String existing = target.getText() != null ? target.getText().toString() : "";
        if (existing.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = existing.split("-");
            cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
        }
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            target.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveProject(Project project) {
        // 1. Salvare LOCALĂ (Funcționează mereu)
        new Thread(() -> db.projectDao().insert(project)).start();

        // 2. Salvare pe SERVER
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().addProject(token, project).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProjectActivity.this, "Proiect salvat pe server!", Toast.LENGTH_SHORT).show();
                    loadProjectsFromServer(); // refresh list
                } else {
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : "cod " + response.code();
                        Toast.makeText(ProjectActivity.this, "Eroare server: " + err, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(ProjectActivity.this, "Eroare server: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ProjectActivity.this, "Salvat local - server: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
