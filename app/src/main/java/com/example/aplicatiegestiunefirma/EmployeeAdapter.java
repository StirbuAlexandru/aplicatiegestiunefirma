package com.example.aplicatiegestiunefirma;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.Employee;

import java.util.ArrayList;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private List<Employee> employees = new ArrayList<>();
    private OnEmployeeClickListener listener;

    public interface OnEmployeeClickListener {
        void onEmployeeClick(Employee employee);
        void onEmployeeLongClick(Employee employee);
    }

    public EmployeeAdapter(OnEmployeeClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Employee currentEmployee = employees.get(position);
        holder.tvName.setText(currentEmployee.getFullName());
        holder.tvPosition.setText(currentEmployee.getPosition());
        holder.tvPhone.setText(currentEmployee.getPhone());
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmployeeClick(currentEmployee);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onEmployeeLongClick(currentEmployee);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
        notifyDataSetChanged();
    }

    class EmployeeViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvPosition;
        private TextView tvPhone;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvPosition = itemView.findViewById(R.id.tvEmployeePosition);
            tvPhone = itemView.findViewById(R.id.tvEmployeePhone);
        }
    }
}