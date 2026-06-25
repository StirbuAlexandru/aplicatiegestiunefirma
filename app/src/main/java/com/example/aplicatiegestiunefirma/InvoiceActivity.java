package com.example.aplicatiegestiunefirma;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Invoice;
import com.example.aplicatiegestiunefirma.model.Project;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceActivity extends AppCompatActivity implements InvoiceAdapter.OnInvoiceClickListener {

    private AppDatabase db;
    private RecyclerView rvInvoices;
    private InvoiceAdapter adapter;
    private FloatingActionButton fabAdd;
    private TabLayout tabLayout;
    private List<Project> projectsList = new ArrayList<>();
    private List<Invoice> allInvoices = new ArrayList<>();
    private String currentSearchQuery = "";
    private android.content.SharedPreferences sharedPreferences;

    private Uri selectedPdfUri;
    private TextView tvPdfStatusRef;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedPdfUri = result.getData().getData();
                    getContentResolver().takePersistableUriPermission(selectedPdfUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (tvPdfStatusRef != null) {
                        tvPdfStatusRef.setText("PDF selectat: " + selectedPdfUri.getLastPathSegment());
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoices);

        db = AppDatabase.getInstance(this);
        sharedPreferences = getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE);
        rvInvoices = findViewById(R.id.rvInvoices);
        fabAdd = findViewById(R.id.fabAddInvoice);
        tabLayout = findViewById(R.id.tabLayout);

        adapter = new InvoiceAdapter(this);
        rvInvoices.setLayoutManager(new LinearLayoutManager(this));
        rvInvoices.setAdapter(adapter);

        db.invoiceDao().getAllInvoices().observe(this, invoices -> {
            allInvoices = invoices;
            filterInvoices();
        });

        db.projectDao().getAllProjects().observe(this, projects -> {
            projectsList = projects;
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { filterInvoices(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabAdd.setOnClickListener(v -> showAddInvoiceDialog());
        setupSearch();
        setupNavigation();
        loadInvoicesFromServer();
    }

    private String token() {
        return "Bearer " + sharedPreferences.getString("token", "");
    }

    private void loadInvoicesFromServer() {
        RetrofitClient.getApiService().getInvoices(token()).enqueue(new Callback<List<Invoice>>() {
            @Override
            public void onResponse(Call<List<Invoice>> call, Response<List<Invoice>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Invoice> serverInvoices = response.body();
                    new Thread(() -> {
                        Set<Integer> serverIds = new HashSet<>();
                        for (Invoice i : serverInvoices) serverIds.add(i.getId());
                        for (Invoice local : db.invoiceDao().getAllInvoicesDirect()) {
                            if (!serverIds.contains(local.getId())) db.invoiceDao().delete(local);
                        }
                        for (Invoice i : serverInvoices) db.invoiceDao().insert(i);
                    }).start();
                }
            }
            @Override
            public void onFailure(Call<List<Invoice>> call, Throwable t) {
                Toast.makeText(InvoiceActivity.this, "Offline - date locale", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setSelectedItemId(R.id.nav_invoices);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class)); finish(); return true;
            } else if (id == R.id.nav_projects) {
                startActivity(new Intent(this, ProjectActivity.class)); finish(); return true;
            } else if (id == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class)); finish(); return true;
            }
            return true;
        });
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString(); filterInvoices();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterInvoices() {
        int pos = tabLayout.getSelectedTabPosition();
        List<Invoice> filtered;
        if (pos == 0) filtered = allInvoices;
        else if (pos == 1) filtered = allInvoices.stream().filter(Invoice::isPaid).collect(Collectors.toList());
        else filtered = allInvoices.stream().filter(i -> !i.isPaid()).collect(Collectors.toList());
        if (!currentSearchQuery.isEmpty()) {
            String lower = currentSearchQuery.toLowerCase();
            filtered = filtered.stream()
                    .filter(i -> (i.getInvoiceNumber() != null && i.getInvoiceNumber().toLowerCase().contains(lower))
                            || (i.getProjectName() != null && i.getProjectName().toLowerCase().contains(lower))
                            || String.valueOf(i.getAmount()).contains(lower))
                    .collect(Collectors.toList());
        }
        adapter.setInvoices(filtered);
    }

    @Override
    public void onInvoiceClick(Invoice invoice) { showInvoiceOptionsDialog(invoice); }

    private void showInvoiceOptionsDialog(Invoice invoice) {
        List<String> optionsList = new ArrayList<>();
        optionsList.add(invoice.isPaid() ? "Marcheaza ca NEPLATITA" : "Marcheaza ca PLATITA");
        optionsList.add("Editare");
        optionsList.add("Genereaza PDF");
        optionsList.add("Trimite pe Email");
        if (invoice.getPdfUri() != null) optionsList.add("Vezi PDF Factura");
        optionsList.add("Sterge");

        String[] options = optionsList.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Optiuni Factura: " + invoice.getInvoiceNumber())
                .setItems(options, (dialog, which) -> {
                    String sel = options[which];
                    if (sel.contains("Marcheaza")) {
                        invoice.setPaid(!invoice.isPaid());
                        new Thread(() -> db.invoiceDao().update(invoice)).start();
                        ActivityLogger.logUpdate(this, "Factura", invoice.getInvoiceNumber() + " -> " + (invoice.isPaid() ? "PLATITA" : "NEPLATITA"));
                    } else if (sel.equals("Editare")) {
                        showEditInvoiceDialog(invoice);
                    } else if (sel.equals("Genereaza PDF")) {
                        generateAndOpenInvoicePdf(invoice);
                    } else if (sel.equals("Trimite pe Email")) {
                        sendInvoiceByEmail(invoice);
                    } else if (sel.equals("Vezi PDF Factura")) {
                        openImportedPdf(invoice.getPdfUri());
                    } else if (sel.equals("Sterge")) {
                        confirmDeleteInvoice(invoice);
                    }
                })
                .show();
    }

    private void confirmDeleteInvoice(Invoice invoice) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmare stergere")
                .setMessage("Esti sigur ca vrei sa stergi factura #" + invoice.getInvoiceNumber() + "?")
                .setPositiveButton("Sterge", (dialog, which) -> {
                    new Thread(() -> db.invoiceDao().delete(invoice)).start();
                    ActivityLogger.logDelete(this, "Factura", invoice.getInvoiceNumber());
                    String token = "Bearer " + sharedPreferences.getString("token", "");
                    RetrofitClient.getApiService().deleteInvoice(token, invoice.getId()).enqueue(new Callback<ResponseBody>() {
                        @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
                        @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    private void openImportedPdf(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Deschide Factura"));
        } catch (Exception e) {
            Toast.makeText(this, "Nu se poate deschide fisierul.", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== GENERARE PDF FACTURA ====================
    private File generateInvoicePdfFile(Invoice invoice) throws Exception {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        // Header gradient simulation
        paint.setColor(Color.parseColor("#6366F1"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, 595, 100, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(24f);
        canvas.drawText("FACTURA", 30, 45, paint);
        paint.setTextSize(12f);
        canvas.drawText("#" + invoice.getInvoiceNumber(), 30, 70, paint);
        canvas.drawText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date()), 450, 70, paint);

        paint.setColor(Color.DKGRAY);
        paint.setTextSize(11f);
        int y = 140;
        canvas.drawText("Proiect: " + invoice.getProjectName(), 30, y, paint); y += 25;
        canvas.drawText("Data emitere: " + invoice.getDate(), 30, y, paint); y += 25;
        canvas.drawText("Data scadenta: " + invoice.getDueDate(), 30, y, paint); y += 25;
        canvas.drawText("Tip: " + ("INCOME".equals(invoice.getType()) ? "Iesire" : "Intrare"), 30, y, paint); y += 40;

        // Linie
        paint.setColor(Color.parseColor("#E5E7EB"));
        canvas.drawLine(30, y, 565, y, paint); y += 20;

        paint.setColor(Color.DKGRAY);
        paint.setTextSize(12f);
        canvas.drawText("Suma fara TVA:", 30, y, paint);
        canvas.drawText(String.format("%.2f RON", invoice.getAmount()), 450, y, paint); y += 25;

        double vatPct = invoice.getVatPercent();
        double vatAmt = invoice.getVatAmount();
        canvas.drawText("TVA (" + (int) vatPct + "%):", 30, y, paint);
        canvas.drawText(String.format("%.2f RON", vatAmt), 450, y, paint); y += 25;

        paint.setColor(Color.parseColor("#6366F1"));
        paint.setTextSize(14f);
        canvas.drawText("TOTAL:", 30, y, paint);
        canvas.drawText(String.format("%.2f RON", invoice.getAmountWithVat()), 430, y, paint); y += 40;

        paint.setColor(invoice.isPaid() ? Color.parseColor("#22C55E") : Color.parseColor("#EF4444"));
        paint.setTextSize(16f);
        canvas.drawText(invoice.isPaid() ? "PLATITA" : "NEPLATITA", 30, y, paint);

        if (invoice.isRecurring()) {
            paint.setColor(Color.parseColor("#8B5CF6"));
            paint.setTextSize(10f);
            canvas.drawText("* Factura recurenta - se genereaza in ziua " + invoice.getRecurringDay() + " a lunii", 30, y + 30, paint);
        }

        document.finishPage(page);
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir != null && !dir.exists()) dir.mkdirs();
        File file = new File(dir, "Factura_" + invoice.getInvoiceNumber().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        FileOutputStream fos = new FileOutputStream(file);
        document.writeTo(fos);
        document.close();
        fos.close();
        return file;
    }

    private void generateAndOpenInvoicePdf(Invoice invoice) {
        new Thread(() -> {
            try {
                File pdfFile = generateInvoicePdfFile(invoice);
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
                runOnUiThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Deschide PDF"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Eroare generare PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void sendInvoiceByEmail(Invoice invoice) {
        new Thread(() -> {
            try {
                File pdfFile = generateInvoicePdfFile(invoice);
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pdfFile);
                runOnUiThread(() -> {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("application/pdf");
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Factura #" + invoice.getInvoiceNumber());
                    emailIntent.putExtra(Intent.EXTRA_TEXT,
                            "Va trimitem atasat factura #" + invoice.getInvoiceNumber()
                            + "\nProiect: " + invoice.getProjectName()
                            + "\nTotal: " + String.format("%.2f RON", invoice.getAmountWithVat())
                            + "\nScadenta: " + invoice.getDueDate());
                    emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(emailIntent, "Trimite Factura"));
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Eroare trimitere email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ==================== DIALOG EDITARE ====================
    private void showEditInvoiceDialog(Invoice invoice) {
        if (projectsList.isEmpty()) {
            Toast.makeText(this, "Nu exista proiecte!", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedPdfUri = null;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_invoice, null);

        Spinner spinner = dialogView.findViewById(R.id.spinnerProjects);
        Spinner spinnerType = dialogView.findViewById(R.id.spinnerInvoiceType);
        TextInputEditText etNum = dialogView.findViewById(R.id.etInvoiceNumber);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etInvoiceAmount);
        TextInputEditText etDate = dialogView.findViewById(R.id.etInvoiceDate);
        TextInputEditText etDueDate = dialogView.findViewById(R.id.etInvoiceDueDate);
        TextInputEditText etVat = dialogView.findViewById(R.id.etVatPercent);
        TextView tvVatAmount = dialogView.findViewById(R.id.tvVatAmount);
        MaterialCheckBox cbPaid = dialogView.findViewById(R.id.cbIsPaid);
        MaterialCheckBox cbRecurring = dialogView.findViewById(R.id.cbIsRecurring);
        TextInputLayout layoutRecurringDay = dialogView.findViewById(R.id.layoutRecurringDay);
        TextInputEditText etRecurringDay = dialogView.findViewById(R.id.etRecurringDay);
        Button btnSelectPdf = dialogView.findViewById(R.id.btnSelectPdf);
        tvPdfStatusRef = dialogView.findViewById(R.id.tvPdfStatus);

        // Tip factura spinner
        List<String> types = Arrays.asList("Iesire (Vanzare)", "Intrare (Achizitie)");
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);
        spinnerType.setSelection("INCOME".equals(invoice.getType()) ? 0 : 1);

        List<String> projectNames = projectsList.stream().map(Project::getName).collect(Collectors.toList());
        ArrayAdapter<String> projAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, projectNames);
        projAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(projAdapter);

        etNum.setText(invoice.getInvoiceNumber());
        etAmount.setText(String.valueOf(invoice.getAmount()));
        etDate.setText(invoice.getDate());
        etDueDate.setText(invoice.getDueDate());
        etVat.setText(String.valueOf((int) invoice.getVatPercent()));
        cbPaid.setChecked(invoice.isPaid());
        cbRecurring.setChecked(invoice.isRecurring());
        if (invoice.isRecurring()) {
            layoutRecurringDay.setVisibility(View.VISIBLE);
            etRecurringDay.setText(String.valueOf(invoice.getRecurringDay()));
        }
        if (invoice.getPdfUri() != null) tvPdfStatusRef.setText("PDF existent");

        for (int i = 0; i < projectsList.size(); i++) {
            if (projectsList.get(i).getId() == invoice.getProjectId()) {
                spinner.setSelection(i); break;
            }
        }

        // TVA auto-calculate
        TextWatcher vatWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double amt = Double.parseDouble(etAmount.getText().toString());
                    double vat = Double.parseDouble(etVat.getText().toString());
                    tvVatAmount.setText(String.format("TVA: %.2f RON", amt * vat / 100));
                } catch (Exception ignored) {}
            }
        };
        etAmount.addTextChangedListener(vatWatcher);
        etVat.addTextChangedListener(vatWatcher);

        cbRecurring.setOnCheckedChangeListener((b, checked) ->
                layoutRecurringDay.setVisibility(checked ? View.VISIBLE : View.GONE));

        btnSelectPdf.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            filePickerLauncher.launch(intent);
        });

        new AlertDialog.Builder(this)
                .setTitle("Editare Factura")
                .setView(dialogView)
                .setPositiveButton("Salveaza", (dialog, which) -> {
                    int selPos = spinner.getSelectedItemPosition();
                    Project selProject = projectsList.get(selPos);
                    invoice.setInvoiceNumber(etNum.getText().toString());
                    invoice.setProjectId(selProject.getId());
                    invoice.setProjectName(selProject.getName());
                    invoice.setDate(etDate.getText().toString());
                    invoice.setDueDate(etDueDate.getText().toString());
                    invoice.setPaid(cbPaid.isChecked());
                    invoice.setType(spinnerType.getSelectedItemPosition() == 0 ? "INCOME" : "EXPENSE");
                    try { invoice.setAmount(Double.parseDouble(etAmount.getText().toString())); } catch (Exception ignored) {}
                    try { invoice.setVatPercent(Double.parseDouble(etVat.getText().toString())); } catch (Exception ignored) {}
                    invoice.setRecurring(cbRecurring.isChecked());
                    if (cbRecurring.isChecked()) {
                        try { invoice.setRecurringDay(Integer.parseInt(etRecurringDay.getText().toString())); } catch (Exception ignored) {}
                    } else {
                        invoice.setRecurringDay(0);
                    }
                    if (selectedPdfUri != null) {
                        com.example.aplicatiegestiunefirma.util.FileUploadHelper.uploadFile(
                                this, selectedPdfUri, token(),
                                new com.example.aplicatiegestiunefirma.util.FileUploadHelper.UploadCallback() {
                                    @Override public void onSuccess(String fileUrl) {
                                        invoice.setPdfUri(fileUrl);
                                        finishUpdateInvoice(invoice);
                                    }
                                    @Override public void onFailure(String error) {
                                        invoice.setPdfUri(selectedPdfUri.toString());
                                        Toast.makeText(InvoiceActivity.this, "PDF neîncărcat pe server - server offline", Toast.LENGTH_SHORT).show();
                                        finishUpdateInvoice(invoice);
                                    }
                                });
                    } else {
                        finishUpdateInvoice(invoice);
                    }
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    private void finishUpdateInvoice(Invoice invoice) {
        new Thread(() -> {
            db.invoiceDao().update(invoice);
            runOnUiThread(() -> Toast.makeText(this, "Factura actualizata", Toast.LENGTH_SHORT).show());
        }).start();
        ActivityLogger.logUpdate(this, "Factura", invoice.getInvoiceNumber());
        RetrofitClient.getApiService().updateInvoice(token(), invoice.getId(), invoice).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {}
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    // ==================== DIALOG ADAUGARE ====================
    private void showAddInvoiceDialog() {
        if (projectsList.isEmpty()) {
            Toast.makeText(this, "Adauga intai un proiect!", Toast.LENGTH_SHORT).show();
            return;
        }
        selectedPdfUri = null;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_invoice, null);

        Spinner spinner = dialogView.findViewById(R.id.spinnerProjects);
        Spinner spinnerType = dialogView.findViewById(R.id.spinnerInvoiceType);
        TextInputEditText etNum = dialogView.findViewById(R.id.etInvoiceNumber);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etInvoiceAmount);
        TextInputEditText etDate = dialogView.findViewById(R.id.etInvoiceDate);
        TextInputEditText etDueDate = dialogView.findViewById(R.id.etInvoiceDueDate);
        TextInputEditText etVat = dialogView.findViewById(R.id.etVatPercent);
        TextView tvVatAmount = dialogView.findViewById(R.id.tvVatAmount);
        MaterialCheckBox cbPaid = dialogView.findViewById(R.id.cbIsPaid);
        MaterialCheckBox cbRecurring = dialogView.findViewById(R.id.cbIsRecurring);
        TextInputLayout layoutRecurringDay = dialogView.findViewById(R.id.layoutRecurringDay);
        TextInputEditText etRecurringDay = dialogView.findViewById(R.id.etRecurringDay);
        Button btnSelectPdf = dialogView.findViewById(R.id.btnSelectPdf);
        tvPdfStatusRef = dialogView.findViewById(R.id.tvPdfStatus);

        List<String> types = Arrays.asList("Iesire (Vanzare)", "Intrare (Achizitie)");
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        List<String> projectNames = projectsList.stream().map(Project::getName).collect(Collectors.toList());
        ArrayAdapter<String> projAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, projectNames);
        projAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(projAdapter);

        // TVA auto-calculate
        TextWatcher vatWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double amt = Double.parseDouble(etAmount.getText().toString());
                    double vat = Double.parseDouble(etVat.getText().toString());
                    tvVatAmount.setText(String.format("TVA: %.2f RON", amt * vat / 100));
                } catch (Exception ignored) {}
            }
        };
        etAmount.addTextChangedListener(vatWatcher);
        etVat.addTextChangedListener(vatWatcher);

        cbRecurring.setOnCheckedChangeListener((b, checked) ->
                layoutRecurringDay.setVisibility(checked ? View.VISIBLE : View.GONE));

        btnSelectPdf.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            filePickerLauncher.launch(intent);
        });

        new AlertDialog.Builder(this)
                .setTitle("Adauga Factura")
                .setView(dialogView)
                .setPositiveButton("Salveaza", (dialog, which) -> {
                    int selPos = spinner.getSelectedItemPosition();
                    Project selProject = projectsList.get(selPos);
                    String num = etNum.getText().toString();
                    String amountStr = etAmount.getText().toString();
                    String type = spinnerType.getSelectedItemPosition() == 0 ? "INCOME" : "EXPENSE";
                    String date = etDate.getText().toString();
                    String dueDate = etDueDate.getText().toString();
                    boolean isPaid = cbPaid.isChecked();
                    double amount = 0;
                    try { amount = Double.parseDouble(amountStr); } catch (Exception ignored) {}
                    double vatPct = 19.0;
                    try { vatPct = Double.parseDouble(etVat.getText().toString()); } catch (Exception ignored) {}
                    if (!num.isEmpty()) {
                        Invoice inv = new Invoice(num, selProject.getId(), selProject.getName(), amount, date, dueDate, isPaid, type, null);
                        inv.setVatPercent(vatPct);
                        inv.setRecurring(cbRecurring.isChecked());
                        if (cbRecurring.isChecked()) {
                            try { inv.setRecurringDay(Integer.parseInt(etRecurringDay.getText().toString())); } catch (Exception ignored) {}
                        }
                        if (selectedPdfUri != null) {
                            com.example.aplicatiegestiunefirma.util.FileUploadHelper.uploadFile(
                                    this, selectedPdfUri, token(),
                                    new com.example.aplicatiegestiunefirma.util.FileUploadHelper.UploadCallback() {
                                        @Override public void onSuccess(String fileUrl) {
                                            inv.setPdfUri(fileUrl);
                                            finishSaveNewInvoice(inv, num);
                                        }
                                        @Override public void onFailure(String error) {
                                            inv.setPdfUri(selectedPdfUri.toString());
                                            Toast.makeText(InvoiceActivity.this, "PDF neîncărcat pe server - server offline", Toast.LENGTH_SHORT).show();
                                            finishSaveNewInvoice(inv, num);
                                        }
                                    });
                        } else {
                            finishSaveNewInvoice(inv, num);
                        }
                    }
                })
                .setNegativeButton("Anuleaza", null)
                .show();
    }

    private void finishSaveNewInvoice(Invoice inv, String num) {
        new Thread(() -> db.invoiceDao().insert(inv)).start();
        ActivityLogger.logCreate(this, "Factura", num);
        RetrofitClient.getApiService().addInvoice(token(), inv).enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) loadInvoicesFromServer();
            }
            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(InvoiceActivity.this, "Salvat local - server offline", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
