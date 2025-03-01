package com.example.icstmgsfbstud;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<String> notificationList;
    private OnNotificationDeleteListener deleteListener;

    public interface OnNotificationDeleteListener {
        void onDelete(int position);
    }

    public NotificationAdapter(List<String> notificationList, OnNotificationDeleteListener deleteListener) {
        this.notificationList = notificationList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view, deleteListener);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        String notification = notificationList.get(position);
        holder.notificationTextView.setText(notification);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView notificationTextView;
        ImageButton deleteButton;
        CardView notificationCardView;

        public NotificationViewHolder(@NonNull View itemView, OnNotificationDeleteListener deleteListener) {
            super(itemView);
            notificationTextView = itemView.findViewById(R.id.notificationTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            notificationCardView = itemView.findViewById(R.id.notificationCardView);

            // Set up the click listener for the delete button
            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        deleteListener.onDelete(position);
                    }
                }
            });
        }
    }
}
