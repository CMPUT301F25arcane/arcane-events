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
import com.example.arcane.model.Users;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventCardAdapter adapter;
    private EventRepository eventRepository;
    private UserRepository userRepository;
    private List<Event> allEvents = new ArrayList<>(); // Store all events for filtering
    private boolean isOrganizer = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

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

        // Setup search functionality
        setupSearch();

        // Check user role and set visibility accordingly
        checkUserRoleAndSetVisibility();
    }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


