/**
 * AllNotificationsFragment.java
 * 
 * Purpose: Fragment to display all notifications from all users in the database (admin only).
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and RecyclerView with adapter pattern for list display.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentAllNotificationsBinding;
import com.example.arcane.model.Notification;
import com.example.arcane.repository.NotificationRepository;

import java.util.List;
import java.util.Map;

/**
 * Fragment to display all notifications from all users in the database (admin only).
 *
 * <p>Displays a comprehensive list of all notifications sent to all users in the system.
 * Admin users can view notifications across all users for monitoring and management purposes.</p>
 *
 * @version 1.0
 */
public class AllNotificationsFragment extends Fragment {

    private FragmentAllNotificationsBinding binding;
    private NotificationCardAdapter adapter;
    private NotificationRepository notificationRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAllNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notificationRepository = new NotificationRepository();
        adapter = new NotificationCardAdapter();

        binding.notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.notificationsRecyclerView.setAdapter(adapter);

        loadAllNotifications();
    }

    /**
     * Loads all notifications from all users in the repository.
     */
    private void loadAllNotifications() {
        notificationRepository.getAllNotificationsWithEmails()
                .addOnSuccessListener(result -> {
                    if (!isAdded() || binding == null || adapter == null) return;
                    
                    List<Notification> notifications = result.getNotifications();
                    Map<String, String> emailMap = result.getUserIdToEmailMap();
                    
                    if (notifications == null || notifications.isEmpty()) {
                        binding.emptyStateText.setVisibility(View.VISIBLE);
                        binding.notificationsRecyclerView.setVisibility(View.GONE);
                    } else {
                        binding.emptyStateText.setVisibility(View.GONE);
                        binding.notificationsRecyclerView.setVisibility(View.VISIBLE);
                        adapter.setItems(notifications, emailMap);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    Toast.makeText(requireContext(), "Failed to load notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.emptyStateText.setVisibility(View.VISIBLE);
                    binding.notificationsRecyclerView.setVisibility(View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

