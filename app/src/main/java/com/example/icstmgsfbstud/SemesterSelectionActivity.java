package com.example.icstmgsfbstud;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SemesterSelectionActivity extends AppCompatActivity {

    private Spinner semesterSpinner, academicYearSpinner;
    private Button continueButton;
    private FirebaseDatabase database;
    private DatabaseReference studentsRef;
    private SharedPreferences sharedPreferences;
    private String studentNumber;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_semester_selection);

        semesterSpinner = findViewById(R.id.semesterSpinner);
        academicYearSpinner = findViewById(R.id.academicYearSpinner);
        continueButton = findViewById(R.id.continueButton);

        sharedPreferences = getSharedPreferences("StudentPrefs", MODE_PRIVATE);
        studentNumber = sharedPreferences.getString("studentNumber", "");
        database = FirebaseDatabase.getInstance();
        studentsRef = database.getReference("normalLogin/students");

        loadSemestersAndYears();

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String selectedSemester = semesterSpinner.getSelectedItem() != null
                        ? semesterSpinner.getSelectedItem().toString() : "";
                String selectedAcademicYear = academicYearSpinner.getSelectedItem() != null
                        ? academicYearSpinner.getSelectedItem().toString() : "";

                // Check if any selection is missing
                if (selectedSemester.isEmpty() || selectedAcademicYear.isEmpty()) {
                    showErrorDialog();
                } else {
                    // Show loading dialog when loading grades
                    showLoadingDialog();
                    loadGrades(selectedSemester, selectedAcademicYear);
                }
            }
        });
    }

    private void showErrorDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Selection Error")
                .setMessage("Please select both a semester and an academic year.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(SemesterSelectionActivity.this);
        progressDialog.setMessage("Loading, please wait...");
        progressDialog.setCancelable(false); // Prevent user from dismissing the dialog manually
        progressDialog.show();
    }

    private void dismissLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void loadSemestersAndYears() {
        studentsRef.child(studentNumber).child("enrolledIn").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> semesters = new ArrayList<>();
                List<String> academicYears = new ArrayList<>();

                for (DataSnapshot semesterSnapshot : snapshot.getChildren()) {
                    semesters.add(semesterSnapshot.getKey());
                    for (DataSnapshot yearSnapshot : semesterSnapshot.getChildren()) {
                        academicYears.add(yearSnapshot.getKey());
                    }
                }

                ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(SemesterSelectionActivity.this, android.R.layout.simple_spinner_item, semesters);
                semesterSpinner.setAdapter(semesterAdapter);

                ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(SemesterSelectionActivity.this, android.R.layout.simple_spinner_item, academicYears);
                academicYearSpinner.setAdapter(yearAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SemesterSelectionActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGrades(final String semester, final String academicYear) {
        studentsRef.child(studentNumber).child("enrolledIn").child(semester).child(academicYear)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        dismissLoadingDialog();

                        String locGrades = snapshot.child("locGrades").getValue(String.class);
                        if (locGrades != null) {
                            Intent intent = new Intent(SemesterSelectionActivity.this, MainActivity.class);
                            intent.putExtra("gradesKey", locGrades);
                            startActivity(intent);
                        } else {
                            Toast.makeText(SemesterSelectionActivity.this, "No Grades Found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        dismissLoadingDialog();
                        Toast.makeText(SemesterSelectionActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
