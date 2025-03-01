//package com.example.icstmgsfbstud;
//
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//
//import java.util.HashMap;
//
//public class SessionManager {
//
//    SharedPreferences pref;
//    SharedPreferences.Editor editor;
//    Context context;
//    int PRIVATE_MODE = 0;
//    private static final String PREF_NAME = "StudentSession";
//    private static final String IS_LOGIN = "IsLoggedIn";
//    public static final String KEY_STUDENT_NUMBER = "studentNumber";
//    public static final String KEY_NAME = "name";
//    public static final String KEY_COURSE = "course";
//    // Add more keys as needed
//
//    public SessionManager(Context context) {
//        this.context = context;
//        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
//        editor = pref.edit();
//    }
//
//    public void createLoginSession(String studentNumber, Student student) {
//        editor.putBoolean(IS_LOGIN, true);
//        editor.putString(KEY_STUDENT_NUMBER, studentNumber);
//        editor.putString(KEY_NAME, student.getName());
//        editor.putString(KEY_COURSE, student.getCourse());
//        // Add more fields if needed
//        editor.commit();
//    }
//
//    public HashMap<String, String> getUserDetails() {
//        HashMap<String, String> user = new HashMap<>();
//        user.put(KEY_STUDENT_NUMBER, pref.getString(KEY_STUDENT_NUMBER, null));
//        user.put(KEY_NAME, pref.getString(KEY_NAME, null));
//        user.put(KEY_COURSE, pref.getString(KEY_COURSE, null));
//        // Add more fields if needed
//        return user;
//    }
//
//    public void logoutUser() {
//        editor.clear();
//        editor.commit();
//        Intent i = new Intent(context, LoginActivity.class);
//        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(i);
//    }
//
//    public boolean isLoggedIn() {
//        return pref.getBoolean(IS_LOGIN, false);
//    }
//}
//
