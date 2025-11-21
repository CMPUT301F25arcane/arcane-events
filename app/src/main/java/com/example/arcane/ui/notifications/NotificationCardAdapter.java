package com.example.arcane.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arcane.R;
import com.example.arcane.model.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying notification cards.
 */
public class NotificationCardAdapter extends RecyclerView.Adapter<NotificationCardAdapter.NotificationViewHolder> {

    private final List<Notification> notifications = new ArrayList<>();
    private final java.util.Map<String, String> userIdToEmailMap = new java.util.HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    /**
     * Sets both notifications and email map.
     */
    public void setItems(@NonNull List<Notification> items, @NonNull java.util.Map<String, String> emailMap) {
        notifications.clear();
        notifications.addAll(items);
        userIdToEmailMap.clear();
        userIdToEmailMap.putAll(emailMap);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_card, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        
        holder.titleView.setText(notification.getTitle() != null ? notification.getTitle() : "Notification");
        holder.messageView.setText(notification.getMessage() != null ? notification.getMessage() : "");
        
        // Set timestamp
        if (notification.getTimestamp() != null) {
            holder.timestampView.setText(dateFormat.format(notification.getTimestamp().toDate()));
        } else {
            holder.timestampView.setText("");
        }
        
        // Set user email (or fallback to userId if email not found)
        String userId = notification.getUserId();
        String userEmail = userId != null ? userIdToEmailMap.get(userId) : null;
        if (userEmail != null && !userEmail.isEmpty()) {
            holder.userIdView.setText("User: " + userEmail);
        } else if (userId != null) {
            holder.userIdView.setText("User: " + userId);
        } else {
            holder.userIdView.setText("");
        }
        
        // Set type/status
        String type = notification.getType();
        if (type != null) {
            holder.typeView.setText(type);
            holder.typeView.setVisibility(View.VISIBLE);
        } else {
            holder.typeView.setVisibility(View.GONE);
        }
        
        // Set read status
        if (Boolean.TRUE.equals(notification.getRead())) {
            holder.readStatusView.setText("Read");
            holder.readStatusView.setVisibility(View.VISIBLE);
        } else {
            holder.readStatusView.setText("Unread");
            holder.readStatusView.setVisibility(View.VISIBLE);
        }
        
        // Set background color based on type (on the card's background)
        View cardView = holder.itemView;
        if (cardView instanceof androidx.cardview.widget.CardView) {
            if ("INVITED".equals(type)) {
                ((androidx.cardview.widget.CardView) cardView).setCardBackgroundColor(
                    cardView.getContext().getResources().getColor(R.color.status_won_bg, null));
            } else if ("LOST".equals(type)) {
                ((androidx.cardview.widget.CardView) cardView).setCardBackgroundColor(
                    cardView.getContext().getResources().getColor(R.color.status_lost_bg, null));
            } else if ("REPLACEMENT_SELECTED".equals(type)) {
                // Use a distinct color for replacement notifications (e.g., blue or info color)
                ((androidx.cardview.widget.CardView) cardView).setCardBackgroundColor(
                    cardView.getContext().getResources().getColor(R.color.status_won_bg, null)); // Use same as INVITED for now
            } else {
                ((androidx.cardview.widget.CardView) cardView).setCardBackgroundColor(
                    cardView.getContext().getResources().getColor(R.color.surface, null));
            }
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView messageView;
        TextView timestampView;
        TextView userIdView;
        TextView typeView;
        TextView readStatusView;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.notification_title);
            messageView = itemView.findViewById(R.id.notification_message);
            timestampView = itemView.findViewById(R.id.notification_timestamp);
            userIdView = itemView.findViewById(R.id.notification_user_id);
            typeView = itemView.findViewById(R.id.notification_type);
            readStatusView = itemView.findViewById(R.id.notification_read_status);
        }
    }
}

