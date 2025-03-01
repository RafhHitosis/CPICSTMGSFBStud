package com.example.icstmgsfbstud;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import okhttp3.Address;

public class profileFragment extends Fragment {

    private ImageView profileImageView;
    private TextView nameTextView, studentNumberTextView, courseTextView, cardAddressTextView;
    private SharedPreferences sharedPreferences;
    private RelativeLayout changepass, logout;
    private DatabaseReference userRef, userRefImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileImageView = view.findViewById(R.id.profileImageView);
        nameTextView = view.findViewById(R.id.nameTextView);
        studentNumberTextView = view.findViewById(R.id.studentNumberTextView);
        courseTextView = view.findViewById(R.id.courseTextView);
        cardAddressTextView = view.findViewById(R.id.cardAddressTextView);
        changepass = view.findViewById(R.id.changePass);
        logout = view.findViewById(R.id.logoutBtn);

        sharedPreferences = getActivity().getSharedPreferences("StudentPrefs", getContext().MODE_PRIVATE);
        String studentNumber = sharedPreferences.getString("studentNumber", "");
        String name = sharedPreferences.getString("name", "");
        String course = sharedPreferences.getString("course", "");
        String address = sharedPreferences.getString("address", "");
        String imageUrl = sharedPreferences.getString("image", "");

        studentNumberTextView.setText("Student Number: " + studentNumber);
        nameTextView.setText(name);
        cardAddressTextView.setText("Address: " + address);
        courseTextView.setText("Course: " + course);

        // Firebase reference for the current student
        userRefImage = FirebaseDatabase.getInstance().getReference("image").child(studentNumber);

        // Load the profile image using Picasso
        loadImageFromFirebase(studentNumber);

        // Firebase reference for the current student
        userRef = FirebaseDatabase.getInstance().getReference("normalLogin").child("students").child(studentNumber);

        changepass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangePasswordDialog(studentNumber);
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutConfirmationDialog(); // Show confirmation dialog
            }
        });

        return view;
    }

    // Add this new method to the profileFragment class
    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Clear SharedPreferences
                    SharedPreferences sharedPreferences = getActivity().getSharedPreferences("StudentPrefs", getActivity().MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear(); // Clear all data from SharedPreferences
                    editor.apply(); // Apply changes

                    // Perform logout action and navigate to LoginActivity
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                    getActivity().finish(); // Finish the current activity
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(false) // Prevent dismissing by tapping outside the dialog
                .show();
    }


    private void loadImageFromFirebase(String studentNumber) {
        // Query the Firebase database for the image URL using the student number
        userRefImage.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Retrieve the image URL from Firebase
                String imageUrl = dataSnapshot.getValue(String.class);

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // Load the image into the ImageView using Picasso
                    Picasso.get().load(imageUrl)
                            .placeholder(R.drawable.ic_baseline_person_24)
                            .error(R.drawable.ic_baseline_person_24)
                            .into(profileImageView);
                } else {
                    // Handle if the image URL is missing or empty
                    Toast.makeText(getContext(), "No image found for this student.", Toast.LENGTH_SHORT).show();
//                    profileImageView.setImageResource(R.drawable.picrd);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle any errors while retrieving the image
                Toast.makeText(getContext(), "Failed to load image: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showChangePasswordDialog(String studentNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        EditText oldPasswordEditText = dialogView.findViewById(R.id.oldPasswordEditText);
        EditText newPasswordEditText = dialogView.findViewById(R.id.newPasswordEditText);
        EditText confirmNewPasswordEditText = dialogView.findViewById(R.id.confirmNewPasswordEditText);

        // Define the password pattern
        String passwordPattern = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@#$%&]).{7,}$";

        builder.setTitle("Change Password")
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(view -> {
                String oldPassword = oldPasswordEditText.getText().toString().trim();
                String newPassword = newPasswordEditText.getText().toString().trim();
                String confirmNewPassword = confirmNewPasswordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(oldPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmNewPassword)) {
                    Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                } else if (!newPassword.equals(confirmNewPassword)) {
                    Toast.makeText(getContext(), "New passwords do not match", Toast.LENGTH_SHORT).show();
                } else if (!newPassword.matches(passwordPattern)) {
                    Toast.makeText(getContext(), "Password must be at least 7 characters long and consist of letters, numbers, and @#$%& symbols", Toast.LENGTH_SHORT).show();
                } else {
                    validateOldPasswordAndChange(oldPassword, newPassword, studentNumber, dialog);
                }
            });
        });

        dialog.show();
    }


    private void validateOldPasswordAndChange(String oldPassword, String newPassword, String studentNumber, AlertDialog dialog) {
        // Show loading dialog
        ProgressDialog loadingDialog = new ProgressDialog(getContext());
        loadingDialog.setMessage("Changing password...");
        loadingDialog.setCancelable(false); // Prevent dialog from being dismissed by tapping outside
        loadingDialog.show();

        // Fetch the student's current password from the database
        userRef.child("password").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentPassword = snapshot.getValue(String.class);

                if (currentPassword != null && currentPassword.equals(oldPassword)) {
                    // Old password is correct, update to new password
                    userRef.child("password").setValue(newPassword).addOnCompleteListener(task -> {
                        loadingDialog.dismiss(); // Dismiss loading dialog

                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(getContext(), "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    loadingDialog.dismiss(); // Dismiss loading dialog
                    Toast.makeText(getContext(), "Old password is incorrect", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadingDialog.dismiss(); // Dismiss loading dialog
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
