package com.example.aplicatiegestiunefirma;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.BankTransaction;
import com.example.aplicatiegestiunefirma.model.Invoice;
import com.google.android.material.card.MaterialCardView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BankReconciliationActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<BankTransaction> transactions = new ArrayList<>();
    private List<Invoice> invoices = new ArrayList<>();

    private final ActivityResultLauncher<Intent> csvPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri csvUri = result.getData().getData();
                    importCsv(csvUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_reconciliation);

        db = AppDatabase.getInstance(this);
        rvTransactions = findViewById(R.id.rvTransactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(transactions);
        rvTransactions.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnImportCsv).setOnClickListener(v -> pickCsvFile());
        findViewById(R.id.btnAutoMatch).setOnClickListener(v -> autoMatchTransactions());
        findViewById(R.id.btnClearTransactions).setOnClickListener(v -> confirmClear());

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            transactions = db.bankTransactionDao().getAllTransactions();
            invoices = db.invoiceDao().getAllInvoicesDirect();
            runOnUiThread(() -> {
                adapter.setData(transactions);
                ((TextView) findViewById(R.id.tvTransactionCount))
                        .setText(transactions.size() + " tranzactii importate");
                long unmatched = transactions.stream().filter(t -> !t.isMatched()).count();
                ((TextView) findViewById(R.id.tvUnmatchedCount))
                        .setText(unmatched + " nepotrivite");
            });
        }).start();
    }

    private void pickCsvFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        csvPickerLauncher.launch(intent);
    }

    private void importCsv(Uri uri) {
        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                boolean firstLine = true;
                int imported = 0;

                while ((line = reader.readLine()) != null) {
                    if (firstLine) { firstLine = false; continue; } // Skip header
                    String[] parts = line.split(",", -1);
                    if (parts.length < 3) continue;
                    // Format CSV: data, descriere, suma, tip(CREDIT/DEBIT)
                    String date = parts[0].trim().replaceAll("\"", "");
                    String desc = parts[1].trim().replaceAll("\"", "");
                    double amount = 0;
                    try { amount = Double.parseDouble(parts[2].trim().replaceAll("\"", "").replace(",", ".")); } catch (Exception ignored) {}
                    String type = parts.length > 3 ? parts[3].trim().replaceAll("\"", "") : (amount >= 0 ? "CREDIT" : "DEBIT");
                    if (amount < 0) { amount = Math.abs(amount); type = "DEBIT"; }

                    BankTransaction tx = new BankTransaction(date, desc, amount, type, false, 0);
                    db.bankTransactionDao().insert(tx);
                    imported++;
                }
                reader.close();
                final int count = imported;
                runOnUiThread(() -> {
                    Toast.makeText(this, count + " tranzactii importate!", Toast.LENGTH_SHORT).show();
                    loadData();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Eroare import CSV: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void autoMatchTransactions() {
        new Thread(() -> {
            int matched = 0;
            for (BankTransaction tx : transactions) {
                if (tx.isMatched()) continue;
                for (Invoice inv : invoices) {
                    // Potrivire dupa suma si data aproximativa
                    double invTotal = inv.getAmountWithVat();
                    if (Math.abs(tx.getAmount() - invTotal) < 0.5) {
                        tx.setMatched(true);
                        tx.setMatchedInvoiceId(inv.getId());
                        db.bankTransactionDao().update(tx);
                        matched++;
                        break;
                    }
                }
            }
            final int m = matched;
            loadData();
            runOnUiThread(() -> Toast.makeText(this, "Potrivite automat: " + m + " tranzactii", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("Sterge toate tranzactiile?")
                .setPositiveButton("Da", (d, w) -> new Thread(() -> {
                    db.bankTransactionDao().deleteAll();
                    loadData();
                }).start())
                .setNegativeButton("Nu", null)
                .show();
    }

    // ==================== ADAPTER ====================
    static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {
        List<BankTransaction> list;
        TransactionAdapter(List<BankTransaction> list) { this.list = list; }
        void setData(List<BankTransaction> d) { list = d; notifyDataSetChanged(); }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.two_line_list_item, parent, false);
            v.setPadding(32, 20, 32, 20);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            BankTransaction tx = list.get(position);
            String type = "CREDIT".equals(tx.getType()) ? "(+)" : "(-)";
            holder.tv1.setText(type + " " + String.format("%.2f RON", tx.getAmount()) + "  -  " + tx.getDate());
            holder.tv2.setText(tx.getDescription() + (tx.isMatched() ? "  [POTRIVITA]" : "  [NEPOTRIVITA]"));
            holder.tv1.setTextColor(tx.isMatched() ? 0xFF22C55E : 0xFFEF4444);
        }

        @Override public int getItemCount() { return list.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tv1, tv2;
            VH(View v) { super(v); tv1 = v.findViewById(android.R.id.text1); tv2 = v.findViewById(android.R.id.text2); }
        }
    }
}
