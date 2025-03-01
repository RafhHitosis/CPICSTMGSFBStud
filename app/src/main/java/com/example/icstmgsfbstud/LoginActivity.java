package com.example.icstmgsfbstud;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import es.dmoral.toasty.Toasty;

public class LoginActivity extends AppCompatActivity {

    private EditText studentNumberEditText, passwordEditText;
    private Button loginButton, qrLoginButton;
    private FirebaseDatabase database;
    private DatabaseReference studentsRef;
    private SharedPreferences sharedPreferences;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("StudentPrefs", MODE_PRIVATE);
        if (isUserLoggedIn()) {
            // If user is already logged in, redirect to SemesterSelectionActivity
            Intent intent = new Intent(LoginActivity.this, SemesterSelectionActivity.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_login);
        studentNumberEditText = findViewById(R.id.studentNumberEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        qrLoginButton = findViewById(R.id.qrLoginButton);

        database = FirebaseDatabase.getInstance();
        studentsRef = database.getReference("normalLogin/students");

        loginButton.setOnClickListener(v -> {
            String studentNumber = studentNumberEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            // Show loading dialog when login process starts
            showLoadingDialog();
            authenticateUser(studentNumber, password);
        });

        qrLoginButton.setOnClickListener(v -> startQRLogin());
    }

    private boolean isUserLoggedIn() {
        return sharedPreferences.contains("studentNumber");
    }

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("Logging in, please wait...");
        progressDialog.setCancelable(false); // Prevent user from dismissing the dialog manually
        progressDialog.show();
    }

    private void dismissLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void startQRLogin() {
        IntentIntegrator integrator = new IntentIntegrator(LoginActivity.this);
        integrator.setPrompt("Scan QR Code");
        integrator.setOrientationLocked(true);
        integrator.setBeepEnabled(true);
        integrator.setCaptureActivity(CustomScannerActivity.class);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                String qrData = result.getContents();
                String[] qrParts = qrData.split(",");
                String studentNumber = qrParts[0].replace("\"", "").trim();

                // Show loading dialog for QR login
                showLoadingDialog();

                authenticateQRUser(studentNumber);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void authenticateUser(final String studentNumber, final String password) {
        studentsRef.child(studentNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dismissLoadingDialog(); // Dismiss loading dialog when done

                if (snapshot.exists()) {
                    String storedPassword = snapshot.child("password").getValue(String.class);
                    if (storedPassword != null && storedPassword.equals(password)) {
                        saveStudentDataInPreferences(snapshot);
                        sharedPreferences.edit().putBoolean("isLoggedIn", true).apply();

                        Intent intent = new Intent(LoginActivity.this, SemesterSelectionActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toasty.warning(getApplicationContext(), "Invalid Password", Toast.LENGTH_SHORT, true).show();
                    }
                } else {
                    Toasty.warning(getApplicationContext(), "Student Not Found", Toast.LENGTH_SHORT, true).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dismissLoadingDialog(); // Dismiss loading dialog on error

                Toasty.error(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT, true).show();
            }
        });
    }

    private void authenticateQRUser(final String scannedStudentNumber) {
        DatabaseReference qrLoginRef = database.getReference("qrLogin/students");
        qrLoginRef.child(scannedStudentNumber).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dismissLoadingDialog(); // Dismiss loading dialog when done

                if (snapshot.exists()) {
                    String matchedStudentNumber = snapshot.getValue(String.class);
                    if (matchedStudentNumber != null) {
                        studentsRef.child(matchedStudentNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot studentSnapshot) {
                                dismissLoadingDialog(); // Dismiss loading dialog after QR login

                                if (studentSnapshot.exists()) {
                                    saveStudentDataInPreferences(studentSnapshot);
                                    sharedPreferences.edit().putBoolean("isLoggedIn", true).apply();

                                    Intent intent = new Intent(LoginActivity.this, SemesterSelectionActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toasty.warning(getApplicationContext(), "Student Not Found in Normal Login", Toast.LENGTH_SHORT, true).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                dismissLoadingDialog(); // Dismiss loading dialog on error
                                Toasty.error(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT, true).show();
                            }
                        });
                    }
                } else {
                    Toasty.warning(getApplicationContext(), "Invalid QR Code or Student Not Found in QR Login", Toast.LENGTH_SHORT, true).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dismissLoadingDialog(); // Dismiss loading dialog on error
                Toasty.error(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT, true).show();
            }
        });
    }

    private void saveStudentDataInPreferences(DataSnapshot snapshot) {
        sharedPreferences = getSharedPreferences("StudentPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("studentNumber", snapshot.getKey());
        editor.putString("course", snapshot.child("course").getValue(String.class));
        editor.putString("name", snapshot.child("name").getValue(String.class));
        editor.putString("address", snapshot.child("address").getValue(String.class));
        editor.putString("image", snapshot.child("image").getValue(String.class));
        editor.apply();
    }
}
