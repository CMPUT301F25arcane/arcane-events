package com.example.arcane.ui.events;

/**
 * This file defines the EventCardAdapter class, which is responsible for displaying
 * event cards in a RecyclerView. It adapts Event data objects into view holders that
 * display event information including name, date, location, and optional status chips.
 * Supports click listeners for event selection and can show/hide status chips based on context.
 *
 * Design Pattern: Adapter Pattern (RecyclerView.Adapter)
 * - Implements the standard RecyclerView.Adapter interface to bridge data and views
 * - Uses ViewHolder pattern for efficient view recycling
 * - Uses callback interface for event click handling
 *
 * Outstanding Issues:
 * - Image loading uses placeholder (should integrate Glide/Picasso)
 * - Status mapping logic (PENDING -> WAITING, INVITED -> WON) may need clarification
 */
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
 * Adapter for displaying event cards in a RecyclerView.
 * Manages the binding of Event data to view holders and handles
 * status chip styling based on decision status.
 *
 * @version 1.0
 */
public class EventCardAdapter extends RecyclerView.Adapter<EventCardAdapter.EventViewHolder> {

    /**
     * Interface for handling event click events.
     * Implemented by fragments to respond to user clicks on event cards.
     *
     * @version 1.0
     */
    public interface OnEventClickListener {
        /**
         * Called when an event card is clicked.
         *
         * @param event The Event object that was clicked
         */
        void onEventClick(@NonNull Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final OnEventClickListener listener;
    private final Map<String, String> eventStatusMap = new HashMap<>(); // eventId -> status
    private boolean showStatus = true; // Hide for organizers

    /**
     * Constructs a new EventCardAdapter with the specified click listener.
     *
     * @param listener The OnEventClickListener to handle event click events
     */
    public EventCardAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the list of events to display and notifies the adapter of the change.
     *
     * @param items The list of Event objects to display
     */
    public void setItems(@NonNull List<Event> items) {
        events.clear();
        events.addAll(items);
        notifyDataSetChanged();
    }

    /**
     * Sets the status map for events to display status chips.
     * Maps event IDs to their decision status (PENDING, INVITED, ACCEPTED, etc.).
     *
     * @param statusMap A map from event ID to decision status string
     */
    public void setEventStatusMap(@NonNull Map<String, String> statusMap) {
        eventStatusMap.clear();
        eventStatusMap.putAll(statusMap);
        notifyDataSetChanged();
    }

    /**
     * Sets whether status chips should be displayed on event cards.
     * Useful for hiding status chips for organizers who don't need to see their own status.
     *
     * @param show true to show status chips, false to hide them
     */
    public void setShowStatus(boolean show) {
        this.showStatus = show;
        notifyDataSetChanged();
    }

    /**
     * Creates a new ViewHolder instance for an event card.
     *
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new EventViewHolder instance
     */
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    /**
     * Binds the event data at the specified position to the ViewHolder.
     * Sets the title, location, date, status chip, and click listener for the event card.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the data set
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

    /**
     * Configures the status chip with appropriate text, text color, and background color
     * based on the decision status. Maps decision statuses to display text and colors.
     *
     * @param chip The Chip view to configure
     * @param decisionStatus The decision status string (PENDING, INVITED, ACCEPTED, DECLINED, LOST, CANCELLED)
     */
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
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in the adapter
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder class that holds references to the views for each event card.
     * Provides efficient view recycling by caching view references.
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
         * Constructs a new EventViewHolder and initializes all view references.
         *
         * @param itemView The root view of the item layout
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


