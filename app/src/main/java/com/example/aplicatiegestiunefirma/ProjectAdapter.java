package com.example.aplicatiegestiunefirma;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aplicatiegestiunefirma.model.Project;

import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projects = new ArrayList<>();
    private OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
        void onProjectLongClick(Project project);
    }

    public ProjectAdapter(OnProjectClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project currentProject = projects.get(position);
        holder.tvName.setText(currentProject.getName());
        holder.tvClient.setText(currentProject.getClientName());
        holder.tvDeadline.setText(currentProject.getDeadline());
        holder.tvStatus.setText(currentProject.getStatus());
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProjectClick(currentProject);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onProjectLongClick(currentProject);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
        notifyDataSetChanged();
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvClient;
        private TextView tvDeadline;
        private TextView tvStatus;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProjectName);
            tvClient = itemView.findViewById(R.id.tvClientName);
            tvDeadline = itemView.findViewById(R.id.tvDeadline);
            tvStatus = itemView.findViewById(R.id.tvProjectStatus);
        }
    }
}