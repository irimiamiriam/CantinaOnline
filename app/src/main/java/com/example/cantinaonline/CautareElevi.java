package com.example.cantinaonline;

import android.os.Bundle;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cantinaonline.R;
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
        payButton.setOnClickListener(v -> addPaymentsToSelectedStudents());
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
                            studentsList.add(new Student(id, name, daysPaid != null ? daysPaid : 0));
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
}