package com.example.arcane.ui.events;

/**
 * This file defines the UserEventsFragment class, which displays all events that the
 * current user has registered for (joined the waitlist). It loads events from the user's
 * registeredEventIds list in their UserProfile, provides search functionality to filter
 * events by name, and allows navigation to global events. Users cannot create events.
 * Shows status chips for events the user has joined.
 *
 * Design Pattern: MVVM (Model-View-ViewModel) - Fragment acts as View
 * - Uses EventRepository, UserRepository, and DecisionRepository for data access
 * - Uses EventCardAdapter with RecyclerView for list display
 * - Uses ViewBinding for type-safe view access
 *
 * Outstanding Issues:
 * - Event click handling is now implemented (navigates to event detail)
 * - Uses array-based counter for async operations (could use CompletableFuture or RxJava)
 * - registeredEventIds workaround may need to be replaced with proper subcollection queries
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

import com.example.arcane.databinding.FragmentEventsBinding;
import com.example.arcane.model.Decision;
import com.example.arcane.model.Event;
import com.example.arcane.model.UserProfile;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment that displays all events the current user has registered for.
 * Provides search functionality and navigation to global events.
 *
 * @version 1.0
 */
public class UserEventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventCardAdapter adapter;
    private UserRepository userRepository;
    private EventRepository eventRepository;
    private DecisionRepository decisionRepository;
    private List<Event> allEvents = new ArrayList<>(); // Store all events for filtering

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
     * Initializes repositories, sets up the RecyclerView adapter, navigation button, and loads event data.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = new UserRepository();
        eventRepository = new EventRepository();
        decisionRepository = new DecisionRepository();
        adapter = new EventCardAdapter(event -> {
            // Navigate to event detail
            if (event.getEventId() != null) {
                androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), com.example.arcane.R.id.nav_host_fragment_activity_main);
                Bundle args = new Bundle();
                args.putString("eventId", event.getEventId());
                navController.navigate(com.example.arcane.R.id.navigation_event_detail, args);
            }
        });

        binding.eventsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.eventsRecyclerView.setAdapter(adapter);

        // Show status chips for users
        adapter.setShowStatus(true);

        // Users cannot create; hide FAB
        binding.fabAddEvent.setVisibility(View.GONE);

        // Show nav button to Global Events
        binding.navButtonsContainer.setVisibility(View.VISIBLE);
        binding.primaryNavButton.setText("Go to Global Events");
        binding.primaryNavButton.setOnClickListener(v -> {
            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), com.example.arcane.R.id.nav_host_fragment_activity_main);
            navController.navigate(com.example.arcane.R.id.navigation_global_events);
        });

        // Setup search functionality
        setupSearch();

        loadUserEvents();
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
                    UserProfile profile = snapshot.toObject(UserProfile.class);
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
                                        // Load user decisions to show status
                                        loadUserDecisions();
                                    }
                                });
                    }
                });
    }

    /**
     * Loads user decisions for all events to determine status.
     * Extracts event IDs from decision document paths and creates a status map
     * that is passed to the adapter to display status chips.
     */
    private void loadUserDecisions() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            performSearch();
            return;
        }

        String userId = currentUser.getUid();
        // Get all decisions for this user (collection group query)
        decisionRepository.getDecisionsByUser(userId)
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, String> statusMap = new HashMap<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Decision decision = doc.toObject(Decision.class);
                        if (decision != null && decision.getStatus() != null) {
                            // Extract eventId from document path: events/{eventId}/decisions/{decisionId}
                            String path = doc.getReference().getPath();
                            String[] pathParts = path.split("/");
                            if (pathParts.length >= 2 && "events".equals(pathParts[0])) {
                                String eventId = pathParts[1];
                                statusMap.put(eventId, decision.getStatus());
                            }
                        }
                    }
                    adapter.setEventStatusMap(statusMap);
                    performSearch();
                })
                .addOnFailureListener(e -> {
                    // On failure, just show events without status
                    performSearch();
                });
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


