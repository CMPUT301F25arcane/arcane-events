package com.example.arcane.ui.users;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
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

        // Show role chip (display only, not interactive)
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            holder.roleChip.setVisibility(View.VISIBLE);
            holder.roleChip.setText(user.getRole());
            holder.roleChip.setCheckable(false);
            holder.roleChip.setClickable(false);
        } else {
            holder.roleChip.setVisibility(View.GONE);
        }

        // Set placeholder image
        holder.imageView.setImageResource(android.R.drawable.ic_menu_myplaces);

        // Setup options menu button
        holder.optionsMenuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            popupMenu.getMenuInflater().inflate(R.menu.user_card_options_menu, popupMenu.getMenu());
            
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_delete_user) {
                    // TODO: Implement delete functionality
                    return true;
                }
                return false;
            });
            
            popupMenu.show();
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameView;
        TextView emailView;
        Chip roleChip;
        ImageButton optionsMenuButton;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.user_image);
            nameView = itemView.findViewById(R.id.user_name);
            emailView = itemView.findViewById(R.id.user_email);
            roleChip = itemView.findViewById(R.id.role_status);
            optionsMenuButton = itemView.findViewById(R.id.options_menu_button);
        }
    }
}

