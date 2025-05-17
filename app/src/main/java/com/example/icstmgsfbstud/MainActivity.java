// Updated MainActivity.java with animation implementations

package com.example.icstmgsfbstud;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.icstmgsfbstud.databinding.ActivityMainBinding;
import com.example.icstmgsfbstud.helpers.BottomNavAnimationHelper;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements NotificationFragment.OnNotificationChangeListener {

    private static final String TAG = "MainActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 1;
    private static final String CHANNEL_ID = "grade"; // Channel ID for notifications

    private ActivityMainBinding binding;
    private DatabaseReference gradesRef;
    private DatabaseReference subjectsRef;
    private SharedPreferences sharedPreferences;
    private Map<String, String> lastKnownGrades = new HashMap<>();

    private BroadcastReceiver notificationReceiver;
    private int currentSelectedItem = 0; // Track the current selected item

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = getSharedPreferences("StudentPrefs", MODE_PRIVATE);

        // Check if user is logged in; if not, redirect to LoginActivity
        if (!isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        // Request notification permission and create notification channel
        requestNotificationPermission();
        createNotificationChannel();

        // Update FCM token after successful login
        updateFCMToken();

        // Initialize Firebase references and start listening for grade changes
        initializeFirebase();
        listenForGradeChanges();

        // Set the initial fragment as the viewGradeFragment
        replaceFragment(new viewGradeFragment());
        binding.bottomNavigationView.setBackground(null);

        // Apply bottom app bar enter animation
        BottomNavAnimationHelper.applyEnterAnimation(this, binding.bottomAppBar);

        // Apply layout animation to the bottom navigation
        binding.bottomNavigationView.setLayoutAnimation(
                AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_from_bottom));

        // Apply state animations to the navigation items
        BottomNavAnimationHelper.applyNavItemStateAnimator(binding.bottomNavigationView);

        // Set custom item color transition
        binding.bottomNavigationView.setItemIconTintList(
                ContextCompat.getColorStateList(this, R.color.nav_item_color_transition));
        binding.bottomNavigationView.setItemTextColor(
                ContextCompat.getColorStateList(this, R.color.nav_item_color_transition));

        // Apply initial elevation
        binding.bottomAppBar.setElevation(0f);

        // Sync badge count with stored notifications when the app starts
        updateNotificationBadge();

        // Setup navigation item click listener with animations
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int previousItem = currentSelectedItem;
            currentSelectedItem = item.getItemId();

            // Apply elevation animation
            BottomNavAnimationHelper.applyElevationAnimation(binding.bottomAppBar, 0f, 8f);

            // Create ripple effect on click
            applyRippleToMenuItem(item.getItemId());

            // Navigate to the selected fragment
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(new viewGradeFragment());
            } else if (itemId == R.id.profile) {
                replaceFragment(new profileFragment());
            } else if (itemId == R.id.notifications) {
                replaceFragment(new NotificationFragment());
                // Animate badge when notifications tab is selected
                BottomNavAnimationHelper.animateBadge(binding.bottomNavigationView, R.id.notifications);
            }
            return true;
        });

        // Register BroadcastReceiver to update UI on new notifications
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Notification received in MainActivity, updating fragment...");
                // Update the notification badge and fragment
                updateNotificationBadge();

                // Animate the notification badge
                BottomNavAnimationHelper.animateBadge(binding.bottomNavigationView, R.id.notifications);

                // Check if NotificationFragment is visible, and if so, refresh it
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
                if (currentFragment instanceof NotificationFragment) {
                    ((NotificationFragment) currentFragment).refreshNotifications();
                }
            }
        };

        // Use LocalBroadcastManager to register receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver,
                new IntentFilter("com.example.icstmgsfbstud.NOTIFICATION_RECEIVED"));
    }

    /**
     * Apply ripple animation to the selected menu item
     */
    private void applyRippleToMenuItem(int itemId) {
        // Use our helper method to apply ripple effect
        BottomNavAnimationHelper.applyRippleToMenuItem(binding.bottomNavigationView, itemId);
    }

    private void updateFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }
            // Get the new FCM token
            String token = task.getResult();
            // Save the token in Firebase
            saveTokenToFirebase(token);

            // Get the student number from SharedPreferences
            String studentNumber = sharedPreferences.getString("studentNumber", "");
            if (!studentNumber.isEmpty()) {
                // Subscribe to a topic for grade updates
                FirebaseMessaging.getInstance().subscribeToTopic("grade_updates_" + studentNumber)
                        .addOnCompleteListener(task1 -> {
                            String msg = task1.isSuccessful() ? "Subscribed to grade updates" : "Subscribe failed";
                            Log.d(TAG, msg);
                        });
            }
        });
    }

    private void saveTokenToFirebase(String token) {
        String studentNumber = sharedPreferences.getString("studentNumber", "");
        if (!studentNumber.isEmpty()) {
            DatabaseReference tokenRef = FirebaseDatabase.getInstance().getReference("normalLogin/students/" + studentNumber + "/fcmtoken");
            tokenRef.setValue(token);
        }
    }

    private void sendGradeUpdateFCMNotification(String studentNumber, String title, String message) {
        // Get the FCM token from Firebase for this student
        DatabaseReference tokenRef = FirebaseDatabase.getInstance().getReference("normalLogin/students/" + studentNumber + "/fcmtoken");
        tokenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fcmToken = snapshot.getValue(String.class);
                if (fcmToken != null) {
                    // Create the payload for the FCM message
                    JSONObject notification = new JSONObject();
                    try {
                        notification.put("to", fcmToken);
                        JSONObject notificationBody = new JSONObject();
                        notificationBody.put("title", title);
                        notificationBody.put("body", message);
                        notification.put("notification", notificationBody);

                        // Send the notification using a background thread
                        new Thread(() -> {
                            sendFCMMessage(notification);
                        }).start();
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to create FCM notification JSON", e);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch FCM token", error.toException());
            }
        });
    }

    private void sendFCMMessage(JSONObject notification) {
        try {
            URL url = new URL("https://fcm.googleapis.com/fcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "key=YOUR_SERVER_KEY"); // Replace with your FCM Server Key
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(notification.toString().getBytes());
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "FCM message sent with response code: " + responseCode);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send FCM message", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sync badge count when MainActivity is resumed
        updateNotificationBadge();
    }

    // Implementation of the OnNotificationChangeListener interface
    @Override
    public void onNotificationDeleted(int newNotificationCount) {
        // Update the badge with the new count after a notification is deleted
        updateNotificationBadge(newNotificationCount);
    }

    private void updateNotificationBadge(int notificationCount) {
        BadgeDrawable badge = binding.bottomNavigationView.getOrCreateBadge(R.id.notifications);
        if (notificationCount > 0) {
            badge.setVisible(true);
            badge.setNumber(notificationCount);
            // Animate the badge
            BottomNavAnimationHelper.animateBadge(binding.bottomNavigationView, R.id.notifications);
        } else {
            badge.setVisible(false);
        }
    }

    private void updateNotificationBadge() {
        // Retrieve the current notification count from SharedPreferences and update the badge
        int notificationCount = sharedPreferences.getInt("notification_count", 0);
        BadgeDrawable badge = binding.bottomNavigationView.getOrCreateBadge(R.id.notifications);
        if (notificationCount > 0) {
            badge.setVisible(true);
            badge.setNumber(notificationCount);
        } else {
            badge.setVisible(false);
        }
    }

    // Unregister the BroadcastReceiver when activity is destroyed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
        }
    }

    // Check if user is logged in by checking SharedPreferences
    private boolean isUserLoggedIn() {
        return sharedPreferences.contains("studentNumber");
    }

    // Redirect to LoginActivity if not logged in
    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("grade", "Grade Updates", NotificationManager.IMPORTANCE_HIGH);
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Request notification permission for Android 13+ (API 33+)
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
            } else {
                Log.d(TAG, "Notification permission denied");
            }
        }
    }

    // Initialize Firebase references for grades and subjects
    private void initializeFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        gradesRef = database.getReference("grades/grade2425_1stsem/data");
        subjectsRef = database.getReference("subjects/First Semester_2024-2025");
    }

    // Start listening for grade changes
    private void listenForGradeChanges() {
        String studentNumber = sharedPreferences.getString("studentNumber", "");
        Log.d(TAG, "Listening for grade changes. Student number: " + studentNumber);

        if (!studentNumber.isEmpty()) {
            // Fetch and store the initial state of grades before listening for changes
            gradesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot gradeSnapshot : snapshot.getChildren()) {
                        String studnum = gradeSnapshot.child("studnum").getValue(String.class);
                        if (studentNumber.equals(studnum)) {
                            String subjectId = gradeSnapshot.child("id").getValue(String.class);
                            String finalGrade = gradeSnapshot.child("finalgrade").getValue(String.class);
                            String remarks = gradeSnapshot.child("remarks").getValue(String.class);

                            if (subjectId != null) {
                                String currentGrade = finalGrade + ":" + remarks;
                                lastKnownGrades.put(subjectId, currentGrade); // Store the initial grade state
                            }
                        }
                    }
                    // Now start listening for grade changes
                    startListeningForGradeChanges(studentNumber);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to fetch initial grades: " + error.getMessage());
                }
            });
        } else {
            Log.e(TAG, "Student number is empty. Cannot listen for grade changes.");
        }
    }

    // Listen for real-time grade changes in Firebase
    private void startListeningForGradeChanges(String studentNumber) {
        gradesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot gradeSnapshot : snapshot.getChildren()) {
                    String studnum = gradeSnapshot.child("studnum").getValue(String.class);
                    if (studentNumber.equals(studnum)) {
                        checkAndNotifyGradeChange(gradeSnapshot);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to listen for grade changes: " + error.getMessage());
            }
        });
    }

    // Check if a grade has changed and notify the user
    private void checkAndNotifyGradeChange(DataSnapshot gradeSnapshot) {
        String subjectId = gradeSnapshot.child("id").getValue(String.class);
        String finalGrade = gradeSnapshot.child("finalgrade").getValue(String.class);
        String remarks = gradeSnapshot.child("remarks").getValue(String.class);

        if (subjectId == null) {
            Log.e(TAG, "Subject ID is null for grade snapshot.");
            return;
        }

        String lastKnownGrade = lastKnownGrades.get(subjectId);
        String currentGrade = finalGrade + ":" + remarks;

        if (lastKnownGrade == null || !lastKnownGrade.equals(currentGrade)) {
            fetchSubjectDescriptionAndNotify(subjectId, finalGrade, remarks);
            lastKnownGrades.put(subjectId, currentGrade);

            // Send FCM notification to the user
            String studentNumber = sharedPreferences.getString("studentNumber", "");
            sendGradeUpdateFCMNotification(studentNumber, "Grade Update", "Your grade for subject " + subjectId + " has been updated.");
        }
    }

    // Fetch subject description from Firebase and send a notification
    private void fetchSubjectDescriptionAndNotify(String subjectId, String finalGrade, String remarks) {
        subjectsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String description = "Subject not found";
                for (DataSnapshot subjectSnapshot : snapshot.getChildren()) {
                    String id = subjectSnapshot.child("id").getValue(String.class);
                    if (subjectId.equals(id)) {
                        description = subjectSnapshot.child("description").getValue(String.class);
                        break;
                    }
                }
                sendGradeUpdateNotification(subjectId, finalGrade, remarks, description);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                sendGradeUpdateNotification(subjectId, finalGrade, remarks, "Failed to fetch description");
            }
        });
    }

    // Send a notification to both the system tray and store it in SharedPreferences
    private void sendGradeUpdateNotification(String subjectId, String finalGrade, String remarks, String description) {
        String title = "Grade Update";
        String body = "Your grade for " + description + " (ID: " + subjectId + ") has been updated.";
        if (finalGrade != null && !finalGrade.isEmpty()) {
            body += " Final grade: " + finalGrade;
        }
        if (remarks != null && !remarks.isEmpty()) {
            body += " Remarks: " + remarks;
        }

        // Save the notification in SharedPreferences to ensure it's displayed in the NotificationFragment
        saveNotificationInPreferences(body);

        // Update notification count badge dynamically with animation
        updateNotificationBadge();
        BottomNavAnimationHelper.animateBadge(binding.bottomNavigationView, R.id.notifications);

        // Display a push notification in the system tray
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_person_24)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        managerCompat.notify(subjectId.hashCode(), builder.build());

        // Notify the NotificationFragment (using LocalBroadcastManager)
        Intent intent = new Intent("com.example.icstmgsfbstud.NOTIFICATION_RECEIVED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Save received notifications in SharedPreferences for display in the NotificationFragment
    private void saveNotificationInPreferences(String notificationMessage) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int notificationCount = sharedPreferences.getInt("notification_count", 0);

        // Avoid duplicates in notifications by checking if the message already exists
        for (int i = 0; i < notificationCount; i++) {
            if (sharedPreferences.getString("notification_" + i, "").equals(notificationMessage)) {
                return; // Duplicate found, do not save again
            }
        }

        editor.putString("notification_" + notificationCount, notificationMessage);
        editor.putInt("notification_count", notificationCount + 1);
        editor.apply();
    }

    // Replace the current fragment with a new fragment with animation
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Add custom animations for fragment transitions
        fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right, // enter animation
                R.anim.slide_out_left, // exit animation
                R.anim.slide_in_left,  // pop enter animation
                R.anim.slide_out_right // pop exit animation
        );

        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}