package com.example.arcane.ui.events;

/**
 * This file defines the EntrantAdapter class, which is responsible for displaying
 * event entrants in a RecyclerView. It adapts EntrantItem data objects into
 * view holders that display entrant information including name, email, phone,
 * location, and decision status.
 *
 * Design Pattern: Adapter Pattern (RecyclerView.Adapter)
 * - Implements the standard RecyclerView.Adapter interface to bridge data and views
 * - Uses ViewHolder pattern for efficient view recycling
 *
 * Outstanding Issues:
 * - Location field is not available in the Users model, so locationView is always empty
 * - Status chip visibility handling could be improved for better UX
 * - Status mapping logic (PENDING -> WAITING, INVITED -> WON) may need clarification
 */
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arcane.R;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying event entrants in a RecyclerView.
 * Manages the binding of EntrantItem data to view holders and handles
 * status chip styling based on decision status.
 *
 * @version 1.0
 */
public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.EntrantViewHolder> {

    private List<EntrantsFragment.EntrantItem> items = new ArrayList<>();

    /**
     * Sets the list of entrant items to display and notifies the adapter of the change.
     *
     * @param items The list of EntrantItem objects to display
     */
    public void setItems(@NonNull List<EntrantsFragment.EntrantItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    /**
     * Creates a new ViewHolder instance for an entrant item.
     *
     * @param parent The ViewGroup into which the new View will be added
     * @param viewType The view type of the new View
     * @return A new EntrantViewHolder instance
     */
    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant_card, parent, false);
        return new EntrantViewHolder(view);
    }

    /**
     * Binds the entrant data at the specified position to the ViewHolder.
     * Sets the name, email, phone, location, and status chip for the entrant.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position of the item in the data set
     */
    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        EntrantsFragment.EntrantItem item = items.get(position);

        holder.nameView.setText(item.name != null ? item.name : "Unknown");
        holder.emailView.setText(item.email != null ? item.email : "");
        holder.phoneView.setText(item.phone != null ? item.phone : "");
        holder.locationView.setText(""); // Location not available in Users model

        // Set status chip
        if (item.status != null) {
            holder.statusChip.setVisibility(View.VISIBLE);
            setStatusChip(holder.statusChip, item.status);
        } else {
            holder.statusChip.setVisibility(View.GONE);
        }
    }

    /**
     * Configures the status chip with appropriate text, text color, and background color
     * based on the decision status. Maps decision statuses to display text and colors.
     *
     * @param chip The Chip view to configure
     * @param decisionStatus The decision status string (PENDING, INVITED, ACCEPTED, DECLINED, LOST, CANCELLED)
     */
    private void setStatusChip(@NonNull Chip chip, @NonNull String decisionStatus) {
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
        return items.size();
    }

    /**
     * ViewHolder class that holds references to the views for each entrant item.
     * Provides efficient view recycling by caching view references.
     *
     * @version 1.0
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        TextView nameView;
        TextView emailView;
        TextView phoneView;
        TextView locationView;
        Chip statusChip;

        /**
         * Constructs a new EntrantViewHolder and initializes all view references.
         *
         * @param itemView The root view of the item layout
         */
        EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.entrant_name);
            emailView = itemView.findViewById(R.id.entrant_email);
            phoneView = itemView.findViewById(R.id.entrant_phone);
            locationView = itemView.findViewById(R.id.entrant_location);
            // Status chip - need to add to layout or use existing view
            statusChip = itemView.findViewById(R.id.entrant_status);
        }
    }
}

