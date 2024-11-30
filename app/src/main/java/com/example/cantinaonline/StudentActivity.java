package com.example.cantinaonline;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class StudentActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView studentNameTextView;
    private TextView daysPaidTextView;
    private ImageView qrCodeImageView;

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

        Intent intent = getIntent();
        String enteredPassword = intent.getStringExtra("enteredPassword");

        if (enteredPassword != null) {
            fetchStudentData(enteredPassword);
        } else {
            Log.e("StudentActivity", "No password received from MainActivity.");
        }
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
                                Long daysPaid = document.getLong("daysPaid");
                                String studentId = document.getString("studentId");

                                // Display data
                                studentNameTextView.setText(name);
                                daysPaidTextView.setText("Zile plătite: " + daysPaid);

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

}