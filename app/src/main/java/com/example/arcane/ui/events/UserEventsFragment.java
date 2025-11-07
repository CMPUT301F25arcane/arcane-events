package com.example.arcane.ui.events;

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
 * UserEventsFragment.java
 * 
 * Purpose: Displays events that the current user has registered for.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and RecyclerView with adapter pattern for list display.
 * 
 * Outstanding Issues:
 * - Event click handling is not yet implemented (placeholder comment on line 48)
 * - Uses registeredEventIds workaround from UserProfile; should ideally query waiting list subcollections
 * - Race condition possible when loading multiple events asynchronously (remaining counter approach)
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
     * Creates and returns the view hierarchy for this fragment.
     *
     * @param inflater the layout inflater
     * @param container the parent view group
     * @param savedInstanceState the saved instance state
     * @return the root view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned.
     *
     * @param view the view returned by onCreateView
     * @param savedInstanceState the saved instance state
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
     * Loads events that the current user has registered for.
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
     * Called when the view hierarchy is being removed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


