package com.example.cantinaonline;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {


    FirebaseFirestore db = FirebaseFirestore.getInstance();
    boolean adminAutenficat=false;
    private EditText passwordInput;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredPassword = passwordInput.getText().toString();

                if (enteredPassword.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Introduceți parola", Toast.LENGTH_SHORT).show();
                } else {
                    // Apelează funcția pentru autentificare cu parola introdusă
                    loginWithPasswordAndAdminStatus(enteredPassword);
                    if(adminAutenficat){
                        Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                        startActivity(intent);
                    }
                }
            }
        });
    }
    public void loginWithPasswordAndAdminStatus(String enteredPassword) {
        // Realizează o interogare pentru a găsi documentele care au atât `password` cât și `isAdmin=true`
        db.collection("admins")
                .whereEqualTo("password", enteredPassword)
                .whereEqualTo("isAdmin", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Verifică dacă există documente care îndeplinesc criteriile
                        if (!task.getResult().isEmpty()) {
                            Log.d("Login", "Autentificare admin reușită.");
                            adminAutenficat=true;
                        } else {
                            Log.d("Login", "Parola este incorectă sau utilizatorul nu este admin.");
                        }
                    } else {
                        Log.w("Login", "Eroare la accesarea bazei de date.", task.getException());
                    }
                });
    }
}