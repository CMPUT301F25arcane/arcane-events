package com.example.arcane.ui.events;

/**
 * This file defines the EntrantsFragment class, which displays a list of event entrants
 * (registered users) for a specific event. It loads registration data from Firestore,
 * fetches user details for each entrant, and displays them in a RecyclerView with their
 * decision status (PENDING, INVITED, ACCEPTED, DECLINED, etc.).
 *
 * Design Pattern: MVVM (Model-View-ViewModel) - Fragment acts as View
 * - Uses EventService and UserService to fetch data from repositories
 * - Uses EntrantAdapter to display data in RecyclerView
 * - Follows Android Fragment lifecycle
 *
 * Outstanding Issues:
 * - View map button is hidden and not implemented
 * - Uses array-based counter for async operations (could use CompletableFuture or RxJava)
 * - Error handling could be improved with better user feedback
 * - Location field is not displayed (not available in Users model)
 */
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
 * Fragment that displays a list of event entrants with their registration status.
 * Loads event registrations and user details, then displays them in a RecyclerView.
 *
 * @version 1.0
 */
public class EntrantsFragment extends Fragment {

    private FragmentEntrantsBinding binding;
    private EntrantAdapter adapter;
    private EventService eventService;
    private UserService userService;
    private String eventId;

    /**
     * Initializes the fragment and retrieves the event ID from arguments.
     *
     * @param savedInstanceState If the fragment is being recreated from a previous saved state, this is the state
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     * @return The root View for the fragment's layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEntrantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored.
     * Initializes the RecyclerView, sets up the adapter, and loads entrant data.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     */
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

    /**
     * Loads event registrations and user details for each entrant.
     * Fetches registration data from EventService, then fetches user details
     * for each registration and updates the adapter when all data is loaded.
     */
    private void loadEntrants() {
        eventService.getEventRegistrations(eventId)
                .addOnSuccessListener(result -> {
                    String status = (String) result.get("status");
                    if (!"success".equals(status)) {
                        Toast.makeText(requireContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> registrations = (List<Map<String, Object>>) result.get("registrations");
                    if (registrations == null || registrations.isEmpty()) {
                        adapter.setItems(new ArrayList<>());
                        return;
                    }

                    // Load user details for each registration
                    List<EntrantItem> entrantItems = new ArrayList<>();
                    final int[] remaining = {registrations.size()};

                    for (Map<String, Object> registration : registrations) {
                        String entrantId = (String) registration.get("entrantId");
                        String decisionStatus = (String) registration.get("status");

                        userService.getUserById(entrantId)
                                .addOnSuccessListener(userDoc -> {
                                    Users user = userDoc.toObject(Users.class);
                                    EntrantItem item = new EntrantItem();
                                    if (user != null) {
                                        item.name = user.getName();
                                        item.email = user.getEmail();
                                        item.phone = user.getPhone();
                                    } else {
                                        item.name = "Unknown";
                                        item.email = "";
                                        item.phone = "";
                                    }
                                    item.status = decisionStatus != null ? decisionStatus : "PENDING";
                                    entrantItems.add(item);

                                    remaining[0] -= 1;
                                    if (remaining[0] == 0) {
                                        adapter.setItems(entrantItems);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // On failure, still add entry with limited info
                                    EntrantItem item = new EntrantItem();
                                    item.name = "Unknown";
                                    item.email = "";
                                    item.phone = "";
                                    item.status = decisionStatus != null ? decisionStatus : "PENDING";
                                    entrantItems.add(item);

                                    remaining[0] -= 1;
                                    if (remaining[0] == 0) {
                                        adapter.setItems(entrantItems);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error loading entrants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Navigates back to the previous fragment in the navigation stack.
     */
    private void navigateBack() {
        androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    /**
     * Called when the view hierarchy associated with the fragment is being removed.
     * Cleans up the binding reference to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Simple data class for holding entrant display information.
     * Used to pass entrant data from the fragment to the adapter.
     *
     * @version 1.0
     */
    static class EntrantItem {
        String name;
        String email;
        String phone;
        String status;
    }
}

