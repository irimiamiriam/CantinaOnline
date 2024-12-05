package com.example.cantinaonline;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AddStudentActivity extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private EditText nameField, passwordField, daysPaidField;
    private TextView userIdDisplay;
    private ImageView qrCodeImage;
    private Button generateUserIdButton, saveButton, printButton;
    private String id; // Stores the generated User ID
    Bitmap qrCodeBitmap;
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
        printButton = findViewById(R.id.printButton);

        printButton.setOnClickListener(v->printQRCode());
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
             qrCodeBitmap = barcodeEncoder.encodeBitmap(data, BarcodeFormat.QR_CODE, 200, 200);
            qrCodeImage.setImageBitmap(qrCodeBitmap);
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
    }
    private void printQRCode() {
        if (qrCodeBitmap == null) {
            Toast.makeText(this, "QR Code not available for printing.", Toast.LENGTH_SHORT).show();
            return;
        }

        PrintManager printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);

        printManager.print("QR Code Print", new PrintDocumentAdapter() {

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
                // Initialize print attributes with default values
                PrintAttributes printAttributes = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(new PrintAttributes.Resolution("QR_Code", "QR Code", 300, 300))
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS) // Ensure margins are set
                        .build();

                PrintedPdfDocument pdfDocument = new PrintedPdfDocument(AddStudentActivity.this, printAttributes);

                PdfDocument.Page page = pdfDocument.startPage(0);

                if (cancellationSignal.isCanceled()) {
                    pdfDocument.close();
                    callback.onWriteCancelled();
                    return;
                }

                Canvas canvas = page.getCanvas();
                float pageWidth = canvas.getWidth();
                float pageHeight = canvas.getHeight();

                // Scale and center the bitmap
                // Set a smaller scale factor to reduce the size of the QR code on the page
                float desiredSize = 150; // Adjust this value to control the size of the QR code in pixels
                float scale = Math.min(desiredSize / qrCodeBitmap.getWidth(), desiredSize / qrCodeBitmap.getHeight());

                // Calculate the position to center the QR code on the page
                float scaledWidth = qrCodeBitmap.getWidth() * scale;
                float scaledHeight = qrCodeBitmap.getHeight() * scale;
                float left = (pageWidth - scaledWidth) / 2;
                float top = (pageHeight - scaledHeight) / 2;

                canvas.drawBitmap(qrCodeBitmap, null, new RectF(left, top, left + scaledWidth, top + scaledHeight), null);
                pdfDocument.finishPage(page);

                try {
                    pdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.onWriteFailed(e.getMessage());
                } finally {
                    pdfDocument.close();
                }

                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            }

            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }

                PrintDocumentInfo info = new PrintDocumentInfo.Builder("QRCode.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1)
                        .build();
                callback.onLayoutFinished(info, true);
            }
        }, null);
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