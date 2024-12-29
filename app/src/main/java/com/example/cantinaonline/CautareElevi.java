package com.example.cantinaonline;

import android.os.Bundle;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cantinaonline.R;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CautareElevi extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView studentsRecyclerView;
    private EditText searchBar, daysInput;
    private Button payButton;
    private StudentAdapter studentAdapter;
    private List<Student> studentsList = new ArrayList<>();
    private List<Student> selectedStudents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cautare_elevi);

        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        db = FirebaseFirestore.getInstance();

        studentsRecyclerView = findViewById(R.id.studentsRecyclerView);
        searchBar = findViewById(R.id.searchBar);
        daysInput = findViewById(R.id.daysInput);
        payButton = findViewById(R.id.payButton);

        studentAdapter = new StudentAdapter(studentsList, selectedStudents);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentsRecyclerView.setAdapter(studentAdapter);




        // Load students from the database
        loadStudents();

        // Search functionality
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add payment logic
        payButton.setOnClickListener(v ->  {

            int daysPaid = Integer.parseInt(daysInput.getText().toString());

            if (!selectedStudents.isEmpty() && daysPaid > 0) {
                showPaymentSummaryDialog(selectedStudents, daysPaid);
            } else {
                Toast.makeText(this, "Selectează elevi și introduceți numărul de zile!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStudents() {
        db.collection("students")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        studentsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String id = document.getId();
                            String name = document.getString("name");
                            Long daysPaid = document.getLong("daysPaid");
                            Long restante = document.getLong("restante");
                            studentsList.add(new Student(id, name, daysPaid != null ? daysPaid : 0, restante != null ? restante : 0));
                        }
                        studentAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void filterStudents(String query) {
        List<Student> filteredList = new ArrayList<>();
        for (Student student : studentsList) {
            if (student.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(student);
            }
        }
        studentAdapter.updateList(filteredList);
    }

    private void addPaymentsToSelectedStudents() {
        String daysStr = daysInput.getText().toString();
        if (daysStr.isEmpty()) {
            Toast.makeText(this, "Introduceți numărul de zile", Toast.LENGTH_SHORT).show();
            return;
        }

        int daysToAdd = Integer.parseInt(daysStr);
        for (Student student : selectedStudents) {
            db.collection("students").document(student.getId())
                    .update("daysPaid", student.getDaysPaid() + daysToAdd)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Plată adăugată cu succes pentru " + student.getName(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Eroare la adăugarea plății pentru " + student.getName(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
    private void showPaymentSummaryDialog(List<Student> selectedStudents, int daysPaid) {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_summary, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        RecyclerView dialogRecyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
        TextView totalPaymentTextView = dialogView.findViewById(R.id.totalPaymentTextView);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        // Adapter for dialog to display individual payments
        PaymentSummaryAdapter adapter = new PaymentSummaryAdapter(selectedStudents, daysPaid);
        dialogRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dialogRecyclerView.setAdapter(adapter);

        // Calculate total payment
        Long totalPayment = 0L;
        for (Student student : selectedStudents) {
            Long restante = student.getRestante(); // Fetch from model
            totalPayment += 18 * (daysPaid - restante);
        }
        totalPaymentTextView.setText("Total Plata: " + totalPayment + " Lei");

        // Show dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle OK button
        confirmButton.setOnClickListener(v -> {
            // Update database for all selected students
            for (Student student : selectedStudents) {
                Long restante = student.getRestante();


                // Add payment to Firestore
                db.collection("students").document(student.getId())
                        .update("daysPaid", daysPaid, "restante", 0, "dateRestante", null)
                        .addOnSuccessListener(aVoid -> Log.d("Payment", "Updated successfully for " + student.getName()))
                        .addOnFailureListener(e -> Log.w("Payment", "Error updating payment", e));
            }

            Toast.makeText(this, "Plata a fost adaugată!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

}