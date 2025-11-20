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

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentEventsBinding;
import com.example.arcane.model.Event;
import com.example.arcane.repository.EventRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * OrganizerEventsFragment.java
 * 
 * Purpose: Displays events created by the current organizer.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and RecyclerView with adapter pattern for list display.
 * 
 * Outstanding Issues:
 * - Event click handling is not yet implemented (placeholder comment on line 46)
 * 
 * @version 1.0
 */
public class OrganizerEventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventCardAdapter adapter;
    private EventRepository eventRepository;
    private List<Event> allEvents = new ArrayList<>(); // Store all events for filtering
    private boolean isOrganizer = true; // Always true since EventsRouterFragment routes us here

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

        eventRepository = new EventRepository();
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

        // Since EventsRouterFragment already determined we're an organizer, 
        // we can skip the role check and load events immediately
        isOrganizer = true;
        binding.fabAddEvent.setVisibility(View.VISIBLE);
        binding.navButtonsContainer.setVisibility(View.GONE);
        binding.fabAddEvent.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                navController.navigate(R.id.navigation_create_event);
            }
        });
        loadOrganizerEvents();
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
     * Loads events created by the current organizer.
     */
    private void loadOrganizerEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String organizerId = currentUser.getUid();
        eventRepository.getEventsByOrganizer(organizerId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || binding == null || adapter == null) return;
                    
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
     * Called when the fragment becomes visible to the user.
     * Refreshes the events list to show newly created events.
     */
    @Override
    public void onResume() {
        super.onResume();
        // Refresh events list when returning to this fragment (e.g., after creating an event)
        // EventsRouterFragment only routes to this fragment when user is an organizer,
        // so we can safely always load organizer events. If role changes, EventsRouterFragment
        // will replace this fragment with the appropriate one.
        if (isOrganizer) {
            loadOrganizerEvents();
        }
        // Defensive: if somehow isOrganizer is false, do nothing (EventsRouterFragment should replace us)
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


