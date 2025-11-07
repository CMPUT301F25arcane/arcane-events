package com.example.arcane.ui.events;

/**
 * This file defines the OrganizerEventsFragment class, which displays all events
 * created by the current organizer. It provides search functionality to filter events
 * by name and allows organizers to create new events via a floating action button.
 * The fragment automatically refreshes when returning from creating a new event.
 * Can also fall back to user view if the user is not an organizer.
 *
 * Design Pattern: MVVM (Model-View-ViewModel) - Fragment acts as View
 * - Uses EventRepository and UserRepository for data access
 * - Uses EventCardAdapter with RecyclerView for list display
 * - Uses ViewBinding for type-safe view access
 *
 * Outstanding Issues:
 * - Event click handling is now implemented (navigates to event detail)
 */
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentEventsBinding;
import com.example.arcane.model.Event;
import com.example.arcane.model.Users;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that displays all events created by the current organizer.
 * Provides search functionality and allows creating new events.
 *
 * @version 1.0
 */
public class OrganizerEventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventCardAdapter adapter;
    private EventRepository eventRepository;
    private UserRepository userRepository;
    private List<Event> allEvents = new ArrayList<>(); // Store all events for filtering
    private boolean isOrganizer = false;

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
        binding = FragmentEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored.
     * Initializes repositories, sets up the RecyclerView adapter, FAB for creating events, and loads event data.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = new EventRepository();
        userRepository = new UserRepository();
        adapter = new EventCardAdapter(event -> {
            // Navigate to event detail
            if (event.getEventId() != null) {
                androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), com.example.arcane.R.id.nav_host_fragment_activity_main);
                android.os.Bundle args = new android.os.Bundle();
                args.putString("eventId", event.getEventId());
                navController.navigate(com.example.arcane.R.id.navigation_event_detail, args);
            }
        });

        binding.eventsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.eventsRecyclerView.setAdapter(adapter);

        // Hide status chips for organizers
        adapter.setShowStatus(false);

        // Setup search functionality
        setupSearch();

        // Check user role and set visibility accordingly
        checkUserRoleAndSetVisibility();
    }

    /**
     * Checks the user's role and sets up the appropriate UI (organizer or user view).
     * Shows FAB for creating events if user is an organizer, otherwise shows navigation button to global events.
     * Loads appropriate events based on role.
     */
    private void checkUserRoleAndSetVisibility() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        UserService userService = new UserService();
        userService.getUserById(currentUser.getUid())
                .addOnSuccessListener(snapshot -> {
                    String role = null;
                    if (snapshot.exists()) {
                        Users user = snapshot.toObject(Users.class);
                        if (user != null) {
                            role = user.getRole();
                        }
                    }
                    
                    if (role != null) {
                        String r = role.toUpperCase();
                        isOrganizer = "ORGANISER".equals(r) || "ORGANIZER".equals(r);
                    } else {
                        isOrganizer = false;
                    }
                    
                    // Set visibility based on role
                    if (isOrganizer) {
                        // Organizer view: show FAB, hide nav buttons
                        binding.fabAddEvent.setVisibility(View.VISIBLE);
                        binding.navButtonsContainer.setVisibility(View.GONE);
                        binding.fabAddEvent.setOnClickListener(v -> {
                            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                            navController.navigate(R.id.navigation_create_event);
                        });
                        loadOrganizerEvents();
                    } else {
                        // User view: hide FAB, show nav buttons
                        binding.fabAddEvent.setVisibility(View.GONE);
                        binding.navButtonsContainer.setVisibility(View.VISIBLE);
                        binding.primaryNavButton.setText("Go to Global Events");
                        binding.primaryNavButton.setOnClickListener(v -> {
                            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                            navController.navigate(R.id.navigation_global_events);
                        });
                        loadUserEvents();
                    }
                })
                .addOnFailureListener(e -> {
                    // Default to user view on failure
                    isOrganizer = false;
                    binding.fabAddEvent.setVisibility(View.GONE);
                    binding.navButtonsContainer.setVisibility(View.VISIBLE);
                    binding.primaryNavButton.setText("Go to Global Events");
                    binding.primaryNavButton.setOnClickListener(v -> {
                        androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                        navController.navigate(R.id.navigation_global_events);
                    });
                    loadUserEvents();
                });
    }

    /**
     * Sets up the search functionality with real-time text filtering.
     * Configures the search button click listener and text change listener for the search EditText.
     */
    private void setupSearch() {
        // Search button click
        binding.searchButton.setOnClickListener(v -> performSearch());

        // Search on text change (real-time search)
        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Performs a case-insensitive search on event names.
     * Filters the events list based on the search query and updates the adapter.
     * If the search query is empty, shows all events.
     */
    private void performSearch() {
        String query = binding.searchEditText.getText() != null ? 
                binding.searchEditText.getText().toString().trim() : "";
        
        if (query.isEmpty()) {
            // Show all events if search is empty
            adapter.setItems(allEvents);
        } else {
            // Filter events case-insensitively
            List<Event> filtered = new ArrayList<>();
            String queryLower = query.toLowerCase();
            for (Event event : allEvents) {
                if (event.getEventName() != null && 
                    event.getEventName().toLowerCase().contains(queryLower)) {
                    filtered.add(event);
                }
            }
            adapter.setItems(filtered);
        }
    }

    /**
     * Loads all events created by the current organizer from the repository.
     * Filters events based on the organizer's user ID and updates the adapter.
     */
    private void loadOrganizerEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String organizerId = currentUser.getUid();
        eventRepository.getEventsByOrganizer(organizerId)
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        items.add(event);
                    }
                    // Store all events and apply current search filter
                    allEvents = items;
                    performSearch();
                });
    }

    /**
     * Loads all events that the current user has registered for.
     * Retrieves the user's registeredEventIds from UserProfile and fetches each event.
     * Uses an array-based counter to track async operations completion.
     */
    private void loadUserEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String userId = currentUser.getUid();
        userRepository.getUserById(userId)
                .addOnSuccessListener(snapshot -> {
                    com.example.arcane.model.UserProfile profile = snapshot.toObject(com.example.arcane.model.UserProfile.class);
                    if (profile == null || profile.getRegisteredEventIds() == null || profile.getRegisteredEventIds().isEmpty()) {
                        allEvents = new ArrayList<>();
                        adapter.setItems(allEvents);
                        return;
                    }

                    List<String> eventIds = profile.getRegisteredEventIds();
                    List<Event> items = new ArrayList<>();

                    final int[] remaining = {eventIds.size()};
                    for (String eventId : eventIds) {
                        eventRepository.getEventById(eventId)
                                .addOnSuccessListener(doc -> {
                                    Event event = doc.toObject(Event.class);
                                    if (event != null) {
                                        event.setEventId(doc.getId());
                                        items.add(event);
                                    }
                                })
                                .addOnCompleteListener(task -> {
                                    remaining[0] -= 1;
                                    if (remaining[0] == 0) {
                                        // Store all events and apply current search filter
                                        allEvents = items;
                                        performSearch();
                                    }
                                });
                    }
                });
    }

    /**
     * Called when the fragment becomes visible to the user.
     * Refreshes the events list when returning to this fragment (e.g., after creating an event).
     */
    @Override
    public void onResume() {
        super.onResume();
        // Refresh events list when returning to this fragment (e.g., after creating an event)
        if (isOrganizer) {
            loadOrganizerEvents();
        } else {
            loadUserEvents();
        }
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
}


