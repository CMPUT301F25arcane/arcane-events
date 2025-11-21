/**
 * EntrantsFragment.java
 * 
 * Purpose: Displays a list of all entrants registered for an event, showing their
 * status and contact information for organizers.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and RecyclerView with adapter pattern for list display.
 * 
 * Outstanding Issues:
 * - View map functionality is not yet implemented
 * - Location information is not available in the Users model
 * 
 * @version 1.0
 */
package com.example.arcane.ui.events;

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
import com.example.arcane.databinding.FragmentEntrantsBinding;
import com.example.arcane.model.Decision;
import com.example.arcane.model.Users;
import com.example.arcane.model.WaitingListEntry;
import com.example.arcane.service.EventService;
import com.example.arcane.service.UserService;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for displaying event entrants list.
 *
 * <p>Shows all users who have registered for an event, including their status
 * (PENDING, INVITED, ACCEPTED, DECLINED, LOST) and contact information.
 * Used by organizers to manage event registrations.</p>
 *
 * @version 1.0
 */
public class EntrantsFragment extends Fragment {

    private FragmentEntrantsBinding binding;
    private EntrantAdapter adapter;
    private EventService eventService;
    private UserService userService;
    private String eventId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEntrantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (eventId == null) {
            Toast.makeText(requireContext(), "Event ID is required", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        eventService = new EventService();
        userService = new UserService();

        adapter = new EntrantAdapter();
        binding.entrantsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.entrantsRecyclerView.setAdapter(adapter);

        // Setup toolbar back button
        binding.entrantsToolbar.setNavigationOnClickListener(v -> navigateBack());

        // Hide view map button for now (can be implemented later)
        binding.viewMapButton.setVisibility(View.GONE);

        // Load entrants
        loadEntrants();
    }

    private void loadEntrants() {
        eventService.getEventRegistrations(eventId)
                .addOnSuccessListener(result -> {
                    if (!isAdded() || binding == null || adapter == null) return;
                    
                    String status = (String) result.get("status");
                    if (!"success".equals(status)) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> registrations = (List<Map<String, Object>>) result.get("registrations");
                    if (registrations == null || registrations.isEmpty()) {
                        adapter.setItems(new ArrayList<>());
                        return;
                    }

                    // Load user details for each registration
                    // Filter out entries where the user doesn't exist (deleted users)
                    List<EntrantItem> entrantItems = new ArrayList<>();
                    final int[] remaining = {registrations.size()};

                    for (Map<String, Object> registration : registrations) {
                        String entrantId = (String) registration.get("entrantId");
                        String decisionStatus = (String) registration.get("status");

                        userService.getUserById(entrantId)
                                .addOnSuccessListener(userDoc -> {
                                    if (!isAdded() || adapter == null) return;
                                    
                                    // Only add entry if user exists (not deleted)
                                    if (userDoc.exists()) {
                                        Users user = userDoc.toObject(Users.class);
                                        if (user != null) {
                                            EntrantItem item = new EntrantItem();
                                            item.name = user.getName();
                                            item.email = user.getEmail();
                                            item.phone = user.getPhone();
                                            item.status = decisionStatus != null ? decisionStatus : "PENDING";
                                            entrantItems.add(item);
                                        }
                                    }
                                    // If user doesn't exist (deleted), skip this entry

                                    remaining[0] -= 1;
                                    if (remaining[0] == 0) {
                                        adapter.setItems(entrantItems);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded() || adapter == null) return;
                                    // On failure, skip this entry (user likely deleted)
                                    // Don't add "Unknown" entries
                                    remaining[0] -= 1;
                                    if (remaining[0] == 0) {
                                        adapter.setItems(entrantItems);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading entrants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateBack() {
        androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Simple data class for entrant display
    static class EntrantItem {
        String name;
        String email;
        String phone;
        String status;
    }
}

