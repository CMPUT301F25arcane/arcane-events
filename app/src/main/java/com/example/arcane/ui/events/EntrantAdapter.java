package com.example.arcane.ui.events;

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

public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.EntrantViewHolder> {

    private List<EntrantsFragment.EntrantItem> items = new ArrayList<>();

    public void setItems(@NonNull List<EntrantsFragment.EntrantItem> items) {
        this.items = items;
        notifyDataSetChanged();
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

