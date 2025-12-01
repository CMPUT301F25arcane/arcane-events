/**
 * EntrantAdapter.java
 * 
 * Purpose: RecyclerView adapter for displaying entrant cards in a list.
 * 
 * Design Pattern: Adapter pattern for RecyclerView. Implements the standard
 * RecyclerView.Adapter interface to bind entrant data to view holders.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arcane.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying entrant cards.
 *
 * <p>Manages the display of entrant information in a RecyclerView,
 * including name, email, phone, and status chip with color coding.</p>
 *
 * @version 1.0
 */
public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.EntrantViewHolder> {

    private List<EntrantsFragment.EntrantItem> items = new ArrayList<>();
    private OnCancelClickListener cancelClickListener;

    public interface OnCancelClickListener {
        void onCancelClick(String entrantId, String decisionId);
    }

    public void setItems(@NonNull List<EntrantsFragment.EntrantItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setOnCancelClickListener(OnCancelClickListener listener) {
        this.cancelClickListener = listener;
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entrant_card, parent, false);
        return new EntrantViewHolder(view);
    }

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

        // Show/hide cancel button based on status
        // Only show for INVITED status (won lottery but not yet accepted/declined)
        if ("INVITED".equals(item.status)) {
            holder.cancelButton.setVisibility(View.VISIBLE);
            holder.cancelButton.setOnClickListener(v -> {
                if (cancelClickListener != null && item.entrantId != null && item.decisionId != null) {
                    cancelClickListener.onCancelClick(item.entrantId, item.decisionId);
                }
            });
        } else {
            holder.cancelButton.setVisibility(View.GONE);
            holder.cancelButton.setOnClickListener(null);
        }
    }

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

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EntrantViewHolder extends RecyclerView.ViewHolder {
        TextView nameView;
        TextView emailView;
        TextView phoneView;
        TextView locationView;
        Chip statusChip;
        MaterialButton cancelButton;

        EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.entrant_name);
            emailView = itemView.findViewById(R.id.entrant_email);
            phoneView = itemView.findViewById(R.id.entrant_phone);
            locationView = itemView.findViewById(R.id.entrant_location);
            statusChip = itemView.findViewById(R.id.entrant_status);
            cancelButton = itemView.findViewById(R.id.cancel_button);
        }
    }
}

