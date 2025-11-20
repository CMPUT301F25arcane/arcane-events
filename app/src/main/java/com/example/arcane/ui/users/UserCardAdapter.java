package com.example.arcane.ui.users;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arcane.R;
import com.example.arcane.model.UserProfile;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying user cards.
 */
public class UserCardAdapter extends RecyclerView.Adapter<UserCardAdapter.UserViewHolder> {

    private final List<UserProfile> users = new ArrayList<>();

    /**
     * Sets the list of users to display.
     */
    public void setItems(@NonNull List<UserProfile> items) {
        users.clear();
        users.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_card, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserProfile user = users.get(position);
        holder.nameView.setText(user.getName() != null ? user.getName() : "Unknown");
        holder.emailView.setText(user.getEmail() != null ? user.getEmail() : "");
        holder.roleView.setText(user.getRole() != null ? user.getRole() : "");

        // Show role chip
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            holder.roleChip.setVisibility(View.VISIBLE);
            holder.roleChip.setText(user.getRole());
        } else {
            holder.roleChip.setVisibility(View.GONE);
        }

        // Set placeholder image
        holder.imageView.setImageResource(android.R.drawable.ic_menu_myplaces);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameView;
        TextView emailView;
        TextView roleView;
        Chip roleChip;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.user_image);
            nameView = itemView.findViewById(R.id.user_name);
            emailView = itemView.findViewById(R.id.user_email);
            roleView = itemView.findViewById(R.id.user_role);
            roleChip = itemView.findViewById(R.id.role_status);
        }
    }
}

