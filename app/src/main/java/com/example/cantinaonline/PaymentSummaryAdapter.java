package com.example.cantinaonline;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PaymentSummaryAdapter extends RecyclerView.Adapter<PaymentSummaryAdapter.ViewHolder> {

    private List<Student> students;
    private int daysPaid;

    public PaymentSummaryAdapter(List<Student> students, int daysPaid) {
        this.students = students;
        this.daysPaid = daysPaid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student student = students.get(position);
        Long restante = student.getRestante();
        Long payment = 18 * (daysPaid - restante);

        holder.studentNameTextView.setText(student.getName());
        holder.paymentTextView.setText("Plata: " + payment + " Lei");
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameTextView;
        TextView paymentTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            paymentTextView = itemView.findViewById(R.id.paymentTextView);
        }
    }
}
