package com.example.icstmgsfbstud;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class viewGradeFragment extends Fragment {

    private LinearLayout gradesContainer;
    private TextView tvNoGrades;
    private FirebaseDatabase database;
    private DatabaseReference gradesRef; // Reference to grades node
    private DatabaseReference subjectsRef; // Reference to subjects node
    private SharedPreferences sharedPreferences;
    private String studentNumber;
    private String gradesKey;
    private static final String HAS_SEEN_INSTRUCTION = "hasSeenInstruction";
    private ValueEventListener gradesListener; // Store the listener

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_view_grade, container, false);

        gradesContainer = view.findViewById(R.id.gradesContainer);
        tvNoGrades = view.findViewById(R.id.tvNoGrades); // Initialize TextView

        // Get SharedPreferences
        sharedPreferences = getActivity().getSharedPreferences("StudentPrefs", getContext().MODE_PRIVATE);
        studentNumber = sharedPreferences.getString("studentNumber", "");
        gradesKey = getActivity().getIntent().getStringExtra("gradesKey"); // Changed to gradesKey

        database = FirebaseDatabase.getInstance();
        gradesRef = database.getReference("grades").child(gradesKey); // Reference to grades using the passed key
        subjectsRef = database.getReference("subjects"); // Reference to subjects node

        // Initialize and attach the listener
        setupGradesListener();

        // Show the instruction pop-up dialog if it's the first time for this user
        if (!hasSeenInstruction()) {
            showInstructionDialog();
        }

        return view;
    }

    private void setupGradesListener() {
        // Remove any existing listener first
        if (gradesListener != null) {
            gradesRef.removeEventListener(gradesListener);
        }

        // Create new listener
        gradesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                gradesContainer.removeAllViews();
                boolean hasGrades = false;

                LayoutInflater inflater = LayoutInflater.from(requireContext());

                if (snapshot.hasChild("data")) {
                    for (DataSnapshot gradeSnapshot : snapshot.child("data").getChildren()) {
                        String studnum = gradeSnapshot.child("studnum").getValue(String.class);

                        if (studentNumber.equals(studnum)) {
                            hasGrades = true;

                            String subjectId = gradeSnapshot.child("id").getValue(String.class);
                            String equivalentStr = gradeSnapshot.child("equivalent").getValue(String.class);
                            String remarks = gradeSnapshot.child("remarks").getValue(String.class);
                            Object finalGradeObj = gradeSnapshot.child("finalgrade").getValue();

                            // Format the equivalent and final grade to two decimal places
                            String equivalent = String.format("%.2f", getDoubleValue(equivalentStr));
                            String finalGrade = finalGradeObj != null ? String.format("%.2f", getDoubleValue(finalGradeObj)) : "N/A";

                            View cardView = inflater.inflate(R.layout.item_grade, gradesContainer, false);

                            ((TextView) cardView.findViewById(R.id.tvSubject)).setText("Subject ID: " + subjectId);
                            ((TextView) cardView.findViewById(R.id.tvGrade)).setText("Final Grade: " + finalGrade);
                            ((TextView) cardView.findViewById(R.id.tvEquivalent)).setText("Equivalent: " + equivalent);
                            ((TextView) cardView.findViewById(R.id.tvRemarks)).setText("Remarks: " + remarks);

                            fetchSubjectDetails(subjectId, cardView);

                            gradesContainer.addView(cardView);
                        }
                    }
                }

                tvNoGrades.setVisibility(hasGrades ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        // Attach the listener
        gradesRef.addValueEventListener(gradesListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the listener when the fragment is destroyed
        if (gradesListener != null) {
            gradesRef.removeEventListener(gradesListener);
        }
    }

    private void fetchSubjectDetails(String subjectId, View cardView) {
        // Navigate through the subjects node
        subjectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean subjectFound = false;

                // Iterate through the semesters and subjects
                for (DataSnapshot semesterSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot subjectSnapshot : semesterSnapshot.getChildren()) {
                        String id = subjectSnapshot.child("id").getValue(String.class);

                        // Check if the ID matches the subject ID from the grades
                        if (subjectId.equals(id)) {
                            String description = subjectSnapshot.child("description").getValue(String.class);
                            String instructor = subjectSnapshot.child("instructor").getValue(String.class);

                            // Update the UI with fetched subject details
                            ((TextView) cardView.findViewById(R.id.tvDescription)).setText("Description: " + description);
                            ((TextView) cardView.findViewById(R.id.tvInstructor)).setText("Instructor: " + instructor);
                            subjectFound = true;
                            break; // No need to search further
                        }
                    }
                    if (subjectFound) break; // Break outer loop if subject found
                }

                // Handle case where no subject is found
                if (!subjectFound) {
                    ((TextView) cardView.findViewById(R.id.tvDescription)).setText("Description: Not available");
                    ((TextView) cardView.findViewById(R.id.tvInstructor)).setText("Instructor: Not available");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                return 0.0;
            }
        }
        return 0.0;
    }

    private void showInstructionDialog() {
        // Create the AlertDialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Instructions");
        builder.setMessage("You can view your grades in this page. If you want to change your password or logout, you can go to the profile page.");

        // Set the Close button
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Mark the user as having seen the instruction
                markInstructionAsSeen();
                dialog.dismiss(); // Close the dialog when the button is clicked
            }
        });

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Check if the user has already seen the instruction dialog
    private boolean hasSeenInstruction() {
        return sharedPreferences.getBoolean(HAS_SEEN_INSTRUCTION, false);
    }

    // Mark the instruction as seen in SharedPreferences
    private void markInstructionAsSeen() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(HAS_SEEN_INSTRUCTION, true);
        editor.apply();
    }
}
