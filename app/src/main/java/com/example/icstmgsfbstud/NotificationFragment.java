package com.example.icstmgsfbstud;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationFragment extends Fragment {

    private RecyclerView notificationRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<String> notificationList;
    private SharedPreferences sharedPreferences;
    private OnNotificationChangeListener notificationChangeListener;

    public NotificationFragment() {
        // Required empty public constructor
    }

    // Interface for communicating with MainActivity
    public interface OnNotificationChangeListener {
        void onNotificationDeleted(int newNotificationCount);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // Ensure the context (Activity) implements OnNotificationChangeListener
            notificationChangeListener = (OnNotificationChangeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnNotificationChangeListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationRecyclerView = view.findViewById(R.id.notificationRecyclerView);
        sharedPreferences = requireActivity().getSharedPreferences("StudentPrefs", requireContext().MODE_PRIVATE);

        // Retrieve and display saved notifications without duplicates
        notificationList = getSavedNotifications();
        notificationAdapter = new NotificationAdapter(notificationList, position -> {
            // Remove notification at the specified position
            notificationList.remove(position);

            // Update SharedPreferences
            saveNotificationsToPreferences();

            // Notify adapter of the change
            notificationAdapter.notifyItemRemoved(position);
            notificationAdapter.notifyItemRangeChanged(position, notificationList.size());

            // Notify MainActivity of the change to update the badge count
            if (notificationChangeListener != null) {
                notificationChangeListener.onNotificationDeleted(notificationList.size());
            }
        });
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        notificationRecyclerView.setAdapter(notificationAdapter);
    }

    // Method to get saved notifications from SharedPreferences and prevent duplicates
    private List<String> getSavedNotifications() {
        List<String> notifications = new ArrayList<>();
        Set<String> notificationSet = new HashSet<>(); // Use Set to avoid duplicates
        int notificationCount = sharedPreferences.getInt("notification_count", 0);
        for (int i = 0; i < notificationCount; i++) {
            String notification = sharedPreferences.getString("notification_" + i, "");
            if (notificationSet.add(notification)) { // Adds to Set and returns false if duplicate
                notifications.add(notification);
            }
        }
        return notifications;
    }

    // Save the modified list of notifications to SharedPreferences
    private void saveNotificationsToPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("notification_count", notificationList.size());
        for (int i = 0; i < notificationList.size(); i++) {
            editor.putString("notification_" + i, notificationList.get(i));
        }
        editor.apply();
    }

    // Method to refresh notifications dynamically
    public void refreshNotifications() {
        notificationList.clear(); // Clear the existing list
        notificationList.addAll(getSavedNotifications()); // Retrieve updated list from SharedPreferences
        notificationAdapter.notifyDataSetChanged(); // Notify the adapter of data changes
    }
}
