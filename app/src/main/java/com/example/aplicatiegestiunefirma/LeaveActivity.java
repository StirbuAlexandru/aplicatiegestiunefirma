package com.example.aplicatiegestiunefirma;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
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
import com.example.aplicatiegestiunefirma.model.Leave;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LeaveActivity extends AppCompatActivity {

    private AppDatabase db;
    private SharedPreferences sharedPreferences;
    private RecyclerView rvLeaves;
    private LeaveAdapter adapter;
    private FloatingActionButton fabAdd;
    private TabLayout tabLayout;
    private List<Leave> allLeaves = new ArrayList<>();
    private List<Employee> employeeList = new ArrayList<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave);

        db = AppDatabase.getInstance(this);
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        rvLeaves = findViewById(R.id.rvLeaves);
        fabAdd = findViewById(R.id.fabAddLeave);
        tabLayout = findViewById(R.id.tabLayoutLeave);

        adapter = new LeaveAdapter();
        rvLeaves.setLayoutManager(new LinearLayoutManager(this));
        rvLeaves.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        new Thread(() -> {
            employeeList = db.employeeDao().getAllEmployeesDirect();
            allLeaves = db.leaveDao().getAllLeaves();
            runOnUiThread(() -> filterLeaves());
        }).start();
        syncLeavesFromServer();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filterLeaves(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabAdd.setOnClickListener(v -> showAddLeaveDialog());
    }

    private String token() {
        return "Bearer " + sharedPreferences.getString("token", "");
    }

    private void syncLeavesFromServer() {
        RetrofitClient.getApiService().getLeaves(token()).enqueue(new Callback<List<Leave>>() {
            @Override
            public void onResponse(Call<List<Leave>> call, Response<List<Leave>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Leave> serverLeaves = response.body();
                    new Thread(() -> {
                        Set<Integer> serverIds = new HashSet<>();
                        for (Leave l : serverLeaves) serverIds.add(l.getId());
                        for (Leave local : db.leaveDao().getAllLeaves()) {
                            if (!serverIds.contains(local.getId())) db.leaveDao().delete(local);
                        }
                        for (Leave l : serverLeaves) db.leaveDao().insert(l);
                        allLeaves = db.leaveDao().getAllLeaves();
                        runOnUiThread(LeaveActivity.this::filterLeaves);
                    }).start();
                }
            }
            @Override
            public void onFailure(Call<List<Leave>> call, Throwable t) {
                Toast.makeText(LeaveActivity.this, "Offline - date locale", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterLeaves() {
        int pos = tabLayout.getSelectedTabPosition();
        List<Leave> filtered;
        if (pos == 0) filtered = allLeaves;
        else if (pos == 1) filtered = allLeaves.stream().filter(l -> "PENDING".equals(l.getStatus())).collect(Collectors.toList());
        else if (pos == 2) filtered = allLeaves.stream().filter(l -> "APPROVED".equals(l.getStatus())).collect(Collectors.toList());
        else filtered = allLeaves.stream().filter(l -> "REJECTED".equals(l.getStatus())).collect(Collectors.toList());
        adapter.setData(filtered);
    }

    private void showAddLeaveDialog() {
        if (employeeList.isEmpty()) {
            Toast.makeText(this, "Nu exista angajati!", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_leave, null);
        Spinner spinnerEmp = dialogView.findViewById(R.id.spinnerLeaveEmployee);
        Spinner spinnerType = dialogView.findViewById(R.id.spinnerLeaveType);
        TextInputEditText etStart = dialogView.findViewById(R.id.etLeaveStart);
        TextInputEditText etEnd = dialogView.findViewById(R.id.etLeaveEnd);
        TextInputEditText etReason = dialogView.findViewById(R.id.etLeaveReason);
        TextInputEditText etDays = dialogView.findViewById(R.id.etLeaveDays);

        List<String> empNames = employeeList.stream().map(Employee::getFullName).collect(Collectors.toList());
        spinnerEmp.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, empNames));

        List<String> types = Arrays.asList("CO - Concediu Odihna", "CM - Concediu Medical", "CFP - Fara Plata", "EVENT - Eveniment");
        spinnerType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types));

        // Date pickers
        String today = sdf.format(Calendar.getInstance().getTime());
        etStart.setText(today); etEnd.setText(today);
        etStart.setOnClickListener(v -> showDatePicker(etStart));
        etEnd.setOnClickListener(v -> showDatePicker(etEnd));

        new AlertDialog.Builder(this)
                .setTitle("Adauga Concediu / Absenta")
                .setView(dialogView)
                .setPositiveButton("Salveaza", (d, w) -> {
                    Employee emp = employeeList.get(spinnerEmp.getSelectedItemPosition());
                    String[] typeCodes = {"CO", "CM", "CFP", "EVENT"};
                    String type = typeCodes[spinnerType.getSelectedItemPosition()];
                    String start = etStart.getText().toString();
                    String end = etEnd.getText().toString();
                    String reason = etReason.getText().toString();
                    int days = 1;
                    try { days = Integer.parseInt(etDays.getText().toString()); } catch (Exception ignored) {}
                    Leave leave = new Leave(emp.getId(), emp.getFullName(), start, end, type, reason, "PENDING", days);
                    new Thread(() -> {
                        db.leaveDao().insert(leave);
                        allLeaves = db.leaveDao().getAllLeaves();
                        runOnUiThread(() -> filterLeaves());
                    }).start();
                    RetrofitClient.getApiService().addLeave(token(), leave).enqueue(new Callback<ResponseBody>() {
                        @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) syncLeavesFromServer();
                        }
                        @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(LeaveActivity.this, "Salvat local - server offline", Toast.LENGTH_SHORT).show();
                        }
                    });
                    ActivityLogger.logCreate(this, "Concediu", emp.getFullName() + " " + start + "-" + end);
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    private void showDatePicker(TextInputEditText et) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                et.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // =================== ADAPTER ===================
    class LeaveAdapter extends RecyclerView.Adapter<LeaveAdapter.VH> {
        List<Leave> list = new ArrayList<>();
        void setData(List<Leave> d) { list = d; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.two_line_list_item, parent, false);
            v.setPadding(32, 24, 32, 24);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Leave l = list.get(position);
            holder.tv1.setText(l.getEmployeeName() + " - " + l.getTypeLabel());
            holder.tv2.setText(l.getStartDate() + " - " + l.getEndDate()
                    + "  |  " + l.getWorkingDays() + " zile  |  " + l.getStatusLabel());
            holder.itemView.setOnLongClickListener(v -> {
                showLeaveOptions(l); return true;
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            VH(View v) { super(v); tv1 = v.findViewById(android.R.id.text1); tv2 = v.findViewById(android.R.id.text2); }
        }
    }

    private void showLeaveOptions(Leave leave) {
        String[] options = {"Aproba", "Respinge", "Sterge"};
        new AlertDialog.Builder(this)
                .setTitle(leave.getEmployeeName())
                .setItems(options, (d, which) -> {
                    if (which == 0) { leave.setStatus("APPROVED"); updateLeave(leave); }
                    else if (which == 1) { leave.setStatus("REJECTED"); updateLeave(leave); }
                    else { deleteLeave(leave); }
                }).show();
    }

    private void updateLeave(Leave leave) {
        new Thread(() -> {
            db.leaveDao().update(leave);
            allLeaves = db.leaveDao().getAllLeaves();
            runOnUiThread(this::filterLeaves);
        }).start();
        RetrofitClient.getApiService().updateLeave(token(), leave.getId(), leave).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LeaveActivity.this, "Actualizat local - server offline", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteLeave(Leave leave) {
        new Thread(() -> {
            db.leaveDao().delete(leave);
            allLeaves = db.leaveDao().getAllLeaves();
            runOnUiThread(this::filterLeaves);
        }).start();
        RetrofitClient.getApiService().deleteLeave(token(), leave.getId()).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LeaveActivity.this, "Șters local - server offline", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
