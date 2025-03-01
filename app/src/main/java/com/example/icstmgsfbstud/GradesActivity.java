package com.example.icstmgsfbstud;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GradesActivity extends AppCompatActivity {

    private TextView gradesTextView;
    private FirebaseDatabase database;
    private DatabaseReference subjectsRef;
    private SharedPreferences sharedPreferences;
    private String studentNumber;
    private String subjectKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grades);

        gradesTextView = findViewById(R.id.gradesTextView);

        sharedPreferences = getSharedPreferences("StudentPrefs", MODE_PRIVATE);
        studentNumber = sharedPreferences.getString("studentNumber", "");
        subjectKey = getIntent().getStringExtra("subjectKey");

        database = FirebaseDatabase.getInstance();
        subjectsRef = database.getReference("subjects");

        loadGrades();
    }

    private void loadGrades() {
        subjectsRef.child(subjectKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder gradesBuilder = new StringBuilder();

                for (DataSnapshot subjectSnapshot : snapshot.getChildren()) {
                    // Retrieve the instructor for the current subjectKey
                    String instructor = subjectSnapshot.child("instructor").getValue(String.class);

                    // Loop through each student enrolled in the subject
                    for (DataSnapshot studentSnapshot : subjectSnapshot.child("studentEnrolled").getChildren()) {
                        if (studentSnapshot.getKey().equals(studentNumber)) {
//                            String name = studentSnapshot.child("name").getValue(String.class);
//                            double finalGrade = studentSnapshot.child("final").getValue(Double.class);
//                            double midtermGrade = studentSnapshot.child("midterm").getValue(Double.class);
                            double total = studentSnapshot.child("total").getValue(Double.class);
                            String equivalentStr = studentSnapshot.child("equivalent").getValue(String.class);
                            double equivalent = getDoubleValue(equivalentStr);
                            String remarks = studentSnapshot.child("remarks").getValue(String.class);

                            // Format the numerical values to 2 decimal places
                            String formattedEquivalent = String.format("%.2f", equivalent);

                            gradesBuilder.append("Subject: ").append(subjectSnapshot.getKey()).append("\n")
                                    .append("Description: ").append(subjectSnapshot.child("description").getValue(String.class)).append("\n")
                                    .append("Instructor: ").append(instructor).append("\n")
//                                    .append("Name: ").append(name).append("\n")
//                                    .append("Final: ").append(finalGrade).append("\n")
//                                    .append("Midterm: ").append(midtermGrade).append("\n")
                                    .append("Grade: ").append(total).append("\n")
                                    .append("Equivalent: ").append(formattedEquivalent).append("\n")
                                    .append("Remarks: ").append(remarks).append("\n\n");
                        }
                    }
                }
                gradesTextView.setText(gradesBuilder.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GradesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private double getDoubleValue(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                // Handle the exception (e.g., log the error or set a default value)
                return 0.0;
            }
        }
        return 0.0; // Default value if not a Double or String
    }
}
