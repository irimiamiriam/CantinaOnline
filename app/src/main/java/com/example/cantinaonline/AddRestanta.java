package com.example.cantinaonline;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRestanta extends AppCompatActivity {

    private EditText studentNameInput;
    private Button datePickerButton, saveButton;
    private TextView selectedDateText;
    private FirebaseFirestore db;
    private String selectedDate; // Store the selected date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_restanta);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        studentNameInput = findViewById(R.id.studentNameInput);
        datePickerButton = findViewById(R.id.datePickerButton);
        selectedDateText = findViewById(R.id.selectedDateText);
        saveButton = findViewById(R.id.saveButton);

        // Set listener for the Date Picker button
        datePickerButton.setOnClickListener(v -> showDatePickerDialog());

        // Set listener for the Save button
        saveButton.setOnClickListener(v -> saveStudentData());
    }

    private void showDatePickerDialog() {
        // Get current date
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create a DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    // Format the selected date as YYYY-MM-DD
                    selectedDate = year1 + "-" + (month1 + 1) + "-" + dayOfMonth;
                    selectedDateText.setText("Data selectată: " + selectedDate);
                }, year, month, day);

        datePickerDialog.show();
    }

    private void saveStudentData() {
        String studentName = studentNameInput.getText().toString().trim();

        // Validare introducere date
        if (TextUtils.isEmpty(studentName)) {
            Toast.makeText(this, "Introduceți numele elevului.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(selectedDate)) {
            Toast.makeText(this, "Selectați data restanței.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Căutăm elevul în baza de date
        db.collection("students")
                .whereEqualTo("name", studentName)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Elev găsit
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String docId = document.getId();

                        // Actualizăm lista de restanțe
                        updateStudentRestanta(docId);
                    } else {
                        // Elevul nu există
                        Toast.makeText(this, "Elevul nu există în baza de date.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la accesarea bazei de date.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateStudentRestanta(String docId) {
        // Obținem documentul elevului
        db.collection("students").document(docId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Obținem lista de restanțe existentă
                        List<String> restanteDates = (List<String>) document.get("dateRestante");
                        if (restanteDates == null) {
                            restanteDates = new ArrayList<>();
                        }

                        // Obținem numărul actual de restanțe
                        Long restanteCount = document.getLong("restante");
                        if (restanteCount == null) {
                            restanteCount = 0L;
                        }

                        // Adăugăm noua dată în lista de restanțe
                        restanteDates.add(selectedDate);

                        // Incrementăm numărul de restanțe
                        restanteCount++;

                        // Actualizăm documentul cu noua listă și numărul de restanțe
                        db.collection("students").document(docId)
                                .update(
                                        "dateRestante", restanteDates,
                                        "restante", restanteCount
                                )
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Restanță adăugată cu succes.", Toast.LENGTH_SHORT).show();
                                    clearFields();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Eroare la actualizarea datelor.", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la accesarea bazei de date.", Toast.LENGTH_SHORT).show();
                });
    }


    private void clearFields() {
        studentNameInput.setText("");
        selectedDateText.setText("Data selectată: ");
        selectedDate = null;
    }
}