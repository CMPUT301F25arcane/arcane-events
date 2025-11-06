/*
package com.example.arcane.model;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arcane.R;

import java.util.List;

// Adapter for displaying event cards in the RecyclerView.

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private final List<EventModel> events;

    public EventAdapter(List<EventModel> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_globaleventcards, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventModel event = events.get(position);
        holder.tvName.setText(event.getEventName());
        holder.tvDate.setText(event.getEventDate());
        holder.tvLocation.setText(event.getLocation());
        
        // Set waitlist status text with color coding
        String status = event.getWaitlistStatus();
        holder.tvStatus.setText("Waitlist Status: " + status);
        
        // Set status color based on value (PENDING = grey, WON = green, LOST = red)
        if (status != null) {
            switch (status.toUpperCase()) {
                case "PENDING":
                    holder.tvStatus.setTextColor(Color.parseColor("#555555")); // Grey
                    break;
                case "WON":
                    holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                    break;
                case "LOST":
                    holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // Red
                    break;
                default:
                    holder.tvStatus.setTextColor(Color.parseColor("#1976D2")); // Default blue
                    break;
            }
        }

        // Load image from Firebase URL if available
        if (event.getPosterImageUrl() != null && !event.getPosterImageUrl().isEmpty()) {
            // Use Glide or Picasso to load image from URL
            // For now, using placeholder - you'll need to add image loading library
            holder.imgPoster.setImageResource(android.R.drawable.ic_menu_gallery);
        } else {
            holder.imgPoster.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView tvName, tvDate, tvLocation, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgEventPoster);
            tvName = itemView.findViewById(R.id.tvEventName);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvLocation = itemView.findViewById(R.id.tvEventLocation);
            tvStatus = itemView.findViewById(R.id.tvWaitlistStatus);
        }
    }
}
*/