package com.example.aplicatiegestiunefirma;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.DailyReport;
import com.example.aplicatiegestiunefirma.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private List<DailyReport> reports = new ArrayList<>();

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        DailyReport current = reports.get(position);
        holder.tvDate.setText("Data: " + current.getDate());
        holder.tvEmployee.setText("Angajat: " + current.getEmployeeName());
        holder.tvProject.setText("Proiect: " + current.getProjectName());
        holder.tvDesc.setText(current.getReportText());
        double h = current.getHoursWorked();
        String hoursLabel = (h == Math.floor(h))
                ? (int) h + "h"
                : String.format(java.util.Locale.getDefault(), "%.1fh", h);
        holder.tvHours.setText(hoursLabel);

        if (current.hasPhoto()) {
            holder.tvViewPhoto.setVisibility(View.VISIBLE);
            holder.tvViewPhoto.setOnClickListener(v -> {
                try {
                    String url = RetrofitClient.getFileUrl(current.getPhotoUri());
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "Nu se poate deschide fotografia", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.tvViewPhoto.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    public void setReports(List<DailyReport> reports) {
        this.reports = reports;
        notifyDataSetChanged();
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDate, tvEmployee, tvProject, tvDesc, tvHours, tvViewPhoto;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvReportDate);
            tvEmployee = itemView.findViewById(R.id.tvReportEmployee);
            tvProject = itemView.findViewById(R.id.tvReportProject);
            tvDesc = itemView.findViewById(R.id.tvReportDescription);
            tvHours = itemView.findViewById(R.id.tvReportHours);
            tvViewPhoto = itemView.findViewById(R.id.tvViewPhoto);
        }
    }
}