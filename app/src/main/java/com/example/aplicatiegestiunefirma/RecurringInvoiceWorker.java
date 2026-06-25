package com.example.aplicatiegestiunefirma;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Invoice;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Worker care ruleaza zilnic si genereaza automat copii ale facturilor recurente
 * in ziua configurata din luna.
 */
public class RecurringInvoiceWorker extends Worker {

    public RecurringInvoiceWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<Invoice> allInvoices = db.invoiceDao().getAllInvoicesDirect();

        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear = today.get(Calendar.YEAR);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (Invoice invoice : allInvoices) {
            if (!invoice.isRecurring() || invoice.getRecurringDay() <= 0) continue;

            // Verifica daca astazi este ziua configurata
            if (invoice.getRecurringDay() == todayDay) {
                // Formeaza data de emitere si scadenta (30 zile)
                Calendar emitere = Calendar.getInstance();
                emitere.set(todayYear, todayMonth, todayDay);

                Calendar scadenta = Calendar.getInstance();
                scadenta.set(todayYear, todayMonth, todayDay);
                scadenta.add(Calendar.DAY_OF_MONTH, 30);

                String dateStr = sdf.format(emitere.getTime());
                String dueDateStr = sdf.format(scadenta.getTime());

                // Verifica daca nu s-a generat deja in aceasta luna
                boolean alreadyGenerated = allInvoices.stream().anyMatch(inv ->
                        inv.getProjectId() == invoice.getProjectId()
                        && dateStr.equals(inv.getDate())
                        && !inv.isRecurring() // copiile nu sunt recurente
                );

                if (!alreadyGenerated) {
                    String newNumber = invoice.getInvoiceNumber() + "-R-" + todayYear + String.format("%02d", todayMonth + 1);
                    Invoice copy = new Invoice(
                            newNumber,
                            invoice.getProjectId(),
                            invoice.getProjectName(),
                            invoice.getAmount(),
                            dateStr,
                            dueDateStr,
                            false,
                            invoice.getType(),
                            null
                    );
                    copy.setVatPercent(invoice.getVatPercent());
                    copy.setRecurring(false); // copia nu e recurenta
                    db.invoiceDao().insert(copy);
                }
            }
        }
        return Result.success();
    }
}
