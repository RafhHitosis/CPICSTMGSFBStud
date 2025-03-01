//package com.example.icstmgsfbstud;
//
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//public class landing extends AppCompatActivity {
//
//    private static final String PREFS_NAME = "MyPrefsFile";
//    private static final String KEY_LANDING_VIEWED = "landing_viewed";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
////        // Check if landing page has been viewed before
////        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
////        boolean landingViewed = settings.getBoolean(KEY_LANDING_VIEWED, false);
////
////        if (landingViewed) {
////            // If landing page has been viewed, go directly to login
////            goToLogin();
////            return;
////        }
//
//        // Otherwise, show the landing page layout
//        setContentView(R.layout.activity_landing);
//
//        // Get Started button click listener
//        Button getStartedButton = findViewById(R.id.get_started_button);
//        getStartedButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Mark landing page as viewed
////                SharedPreferences.Editor editor = settings.edit();
////                editor.putBoolean(KEY_LANDING_VIEWED, true);
////                editor.apply();
//
//                // Navigate to login activity
//                goToLogin();
//            }
//        });
//    }
//
//    private void goToLogin() {
//        Intent intent = new Intent(landing.this, login.class);
//        startActivity(intent);
//        finish(); // Optional: Finish the landing activity to prevent going back to it
//    }
//}
