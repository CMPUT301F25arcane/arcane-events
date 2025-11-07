/**
 * EventCardAdapter.java
 * 
 * Purpose: RecyclerView adapter for displaying event cards in a list.
 * 
 * Design Pattern: Adapter pattern for RecyclerView. Implements the standard
 * RecyclerView.Adapter interface to bind event data to view holders.
 * 
 * Outstanding Issues:
 * - Image loading uses placeholder; should integrate Glide or Picasso for poster images
 * 
 * @version 1.0
 */
package com.example.arcane.ui.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arcane.R;
import com.example.arcane.model.Event;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for displaying event cards.
 *
 * <p>Manages the display of event information in a RecyclerView,
 * including event name, location, date, and poster image.</p>
 *
 * @version 1.0
 */
public class EventCardAdapter extends RecyclerView.Adapter<EventCardAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(@NonNull Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final OnEventClickListener listener;
    private final Map<String, String> eventStatusMap = new HashMap<>(); // eventId -> status
    private boolean showStatus = true; // Hide for organizers

    /**
     * Constructs a new EventCardAdapter.
     *
     * @param listener the click listener for event cards
     */
    public EventCardAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the list of events to display.
     *
     * @param items the list of events to display
     */
    public void setItems(@NonNull List<Event> items) {
        events.clear();
        events.addAll(items);
        notifyDataSetChanged();
    }

    public void setEventStatusMap(@NonNull Map<String, String> statusMap) {
        eventStatusMap.clear();
        eventStatusMap.putAll(statusMap);
        notifyDataSetChanged();
    }

    public void setShowStatus(boolean show) {
        this.showStatus = show;
        notifyDataSetChanged();
    }

    /**
     * Creates a new ViewHolder for an event card.
     *
     * @param parent the parent ViewGroup
     * @param viewType the view type
     * @return a new EventViewHolder instance
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds event data to a ViewHolder.
     *
     * @param holder the ViewHolder to bind
     * @param position the position of the item in the list
     */
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.titleView.setText(event.getEventName());
        holder.locationView.setText(event.getLocation());

        if (event.getEventDate() != null) {
            holder.dateView.setText(android.text.format.DateFormat.format("yyyy-MM-dd", event.getEventDate().toDate()));
        } else {
            holder.dateView.setText("");
        }

        // Set status chip if enabled and status exists
        if (showStatus && event.getEventId() != null) {
            String status = eventStatusMap.get(event.getEventId());
            if (status != null && !status.isEmpty()) {
                holder.statusChip.setVisibility(View.VISIBLE);
                setStatusChip(holder.statusChip, status);
            } else {
                holder.statusChip.setVisibility(View.GONE);
            }
        } else {
            holder.statusChip.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });

        // Placeholder image; integrate Glide/Picasso later if needed
        holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    private void setStatusChip(@NonNull Chip chip, @NonNull String decisionStatus) {
        // Map Decision status to display status
        String displayStatus;
        int textColor;
        int bgColor;

        switch (decisionStatus.toUpperCase()) {
            case "PENDING":
                displayStatus = "WAITING";
                textColor = ContextCompat.getColor(chip.getContext(), R.color.status_pending);
                bgColor = ContextCompat.getColor(chip.getContext(), R.color.status_pending_bg);
                break;
            case "INVITED":
                displayStatus = "WON";
                textColor = ContextCompat.getColor(chip.getContext(), R.color.status_won);
                bgColor = ContextCompat.getColor(chip.getContext(), R.color.status_won_bg);
                break;
            case "ACCEPTED":
                displayStatus = "ACCEPTED";
                textColor = ContextCompat.getColor(chip.getContext(), R.color.status_accepted);
                bgColor = ContextCompat.getColor(chip.getContext(), R.color.status_accepted_bg);
                break;
            case "DECLINED":
                displayStatus = "DECLINED";
                textColor = ContextCompat.getColor(chip.getContext(), R.color.status_declined);
                bgColor = ContextCompat.getColor(chip.getContext(), R.color.status_declined_bg);
                break;
            case "LOST":
            case "CANCELLED":
                displayStatus = "LOST";
                textColor = ContextCompat.getColor(chip.getContext(), R.color.status_lost);
                bgColor = ContextCompat.getColor(chip.getContext(), R.color.status_lost_bg);
                break;
            default:
                displayStatus = "WAITING";
                textColor = ContextCompat.getColor(chip.getContext(), R.color.status_pending);
                bgColor = ContextCompat.getColor(chip.getContext(), R.color.status_pending_bg);
        }

        chip.setText(displayStatus);
        chip.setTextColor(textColor);
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(bgColor));
    }

    /**
     * Gets the number of items in the adapter.
     *
     * @return the number of events
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for event card items.
     *
     * @version 1.0
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleView;
        TextView dateView;
        TextView locationView;
        Chip statusChip;

        /**
         * Constructs a new EventViewHolder.
         *
         * @param itemView the item view
         */
        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.event_image);
            titleView = itemView.findViewById(R.id.event_name);
            dateView = itemView.findViewById(R.id.event_date);
            locationView = itemView.findViewById(R.id.event_location);
            statusChip = itemView.findViewById(R.id.waitlist_status);
        }
    }
}


