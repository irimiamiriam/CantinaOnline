package com.example.cantinaonline;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AddStudentActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText nameField, passwordField, daysPaidField;
    private TextView userIdDisplay;
    private ImageView qrCodeImage;
    private Button generateUserIdButton, saveButton;
    private String id; // Stores the generated User ID
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_student);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        nameField = findViewById(R.id.nameField);
        passwordField = findViewById(R.id.passwordField);
        daysPaidField = findViewById(R.id.daysPaidField);
        userIdDisplay = findViewById(R.id.userIdDisplay);
        qrCodeImage = findViewById(R.id.qrCodeImage);
        saveButton = findViewById(R.id.saveButton);

        generateUniqueUserId(new Callback() {
            @Override
            public void onSuccess(String userId) {
                if (userId != null) {
                    // Use the generated unique userId
                    Log.d("Generated UserId", userId);
                    // You can now use this userId to save the student to Firestore
                    generateQRCode(userId);
                    id=userId;
                    userIdDisplay.setText("User ID : "+ userId);

                } else {
                    // Handle error if userId generation failed
                    Log.e("Error", "Failed to generate unique user ID");
                }
            }
            @Override
            public void onFailure(Exception e) {
                // Handle failure
                Log.e("Error", "Firestore query failed", e);
            }
        });
        generateUniquePassword(new PasswordCallback() {
            @Override
            public void onSuccess(String password) {
                Log.d("Generated Password", password);

                // Afișează parola sau folosește-o pentru a crea un nou utilizator
                passwordField.setText(password);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("Error", "Failed to generate unique password", e);
                Toast.makeText(getApplicationContext(), "Error generating password", Toast.LENGTH_SHORT).show();
            }
        });
        saveButton.setOnClickListener(v -> saveUserDetails());

    }
    private void generateQRCode(String data) {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try {
            Bitmap bitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 200, 200);
            qrCodeImage.setImageBitmap(bitmap);
            qrCodeImage.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR Code", Toast.LENGTH_SHORT).show();
        }
    }
    private void generateUniqueUserId(Callback callback) {
        Random random = new Random();
        int randomId = 10000 + random.nextInt(90000); // Generate a 5-digit number
        String userId = String.valueOf(randomId);

        // Check uniqueness in Firestore
        db.collection("students").whereEqualTo("studentId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot.isEmpty()) {
                            // Unique ID found
                            callback.onSuccess(userId);
                        } else {
                            // ID exists; retry
                            generateUniqueUserId(callback);
                        }
                    } else {
                        // Handle error
                        callback.onFailure(task.getException());
                    }
                });
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

    private void generateUniquePassword(PasswordCallback callback) {
        String password = generateRandomPassword();

        // Verificare în Firestore
        db.collection("students").whereEqualTo("parola", password)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // Parola este unică
                            callback.onSuccess(password);
                        } else {
                            // Parola există deja, încearcă din nou
                            generateUniquePassword(callback);
                        }
                    } else {
                        // Eroare la verificarea unicității
                        callback.onFailure(task.getException());
                    }
                });
    }


    private void saveUserDetails() {
        String name = nameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String daysPaid = daysPaidField.getText().toString().trim();

        if (name.isEmpty() || password.isEmpty() || daysPaid.isEmpty() || id== null) {
            Toast.makeText(this, "Please fill all fields and generate a User ID", Toast.LENGTH_SHORT).show();
            return;
        }


         Map<String, Object> student = new HashMap<>();
         student.put("name", name);
         student.put("parola", password);
         student.put("daysPaid", Integer.parseInt(daysPaid));
         student.put("studentId", id);
         db.collection("students").document(id).set(student);

        Toast.makeText(this, "User details saved successfully!", Toast.LENGTH_SHORT).show();

        // Clear fields for the next input
        nameField.setText("");
        passwordField.setText("");
        daysPaidField.setText("");
        userIdDisplay.setText("User ID: Not Generated");
        qrCodeImage.setVisibility(View.INVISIBLE);
    }

    interface Callback {
        void onSuccess(String userId);
        void onFailure(Exception e);
    }

    interface PasswordCallback {
        void onSuccess(String password);
        void onFailure(Exception e);
    }
}