package com.example.aplicatiegestiunefirma;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.Invoice;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder> {

    private List<Invoice> invoices = new ArrayList<>();
    private OnInvoiceClickListener listener;

    public interface OnInvoiceClickListener {
        void onInvoiceClick(Invoice invoice);
    }

    public InvoiceAdapter(OnInvoiceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public InvoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invoice, parent, false);
        return new InvoiceViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull InvoiceViewHolder holder, int position) {
        Invoice current = invoices.get(position);
        holder.tvNum.setText("Factură #" + current.getInvoiceNumber());
        holder.tvProject.setText(current.getProjectName());
        holder.tvAmount.setText(String.format("%.2f", current.getAmount()));
        holder.tvDate.setText("Scadență: " + current.getDate());

        if (current.isPaid()) {
            holder.tvStatus.setText("ACHITATĂ");
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
            holder.cardIcon.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            holder.ivIcon.setImageResource(android.R.drawable.checkbox_on_background);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            holder.tvDate.setTextColor(Color.parseColor("#757575")); // Neutral
        } else {
            holder.tvStatus.setText("NEPLĂTITĂ");
            holder.tvStatus.setTextColor(Color.parseColor("#E53935")); // Red
            holder.cardIcon.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
            holder.tvDate.setTextColor(Color.parseColor("#E53935")); // Warning red
        }

        holder.itemView.setOnClickListener(v -> listener.onInvoiceClick(current));
    }

    @Override
    public int getItemCount() {
        return invoices.size();
    }

    public void setInvoices(List<Invoice> invoices) {
        this.invoices = invoices;
        notifyDataSetChanged();
    }

    class InvoiceViewHolder extends RecyclerView.ViewHolder {
        private TextView tvNum, tvStatus, tvProject, tvAmount, tvDate;
        private MaterialCardView cardIcon;
        private ImageView ivIcon;

        public InvoiceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNum = itemView.findViewById(R.id.tvInvoiceNum);
            tvStatus = itemView.findViewById(R.id.tvInvoiceStatus);
            tvProject = itemView.findViewById(R.id.tvInvoiceProject);
            tvAmount = itemView.findViewById(R.id.tvInvoiceAmount);
            tvDate = itemView.findViewById(R.id.tvInvoiceDate);
            cardIcon = itemView.findViewById(R.id.cardStatusIcon);
            ivIcon = itemView.findViewById(R.id.ivStatusIcon);
        }
    }
}
