package com.example.aplicatiegestiunefirma;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.model.WorkSchedule;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkScheduleActivity extends AppCompatActivity {

    private AppDatabase db;
    private SharedPreferences sharedPreferences;
    private RecyclerView rvSchedule;
    private ScheduleAdapter adapter;
    private List<Employee> employeeList = new ArrayList<>();
    private int selectedEmployeeId = -1;
    private String selectedEmployeeName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_schedule);

        db = AppDatabase.getInstance(this);
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        rvSchedule = findViewById(R.id.rvSchedule);
        rvSchedule.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScheduleAdapter(new ArrayList<>());
        rvSchedule.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSelectEmployee).setOnClickListener(v -> showEmployeeSelector());
        findViewById(R.id.btnSaveSchedule).setOnClickListener(v -> showAddScheduleDialog());

        syncAllSchedulesFromServer();
    }

    private String token() {
        return "Bearer " + sharedPreferences.getString("token", "");
    }

    private void syncAllSchedulesFromServer() {
        RetrofitClient.getApiService().getWorkSchedules(token()).enqueue(new Callback<List<WorkSchedule>>() {
            @Override
            public void onResponse(Call<List<WorkSchedule>> call, Response<List<WorkSchedule>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<WorkSchedule> serverSchedules = response.body();
                    new Thread(() -> {
                        Set<Integer> serverIds = new HashSet<>();
                        for (WorkSchedule s : serverSchedules) serverIds.add(s.getId());
                        for (WorkSchedule local : db.workScheduleDao().getAllSchedulesDirect()) {
                            if (!serverIds.contains(local.getId())) db.workScheduleDao().delete(local);
                        }
                        for (WorkSchedule s : serverSchedules) db.workScheduleDao().insert(s);
                        if (selectedEmployeeId >= 0) loadScheduleForEmployee(selectedEmployeeId);
                    }).start();
                }
            }
            @Override
            public void onFailure(Call<List<WorkSchedule>> call, Throwable t) {
                Toast.makeText(WorkScheduleActivity.this, "Offline - date locale", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmployeeSelector() {
        new Thread(() -> {
            employeeList = db.employeeDao().getAllEmployeesDirect();
            runOnUiThread(() -> {
                String[] names = employeeList.stream().map(Employee::getFullName).toArray(String[]::new);
                new AlertDialog.Builder(this)
                        .setTitle("Selecteaza Angajat")
                        .setItems(names, (d, which) -> {
                            Employee emp = employeeList.get(which);
                            selectedEmployeeId = emp.getId();
                            selectedEmployeeName = emp.getFullName();
                            ((TextView) findViewById(R.id.tvSelectedEmployee)).setText(emp.getFullName());
                            loadScheduleForEmployee(emp.getId());
                        })
                        .show();
            });
        }).start();
    }

    private void loadScheduleForEmployee(int employeeId) {
        new Thread(() -> {
            List<WorkSchedule> schedules = db.workScheduleDao().getScheduleForEmployee(employeeId);
            runOnUiThread(() -> adapter.setData(schedules));
        }).start();
    }

    private void showAddScheduleDialog() {
        if (selectedEmployeeId < 0) {
            Toast.makeText(this, "Selecteaza un angajat mai intai!", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_schedule, null);
        Spinner spinnerDay = dialogView.findViewById(R.id.spinnerDayOfWeek);
        TextInputEditText etStart = dialogView.findViewById(R.id.etScheduleStart);
        TextInputEditText etEnd = dialogView.findViewById(R.id.etScheduleEnd);

        String[] days = {"Luni", "Marti", "Miercuri", "Joi", "Vineri", "Sambata", "Duminica"};
        spinnerDay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, days));
        etStart.setText("08:00");
        etEnd.setText("17:00");

        new AlertDialog.Builder(this)
                .setTitle("Adauga Tura")
                .setView(dialogView)
                .setPositiveButton("Salveaza", (d, w) -> {
                    int day = spinnerDay.getSelectedItemPosition() + 1;
                    String start = etStart.getText().toString();
                    String end = etEnd.getText().toString();
                    WorkSchedule ws = new WorkSchedule(selectedEmployeeId, day, start, end, true);
                    new Thread(() -> {
                        db.workScheduleDao().insert(ws);
                        loadScheduleForEmployee(selectedEmployeeId);
                    }).start();
                    RetrofitClient.getApiService().addWorkSchedule(token(), ws).enqueue(new Callback<ResponseBody>() {
                        @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) syncAllSchedulesFromServer();
                        }
                        @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(WorkScheduleActivity.this, "Salvat local - server offline", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    // ==================== ADAPTER ====================
    class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.VH> {
        List<WorkSchedule> list;
        ScheduleAdapter(List<WorkSchedule> list) { this.list = list; }
        void setData(List<WorkSchedule> d) { list = d; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.two_line_list_item, parent, false);
            v.setPadding(32, 20, 32, 20);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            WorkSchedule ws = list.get(position);
            holder.tv1.setText(ws.getDayName() + (ws.isWorkDay() ? "" : " (Zi libera)"));
            holder.tv2.setText(ws.isWorkDay() ? ws.getStartTime() + " - " + ws.getEndTime() : "-");
            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(WorkScheduleActivity.this)
                        .setTitle("Sterge tura " + ws.getDayName() + "?")
                        .setPositiveButton("Sterge", (d, w) -> {
                            new Thread(() -> {
                                db.workScheduleDao().delete(ws);
                                loadScheduleForEmployee(selectedEmployeeId);
                            }).start();
                            RetrofitClient.getApiService().deleteWorkSchedule(token(), ws.getId()).enqueue(new Callback<ResponseBody>() {
                                @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
                                @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    Toast.makeText(WorkScheduleActivity.this, "Șters local - server offline", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Nu", null).show();
                return true;
            });
        }

        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            VH(View v) { super(v); tv1 = v.findViewById(android.R.id.text1); tv2 = v.findViewById(android.R.id.text2); }
        }
    }
}
