package com.example.cantinaonline;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StudentActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView studentNameTextView;
    private TextView daysPaidTextView;
    private ImageView qrCodeImageView;
    private Button datePickerButton, saveButton;
    private TextView selectedDateText;
    String studentId;
    private String selectedDate; // Store the selected date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        db = FirebaseFirestore.getInstance();
        studentNameTextView = findViewById(R.id.studentNameTextView);
        daysPaidTextView = findViewById(R.id.daysPaidTextView);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);
        datePickerButton = findViewById(R.id.datePickerButton);
        selectedDateText = findViewById(R.id.selectedDateText);
        saveButton = findViewById(R.id.saveButton);

        // Set listener for the Date Picker button


        Intent intent = getIntent();
        String enteredPassword = intent.getStringExtra("enteredPassword");

        if (enteredPassword != null) {
            fetchStudentData(enteredPassword);

        } else {
            Log.e("StudentActivity", "No password received from MainActivity.");
        }

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


        // Căutăm elevul în baza de date
        db.collection("students")
                .whereEqualTo("studentId", studentId)
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
    private void fetchStudentData(String enteredPassword) {
        db.collection("students")
                .whereEqualTo("parola", enteredPassword)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Extract data from document
                                String name = document.getString("name");

                                studentId = document.getString("studentId");

                                // Display data
                                studentNameTextView.setText(name);
                                daysPaidTextView.setText("Zile platite: "+ document.get("daysPaid",Integer.class).toString());

                                // Generate QR Code
                                generateQRCode(studentId);
                            }
                        } else {
                            Log.d("StudentActivity", "No matching student found for the entered password.");
                        }
                    } else {
                        Log.w("StudentActivity", "Error fetching student data.", task.getException());
                    }
                });
    }
    private void generateQRCode(String studentId) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(studentId, BarcodeFormat.QR_CODE, 400, 400);
            qrCodeImageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Log.e("StudentActivity", "Error generating QR Code", e);
        }
    }
    private void clearFields() {

        selectedDateText.setText("Data selectată: ");
        selectedDate = null;
    }

}