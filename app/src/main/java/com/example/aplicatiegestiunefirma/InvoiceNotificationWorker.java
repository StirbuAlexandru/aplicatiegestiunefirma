package com.example.aplicatiegestiunefirma;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.aplicatiegestiunefirma.model.AppDatabase;
import com.example.aplicatiegestiunefirma.model.Invoice;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class InvoiceNotificationWorker extends Worker {

    public static final String CHANNEL_ID = "invoice_reminders";

    public InvoiceNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        List<Invoice> unpaidInvoices = db.invoiceDao().getUnpaidInvoicesDirect();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        int notifId = 1000;

        for (Invoice invoice : unpaidInvoices) {
            if (invoice.getDueDate() == null || invoice.getDueDate().isEmpty()) continue;

            try {
                Calendar dueDate = Calendar.getInstance();
                dueDate.setTime(sdf.parse(invoice.getDueDate()));
                dueDate.set(Calendar.HOUR_OF_DAY, 0);
                dueDate.set(Calendar.MINUTE, 0);
                dueDate.set(Calendar.SECOND, 0);
                dueDate.set(Calendar.MILLISECOND, 0);

                long diffMillis = dueDate.getTimeInMillis() - today.getTimeInMillis();
                long daysUntilDue = TimeUnit.MILLISECONDS.toDays(diffMillis);

                if (daysUntilDue == 1 || daysUntilDue == 3 || daysUntilDue == 7) {
                    String title;
                    if (daysUntilDue == 1) title = "Factura scadenta MAINE!";
                    else if (daysUntilDue == 3) title = "Factura scadenta in 3 zile";
                    else title = "Factura scadenta in 7 zile";

                    String message = String.format(Locale.getDefault(),
                            "Factura #%s - %.2f RON (Total cu TVA: %.2f RON) scade pe %s",
                            invoice.getInvoiceNumber(),
                            invoice.getAmount(),
                            invoice.getAmountWithVat(),
                            invoice.getDueDate());

                    showNotification(title, message, notifId++);
                } else if (daysUntilDue < 0) {
                    // Factura expirata, trimite alerta urgenta
                    String title = "FACTURA EXPIRATA - " + Math.abs(daysUntilDue) + " zile intarziere!";
                    String message = String.format(Locale.getDefault(),
                            "Factura #%s - %.2f RON (cu TVA: %.2f RON) a expirat pe %s",
                            invoice.getInvoiceNumber(),
                            invoice.getAmount(),
                            invoice.getAmountWithVat(),
                            invoice.getDueDate());
                    showNotification(title, message, notifId++);
                }
            } catch (Exception e) {
                // Skip invalid dates
            }
        }

        return Result.success();
    }

    private void showNotification(String title, String message, int notifId) {
        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notificări Facturi",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Reamintiri pentru scadența facturilor");
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        manager.notify(notifId, builder.build());
    }
}
