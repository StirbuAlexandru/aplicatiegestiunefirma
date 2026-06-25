package com.example.aplicatiegestiunefirma;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Company;
import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.model.Employee;
import com.example.aplicatiegestiunefirma.model.Project;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView rvReports;
    private ReportAdapter adapter;
    private FloatingActionButton fabAdd;
    private CalendarView calendarView;
    private TextView tvSelectedDate, tvNoReports;
    private ImageView btnExportMonth, btnExportYear, btnExportMonthCsv, btnExportYearCsv;
    private List<Employee> employeeList = new ArrayList<>();
    private List<Project> projectList = new ArrayList<>();
    private String selectedDate;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private LiveData<List<DailyReport>> currentReportsLiveData;
    private SharedPreferences sharedPreferences;
    private Company currentCompany;
    private FusedLocationProviderClient fusedLocationClient;

    // Foto/GPS temporare pentru dialogul activ
    private String pendingPhotoUri = null;
    private double pendingLatitude = 0.0;
    private double pendingLongitude = 0.0;
    private ImageView dialogPhotoPreview;
    private TextView dialogPhotoStatus, dialogLocationStatus;
    private Uri cameraFileUri;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    pendingPhotoUri = selectedUri.toString();
                    updatePhotoPreview(selectedUri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && cameraFileUri != null) {
                    pendingPhotoUri = cameraFileUri.toString();
                    updatePhotoPreview(cameraFileUri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> locationPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                    getCurrentLocation();
                } else {
                    Toast.makeText(this, "Permisiunea GPS refuzata", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        db = AppDatabase.getInstance(this);
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        
        rvReports = findViewById(R.id.rvReports);
        fabAdd = findViewById(R.id.fabAddReport);
        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvNoReports = findViewById(R.id.tvNoReports);
        btnExportMonth = findViewById(R.id.btnExportMonth);
        btnExportYear = findViewById(R.id.btnExportYear);
        btnExportMonthCsv = findViewById(R.id.btnExportMonthCsv);
        btnExportYearCsv = findViewById(R.id.btnExportYearCsv);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        adapter = new ReportAdapter();
        rvReports.setLayoutManager(new LinearLayoutManager(this));
        rvReports.setAdapter(adapter);

        Calendar calendar = Calendar.getInstance();
        selectedDate = dateFormat.format(calendar.getTime());
        updateDateHeader(calendar);
        syncEmployeesAndProjectsFromServer();
        syncAndLoadReportsForDate(selectedDate);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar selCal = Calendar.getInstance();
            selCal.set(year, month, dayOfMonth);
            selectedDate = dateFormat.format(selCal.getTime());
            updateDateHeader(selCal);
            syncAndLoadReportsForDate(selectedDate);
        });

        db.employeeDao().getAllEmployees().observe(this, employees -> employeeList = employees);
        db.projectDao().getAllProjects().observe(this, projects -> projectList = projects);

        fabAdd.setOnClickListener(v -> showAddReportDialog());
        btnExportMonth.setOnClickListener(v -> exportPdf("month"));
        btnExportYear.setOnClickListener(v -> exportPdf("year"));
        btnExportMonthCsv.setOnClickListener(v -> exportCsv("month"));
        btnExportYearCsv.setOnClickListener(v -> exportCsv("year"));

        findViewById(R.id.btnEmployeeHoursReport).setOnClickListener(v ->
                startActivity(new Intent(this, EmployeeHoursReportActivity.class)));

        new Thread(() -> {
            List<Company> companies = db.companyDao().getAllCompaniesDirect();
            if (companies != null && !companies.isEmpty()) currentCompany = companies.get(0);
        }).start();

        setupNavigation();
    }

    private void updateDateHeader(Calendar cal) {
        SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        tvSelectedDate.setText("Rapoarte pentru: " + displayFormat.format(cal.getTime()));
    }

    private void syncAndLoadReportsForDate(String date) {
        // Fetch from server first, then show from local DB (LiveData auto-refreshes)
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().getReports(token, date).enqueue(new retrofit2.Callback<java.util.List<com.example.aplicatiegestiunefirma.model.DailyReport>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.example.aplicatiegestiunefirma.model.DailyReport>> call, retrofit2.Response<java.util.List<com.example.aplicatiegestiunefirma.model.DailyReport>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<DailyReport> serverReports = response.body();
                    new Thread(() -> {
                        java.util.Set<Integer> serverIds = new java.util.HashSet<>();
                        for (DailyReport r : serverReports) serverIds.add(r.getId());
                        for (DailyReport local : db.dailyReportDao().getReportsByDateDirect(date)) {
                            if (!serverIds.contains(local.getId())) db.dailyReportDao().delete(local);
                        }
                        for (DailyReport r : serverReports) db.dailyReportDao().insert(r);
                    }).start();
                }
                loadReportsForDate(date);
            }
            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.example.aplicatiegestiunefirma.model.DailyReport>> call, Throwable t) {
                loadReportsForDate(date);
            }
        });
    }

    private void syncEmployeesAndProjectsFromServer() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().getEmployees(token).enqueue(new retrofit2.Callback<java.util.List<com.example.aplicatiegestiunefirma.model.Employee>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.example.aplicatiegestiunefirma.model.Employee>> call, retrofit2.Response<java.util.List<com.example.aplicatiegestiunefirma.model.Employee>> response) {
                if (response.isSuccessful() && response.body() != null)
                    new Thread(() -> { for (com.example.aplicatiegestiunefirma.model.Employee e : response.body()) db.employeeDao().insert(e); }).start();
            }
            @Override public void onFailure(retrofit2.Call<java.util.List<com.example.aplicatiegestiunefirma.model.Employee>> call, Throwable t) {}
        });
        RetrofitClient.getApiService().getProjects(token).enqueue(new retrofit2.Callback<java.util.List<com.example.aplicatiegestiunefirma.model.Project>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.example.aplicatiegestiunefirma.model.Project>> call, retrofit2.Response<java.util.List<com.example.aplicatiegestiunefirma.model.Project>> response) {
                if (response.isSuccessful() && response.body() != null)
                    new Thread(() -> { for (com.example.aplicatiegestiunefirma.model.Project p : response.body()) db.projectDao().insert(p); }).start();
            }
            @Override public void onFailure(retrofit2.Call<java.util.List<com.example.aplicatiegestiunefirma.model.Project>> call, Throwable t) {}
        });
    }

    private void loadReportsForDate(String date) {
        if (currentReportsLiveData != null) {
            currentReportsLiveData.removeObservers(this);
        }
        currentReportsLiveData = db.dailyReportDao().getReportsByDate(date);
        currentReportsLiveData.observe(this, reports -> {
            if (reports == null || reports.isEmpty()) {
                rvReports.setVisibility(View.GONE);
                tvNoReports.setVisibility(View.VISIBLE);
            } else {
                rvReports.setVisibility(View.VISIBLE);
                tvNoReports.setVisibility(View.GONE);
                adapter.setReports(reports);
            }
        });
    }

    private void exportPdf(String type) {
        new Thread(() -> {
            List<DailyReport> reports;
            String fileName;
            if ("month".equals(type)) {
                String monthPrefix = selectedDate.substring(0, 7); // yyyy-MM
                reports = db.dailyReportDao().getReportsByMonthDirect(monthPrefix);
                fileName = "Raport_Lunar_" + monthPrefix + ".pdf";
            } else {
                String yearPrefix = selectedDate.substring(0, 4); // yyyy
                reports = db.dailyReportDao().getReportsByYearDirect(yearPrefix);
                fileName = "Raport_Anual_" + yearPrefix + ".pdf";
            }

            if (reports.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "Nu există date pentru acest raport!", Toast.LENGTH_SHORT).show());
                return;
            }

            generatePdfFile(reports, fileName);
        }).start();
    }

    private void generatePdfFile(List<DailyReport> reports, String fileName) {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        int pageNum = 1;
        int pageWidth = 595;
        int pageHeight = 842;
        int marginTop = 40;
        int marginBottom = 60;
        int y = marginTop;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // === HEADER with company logo placeholder ===
        if (currentCompany != null) {
            Paint logoPaint = new Paint();
            logoPaint.setColor(Color.parseColor("#1565C0"));
            canvas.drawRect(40, y, 90, y + 50, logoPaint);
            logoPaint.setColor(Color.WHITE);
            logoPaint.setTextSize(12);
            logoPaint.setFakeBoldText(true);
            canvas.drawText("LOGO", 48, y + 30, logoPaint);

            paint.setColor(Color.BLACK);
            paint.setTextSize(14);
            paint.setFakeBoldText(true);
            canvas.drawText(currentCompany.getName(), 100, y + 18, paint);
            paint.setTextSize(10);
            paint.setFakeBoldText(false);
            canvas.drawText("CUI: " + currentCompany.getTaxId(), 100, y + 33, paint);
            canvas.drawText(currentCompany.getAddress() != null ? currentCompany.getAddress() : "", 100, y + 46, paint);
        }
        y += 65;

        paint.setTextSize(18);
        paint.setFakeBoldText(true);
        paint.setColor(Color.parseColor("#1565C0"));
        canvas.drawText("Raport Activitate Firma", 40, y, paint);
        y += 30;

        paint.setTextSize(11);
        paint.setFakeBoldText(true);
        paint.setColor(Color.BLACK);
        canvas.drawText("Data", 40, y, paint);
        canvas.drawText("Angajat", 120, y, paint);
        canvas.drawText("Proiect", 250, y, paint);
        canvas.drawText("Ore", 400, y, paint);
        canvas.drawText("Descriere", 440, y, paint);
        
        y += 15;
        canvas.drawLine(40, y, 550, y, paint);
        y += 20;
        paint.setFakeBoldText(false);

        for (DailyReport r : reports) {
            if (y > pageHeight - marginBottom - 60) {
                document.finishPage(page);
                pageNum++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = marginTop + 20;
                // Repeat column headers
                paint.setTextSize(11);
                paint.setFakeBoldText(true);
                canvas.drawText("Data", 40, y, paint);
                canvas.drawText("Angajat", 120, y, paint);
                canvas.drawText("Proiect", 250, y, paint);
                canvas.drawText("Ore", 400, y, paint);
                canvas.drawText("Descriere", 440, y, paint);
                y += 15;
                canvas.drawLine(40, y, 550, y, paint);
                y += 20;
                paint.setFakeBoldText(false);
            }
            paint.setTextSize(10);
            canvas.drawText(r.getDate(), 40, y, paint);
            canvas.drawText(r.getEmployeeName(), 120, y, paint);
            String projName = r.getProjectName();
            if (projName.length() > 18) projName = projName.substring(0, 15) + "...";
            canvas.drawText(projName, 250, y, paint);
            canvas.drawText(String.valueOf(r.getHoursWorked()), 400, y, paint);
            
            String desc = r.getReportText();
            if (desc.length() > 18) desc = desc.substring(0, 15) + "...";
            canvas.drawText(desc, 440, y, paint);
            y += 20;
        }

        // === SIGNATURE / STAMP placeholder ===
        y += 40;
        if (y > pageHeight - 80) {
            document.finishPage(page);
            pageNum++;
            pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
            page = document.startPage(pageInfo);
            canvas = page.getCanvas();
            y = marginTop + 20;
        }
        paint.setTextSize(10);
        paint.setFakeBoldText(false);
        paint.setColor(Color.GRAY);
        canvas.drawText("Semnătura: _______________________", 40, y, paint);
        canvas.drawText("Ștampila:", 350, y, paint);
        Paint stampPaint = new Paint();
        stampPaint.setColor(Color.LTGRAY);
        stampPaint.setStyle(Paint.Style.STROKE);
        stampPaint.setStrokeWidth(2f);
        canvas.drawCircle(430, y + 30, 30, stampPaint);
        stampPaint.setTextSize(8);
        stampPaint.setStyle(Paint.Style.FILL);
        canvas.drawText("ȘTAMPILĂ", 410, y + 33, stampPaint);

        document.finishPage(page);
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
        try {
            document.writeTo(new FileOutputStream(file));
            runOnUiThread(() -> {
                Toast.makeText(this, "PDF generat!", Toast.LENGTH_SHORT).show();
                openPdf(file);
            });
        } catch (IOException e) {
            runOnUiThread(() -> Toast.makeText(this, "Eroare la generare PDF", Toast.LENGTH_SHORT).show());
        }
        document.close();
    }

    private void openPdf(File file) {
        Uri path = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Deschide PDF"));
    }

    private void exportCsv(String type) {
        new Thread(() -> {
            List<DailyReport> reports;
            String fileName;
            if ("month".equals(type)) {
                String monthPrefix = selectedDate.substring(0, 7);
                reports = db.dailyReportDao().getReportsByMonthDirect(monthPrefix);
                fileName = "Raport_Lunar_" + monthPrefix + ".csv";
            } else {
                String yearPrefix = selectedDate.substring(0, 4);
                reports = db.dailyReportDao().getReportsByYearDirect(yearPrefix);
                fileName = "Raport_Anual_" + yearPrefix + ".csv";
            }

            if (reports.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "Nu există date!", Toast.LENGTH_SHORT).show());
                return;
            }

            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Data,Angajat,Proiect,Ore,Descriere\n");
                for (DailyReport r : reports) {
                    writer.write(String.format("\"%s\",\"%s\",\"%s\",%.1f,\"%s\"\n",
                            r.getDate(), r.getEmployeeName(), r.getProjectName(),
                            r.getHoursWorked(), r.getReportText().replace("\"", "\"\"")));
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "CSV exportat!", Toast.LENGTH_SHORT).show();
                    openCsv(file);
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Eroare la export CSV", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void openCsv(File file) {
        Uri path = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(path, "text/csv");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Deschide CSV"));
        } catch (Exception e) {
            Toast.makeText(this, "CSV salvat: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_reports);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, MainActivity.class)); finish(); return true; }
            else if (id == R.id.nav_projects) { startActivity(new Intent(this, ProjectActivity.class)); finish(); return true; }
            else if (id == R.id.nav_invoices) { startActivity(new Intent(this, InvoiceActivity.class)); finish(); return true; }
            return true;
        });
    }

    private void updatePhotoPreview(Uri uri) {
        if (dialogPhotoPreview != null) {
            dialogPhotoPreview.setVisibility(View.VISIBLE);
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                dialogPhotoPreview.setImageBitmap(bmp);
            } catch (Exception e) {
                dialogPhotoPreview.setImageURI(uri);
            }
        }
        if (dialogPhotoStatus != null) dialogPhotoStatus.setText("Fotografie selectata");
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                pendingLatitude = location.getLatitude();
                pendingLongitude = location.getLongitude();
                if (dialogLocationStatus != null) {
                    dialogLocationStatus.setText(String.format(Locale.getDefault(),
                            "%.5f, %.5f", pendingLatitude, pendingLongitude));
                }
            } else {
                Toast.makeText(this, "Locatia nu este disponibila", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddReportDialog() {
        if (employeeList.isEmpty() || projectList.isEmpty()) {
            Toast.makeText(this, "Adaugă întâi angajați și proiecte!", Toast.LENGTH_SHORT).show();
            return;
        }

        pendingPhotoUri = null;
        pendingLatitude = 0.0;
        pendingLongitude = 0.0;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_report, null);
        builder.setView(dialogView);

        Spinner spinnerEmployees = dialogView.findViewById(R.id.spinnerEmployees);
        Spinner spinnerProjects = dialogView.findViewById(R.id.spinnerProjectsReport);
        TextInputEditText etDate = dialogView.findViewById(R.id.etReportDate);
        TextInputEditText etHours = dialogView.findViewById(R.id.etReportHours);
        TextInputEditText etText = dialogView.findViewById(R.id.etReportText);
        MaterialButton btnAttachPhoto = dialogView.findViewById(R.id.btnAttachPhoto);
        MaterialButton btnGetLocation = dialogView.findViewById(R.id.btnGetLocation);
        dialogPhotoPreview = dialogView.findViewById(R.id.ivPhotoPreview);
        dialogPhotoStatus = dialogView.findViewById(R.id.tvPhotoStatus);
        dialogLocationStatus = dialogView.findViewById(R.id.tvLocationStatus);

        etDate.setText(selectedDate);

        List<String> eNames = new ArrayList<>();
        for (Employee e : employeeList) eNames.add(e.getFullName());
        ArrayAdapter<String> eAdp = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, eNames);
        eAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEmployees.setAdapter(eAdp);

        List<String> pNames = new ArrayList<>();
        for (Project p : projectList) pNames.add(p.getName());
        ArrayAdapter<String> pAdp = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pNames);
        pAdp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProjects.setAdapter(pAdp);

        btnAttachPhoto.setOnClickListener(v -> {
            String[] options = {"Camera", "Galerie"};
            new AlertDialog.Builder(this)
                    .setTitle("Selecteaza sursa")
                    .setItems(options, (d, which) -> {
                        if (which == 0) {
                            // Camera
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
                                return;
                            }
                            try {
                                File photoFile = File.createTempFile("photo_" + System.currentTimeMillis(),
                                        ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
                                cameraFileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraFileUri);
                                cameraLauncher.launch(cameraIntent);
                            } catch (IOException e) {
                                Toast.makeText(this, "Eroare la deschiderea camerei", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Galerie
                            Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            galleryLauncher.launch(galleryIntent);
                        }
                    })
                    .show();
        });

        btnGetLocation.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                locationPermLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        builder.setTitle("Raport Zilnic Nou")
                .setPositiveButton("Salveaza", (dialog, which) -> {
                    Employee selEmp = employeeList.get(spinnerEmployees.getSelectedItemPosition());
                    Project selProj = projectList.get(spinnerProjects.getSelectedItemPosition());
                    String date = etDate.getText().toString();
                    String hoursStr = etHours.getText().toString();
                    String text = etText.getText().toString();
                    double hours = 0;
                    try { hours = Double.parseDouble(hoursStr); } catch (Exception e) {}

                    if (!date.isEmpty() && !text.isEmpty()) {
                        DailyReport report = new DailyReport(selEmp.getId(), selEmp.getFullName(),
                                selProj.getId(), selProj.getName(), date, text, hours);
                        report.setPhotoUri(pendingPhotoUri);
                        report.setLatitude(pendingLatitude);
                        report.setLongitude(pendingLongitude);
                        if (pendingPhotoUri != null) {
                            String token = "Bearer " + sharedPreferences.getString("token", "");
                            com.example.aplicatiegestiunefirma.util.FileUploadHelper.uploadFile(
                                    this, Uri.parse(pendingPhotoUri), token,
                                    new com.example.aplicatiegestiunefirma.util.FileUploadHelper.UploadCallback() {
                                        @Override public void onSuccess(String fileUrl) {
                                            report.setPhotoUri(fileUrl);
                                            saveReport(report);
                                        }
                                        @Override public void onFailure(String error) {
                                            Toast.makeText(ReportsActivity.this, "Poza nu s-a putut incarca pe server - server offline", Toast.LENGTH_SHORT).show();
                                            saveReport(report);
                                        }
                                    });
                        } else {
                            saveReport(report);
                        }
                        ActivityLogger.logCreate(this, "Raport", selEmp.getFullName() + " - " + date);
                    }
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    private void saveReport(DailyReport report) {
        new Thread(() -> {
            db.dailyReportDao().insert(report);
            runOnUiThread(() -> Toast.makeText(this, "Raport salvat local", Toast.LENGTH_SHORT).show());
        }).start();

        String token = "Bearer " + sharedPreferences.getString("token", "");
        RetrofitClient.getApiService().addReport(token, report).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ReportsActivity.this, "Raport sincronizat cu serverul!", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : "cod " + response.code();
                        Toast.makeText(ReportsActivity.this, "Eroare server: " + err, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(ReportsActivity.this, "Eroare server: " + response.code(), Toast.LENGTH_LONG).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ReportsActivity.this, "Salvat local - server offline", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
