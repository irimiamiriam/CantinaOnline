package com.example.cantinaonline;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    private List<Student> studentsList;
    private List<Student> selectedStudents;

    public StudentAdapter(List<Student> studentsList, List<Student> selectedStudents) {
        this.studentsList = studentsList;
        this.selectedStudents = selectedStudents;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.student_item, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        Student student = studentsList.get(position);

        // Bind student data
        holder.nameTextView.setText(student.getName());
        holder.remainingDaysTextView.setText("Zile rămase: " + student.getDaysPaid());

        // Handle checkbox selection
        holder.selectCheckBox.setOnCheckedChangeListener(null); // Prevent unintended behavior
        holder.selectCheckBox.setChecked(selectedStudents.contains(student));

        holder.selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedStudents.contains(student)) {
                    selectedStudents.add(student);
                }
            } else {
                selectedStudents.remove(student);
            }
        });
    }

    @Override
    public int getItemCount() {
        return studentsList.size();
    }

    public void updateList(List<Student> newStudentsList) {
        this.studentsList = newStudentsList;
        notifyDataSetChanged();
    }

    public static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView remainingDaysTextView;
        CheckBox selectCheckBox;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            remainingDaysTextView = itemView.findViewById(R.id.daysPaidTextView);
            selectCheckBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
