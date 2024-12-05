package com.example.cantinaonline.ui.theme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cantinaonline.PortraitCaptureActivity;
import com.example.cantinaonline.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Calendar;
import java.util.Date;

public class AdministratorActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_administrator);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        onPaidUntilButtonClicked();
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
                finish();
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
                        ShowUser(docId);
                        // Prompt the admin to enter the new `daysPaid` value

                    } else {
                        Toast.makeText(this, "Id neinregistrat", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void ShowUser(String id){
        db.collection("students").document(id)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        if (document.exists()) {
                            // Obține datele existente ale studentului
                            Long daysPaid = document.getLong("daysPaid");
                            String lastScan = document.getString("lastScan");

                            Date today = new Date(); // Data curentă
                            String todayStr = android.text.format.DateFormat.format("yyyy-MM-dd", today).toString();

                            if (daysPaid != null && daysPaid > 0) {
                                if (lastScan == null || !lastScan.equals(todayStr)) {
                                    // Actualizăm `lastScan` și scădem `daysPaid`
                                    db.collection("students").document(id)
                                            .update("lastScan", todayStr, "daysPaid", daysPaid - 1)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Scanare reușită! Zile plătite actualizate.", Toast.LENGTH_SHORT).show();
                                                onPaidUntilButtonClicked();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Eroare la actualizarea datelor.", Toast.LENGTH_SHORT).show();onPaidUntilButtonClicked();
                                            });
                                } else {
                                    Toast.makeText(this, "Studentul a fost deja scanat astăzi.", Toast.LENGTH_SHORT).show();onPaidUntilButtonClicked();
                                }
                            } else {
                                Toast.makeText(this, "Studentul nu mai are zile plătite disponibile.", Toast.LENGTH_SHORT).show();onPaidUntilButtonClicked();
                            }
                        } else {
                            Toast.makeText(this, "Documentul studentului nu există.", Toast.LENGTH_SHORT).show();onPaidUntilButtonClicked();
                        }
                    } else {
                        Toast.makeText(this, "Eroare la accesarea bazei de date.", Toast.LENGTH_SHORT).show();onPaidUntilButtonClicked();
                    }
                });
    }

}