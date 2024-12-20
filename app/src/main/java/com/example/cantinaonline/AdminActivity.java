package com.example.cantinaonline;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

public class AdminActivity extends AppCompatActivity {


    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    Button paid;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    Button restantaNoua = findViewById(R.id.addRestante);
        Button addStudent = findViewById(R.id.addStudentButton);
        Button cautareElev = findViewById(R.id.payButtonBySearch);

        paid = findViewById(R.id.payButton);
        addStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, AddStudentActivity.class);
                startActivity(intent);
            }
        });
        cautareElev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, CautareElevi.class);
                startActivity(intent);
            }
        });
        restantaNoua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, AddRestanta.class);
                startActivity(intent);
            }
        });
        paid.setOnClickListener(v->onPaidUntilButtonClicked());
    }
        public void onPaidUntilButtonClicked() {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setPrompt("Scan a QR code");
            integrator.setOrientationLocked(false);
            integrator.setCaptureActivity(PortraitCaptureActivity.class);
            integrator.initiateScan();
        }

        // Handle QR Code scan result
        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Scan canceled", Toast.LENGTH_LONG).show();
                } else {
                    // Get the scanned QR code content
                    String scannedId = result.getContents();
                    verifyAndUpdatePaidUntil(scannedId);
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        // Verify if the scanned ID exists in Firestore and update the `daysPaid` field if it does
        private void verifyAndUpdatePaidUntil(String scannedId) {
            db.collection("students").whereEqualTo("studentId", scannedId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // If a match is found, get the document ID and update `daysPaid`
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String docId = document.getId();

                            // Prompt the admin to enter the new `daysPaid` value
                            showDaysPaidDialog(docId);
                        } else {
                            Toast.makeText(this, "Id neinregistrat", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        // Show a dialog to enter the new `daysPaid` value
        private void showDaysPaidDialog(String docId) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(" Introduceti numarul de zile");

            final EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            builder.setView(input);

            builder.setPositiveButton("Update", (dialog, which) -> {
                int daysPaid;
                try {
                    daysPaid = Integer.parseInt(input.getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Introduceti un numar valid", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update `daysPaid` in Firestore
                db.collection("students").document(docId)
                        .update("daysPaid", daysPaid)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, " Noua plata cu succes!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Eroare in adaugarea platii", Toast.LENGTH_SHORT).show();
                        });
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        }

    }
