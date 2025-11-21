/**
 * OrganizerScanEventsFragment.java
 * 
 * Purpose: Displays events created by the current organizer when accessed from Scan navigation.
 * When an event is clicked, it navigates directly to the QR code page.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and RecyclerView with adapter pattern for list display.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.qrcode;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentEventsBinding;
import com.example.arcane.model.Event;
import com.example.arcane.model.Users;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.service.UserService;
import com.example.arcane.ui.events.EventCardAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for displaying organizer events in scan context.
 * Navigates to QR code when event is clicked.
 */
public class OrganizerScanEventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventCardAdapter adapter;
    private EventRepository eventRepository;
    private UserRepository userRepository;
    private List<Event> allEvents = new ArrayList<>();

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
        
        // Navigate to QR code when event is clicked
        adapter = new EventCardAdapter(event -> {
            if (event.getEventId() != null) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                Bundle args = new Bundle();
                args.putString("eventId", event.getEventId());
                navController.navigate(R.id.navigation_qr_code, args);
            }
        });

        binding.eventsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.eventsRecyclerView.setAdapter(adapter);

        // Hide status chips for organizers
        adapter.setShowStatus(false);

        // Setup search functionality
        setupSearch();
        
        // Hide filter button and FAB for scan context
        binding.filterButton.setVisibility(View.GONE);
        binding.fabAddEvent.setVisibility(View.GONE);
        binding.navButtonsContainer.setVisibility(View.GONE);

        // Load organizer events
        loadOrganizerEvents();
    }

    private void setupSearch() {
        binding.searchButton.setOnClickListener(v -> performSearch());
        
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
        String query = binding.searchEditText.getText() != null 
                ? binding.searchEditText.getText().toString().trim().toLowerCase() 
                : "";
        
        List<Event> filtered = new ArrayList<>();
        for (Event event : allEvents) {
            if (query.isEmpty() || 
                (event.getEventName() != null && event.getEventName().toLowerCase().contains(query)) ||
                (event.getLocation() != null && event.getLocation().toLowerCase().contains(query))) {
                filtered.add(event);
            }
        }
        adapter.setItems(filtered);
    }

    private void loadOrganizerEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String organizerId = currentUser.getUid();
        eventRepository.getEventsByOrganizer(organizerId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || binding == null) return;
                    
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setEventId(doc.getId());
                            allEvents.add(event);
                        }
                    }
                    performSearch();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    Toast.makeText(requireContext(), "Failed to load events: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

