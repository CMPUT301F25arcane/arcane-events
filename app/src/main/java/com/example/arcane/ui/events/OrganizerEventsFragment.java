package com.example.arcane.ui.events;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.ArrayAdapter;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentEventsBinding;
import com.example.arcane.model.Event;
import com.example.arcane.model.Users;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.service.UserService;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private UserRepository userRepository;
    private List<Event> allEvents = new ArrayList<>(); // Store all events for filtering
    private boolean isOrganizer = false;
    
    // Filter state
    private List<String> filterCategories = new ArrayList<>();
    private Date filterDateFrom = null;
    private Date filterDateTo = null;
    
    // Available categories
    private static final String[] CATEGORIES = {"Sports", "Entertainment", "Education", "Food & Dining", "Technology"};
    private static final Map<String, String> CATEGORY_MAP = new HashMap<String, String>() {{
        put("Sports", "SPORTS");
        put("Entertainment", "ENTERTAINMENT");
        put("Education", "EDUCATION");
        put("Food & Dining", "FOOD_DINING");
        put("Technology", "TECHNOLOGY");
    }};

    /**
     * Gets the event adapter (for testing purposes).
     *
     * @return the event card adapter
     */
    public EventCardAdapter getAdapter() {
        return adapter;
    }

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
        
        // Setup filter button
        binding.filterButton.setOnClickListener(v -> showFilterDialog());

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
                    if (!isAdded() || binding == null) return;
                    
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
                            if (isAdded() && getActivity() != null) {
                                androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                                navController.navigate(R.id.navigation_create_event);
                            }
                        });
                        loadOrganizerEvents();
                    } else {
                        // User view: hide FAB, show nav buttons
                        binding.fabAddEvent.setVisibility(View.GONE);
                        binding.navButtonsContainer.setVisibility(View.VISIBLE);
                        binding.primaryNavButton.setText("Go to Global Events");
                        binding.primaryNavButton.setOnClickListener(v -> {
                            if (isAdded() && getActivity() != null) {
                                androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                                navController.navigate(R.id.navigation_global_events);
                            }
                        });
                        loadUserEvents();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    // Default to user view on failure
                    isOrganizer = false;
                    binding.fabAddEvent.setVisibility(View.GONE);
                    binding.navButtonsContainer.setVisibility(View.VISIBLE);
                    binding.primaryNavButton.setText("Go to Global Events");
                    binding.primaryNavButton.setOnClickListener(v -> {
                        if (isAdded() && getActivity() != null) {
                            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                            navController.navigate(R.id.navigation_global_events);
                        }
                    });
                    loadUserEvents();
                });
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
     * Performs search and applies filters.
     */
    private void performSearch() {
        String query = binding.searchEditText.getText() != null ? 
                binding.searchEditText.getText().toString().trim() : "";
        String queryLower = query.isEmpty() ? null : query.toLowerCase();
        
        List<Event> filtered = new ArrayList<>();
        for (Event event : allEvents) {
            // Text search (name or description)
            if (queryLower != null && !matchesQuery(event, queryLower)) {
                continue;
            }
            
            // Category filter
            if (!filterCategories.isEmpty()) {
                String eventCategory = event.getCategory();
                if (eventCategory == null || !filterCategories.contains(eventCategory)) {
                    continue;
                }
            }
            
            // Date filter
            if (filterDateFrom != null || filterDateTo != null) {
                Timestamp ts = event.getEventDate();
                if (ts == null) continue;
                Date eventDate = ts.toDate();
                if (filterDateFrom != null && eventDate.before(filterDateFrom)) continue;
                if (filterDateTo != null && eventDate.after(filterDateTo)) continue;
            }
            
            filtered.add(event);
        }
        
        adapter.setItems(filtered);
    }

    /**
     * Returns true if the event matches the query in either name or description.
     */
    private boolean matchesQuery(@NonNull Event event, @NonNull String queryLower) {
        String name = event.getEventName();
        if (name != null && name.toLowerCase().contains(queryLower)) {
            return true;
        }
        String description = event.getDescription();
        return description != null && description.toLowerCase().contains(queryLower);
    }
    
    /**
     * Shows a filter dialog for category and date filtering.
     */
    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_events, null);
        
        android.widget.AutoCompleteTextView categoryDropdown = 
            dialogView.findViewById(R.id.filter_category_dropdown);
        com.google.android.material.chip.ChipGroup categoryChips = 
            dialogView.findViewById(R.id.filter_category_chips);
        com.google.android.material.button.MaterialButton fromDateBtn = 
            dialogView.findViewById(R.id.filter_date_from_button);
        com.google.android.material.button.MaterialButton toDateBtn = 
            dialogView.findViewById(R.id.filter_date_to_button);
        
        // Setup category dropdown
        android.widget.ArrayAdapter<String> categoryAdapter = new android.widget.ArrayAdapter<>(
            requireContext(), android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        categoryDropdown.setAdapter(categoryAdapter);
        
        // Show dropdown when clicked
        categoryDropdown.setOnClickListener(v -> {
            categoryDropdown.showDropDown();
        });
        
        // Also show dropdown when focused
        categoryDropdown.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                categoryDropdown.showDropDown();
            }
        });
        
        // Load existing selected categories as chips
        refreshCategoryChips(categoryChips);
        
        // Handle category selection
        categoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = CATEGORIES[position];
            String internalCategory = CATEGORY_MAP.get(selectedCategory);
            if (internalCategory != null && !filterCategories.contains(internalCategory)) {
                filterCategories.add(internalCategory);
                refreshCategoryChips(categoryChips);
                categoryDropdown.setText(""); // Clear dropdown text
            }
        });
        
        // Set current date values
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        if (filterDateFrom != null) fromDateBtn.setText(dateFormat.format(filterDateFrom));
        if (filterDateTo != null) toDateBtn.setText(dateFormat.format(filterDateTo));
        
        // Date pickers
        Calendar cal = Calendar.getInstance();
        fromDateBtn.setOnClickListener(v -> {
            if (filterDateFrom != null) cal.setTime(filterDateFrom);
            new DatePickerDialog(requireContext(), (view, y, m, d) -> {
                cal.set(y, m, d);
                filterDateFrom = cal.getTime();
                fromDateBtn.setText(dateFormat.format(filterDateFrom));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        
        toDateBtn.setOnClickListener(v -> {
            if (filterDateTo != null) cal.setTime(filterDateTo);
            new DatePickerDialog(requireContext(), (view, y, m, d) -> {
                cal.set(y, m, d);
                filterDateTo = cal.getTime();
                toDateBtn.setText(dateFormat.format(filterDateTo));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Apply", (d, w) -> {
                if (filterDateFrom != null && filterDateTo != null && filterDateFrom.after(filterDateTo)) {
                    Toast.makeText(requireContext(), "From date must be before To date", Toast.LENGTH_SHORT).show();
                    return;
                }
                performSearch();
            })
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear", (d, w) -> {
                filterCategories.clear();
                filterDateFrom = null;
                filterDateTo = null;
                performSearch();
            })
            .create();
        
        dialog.show();
    }
    
    /**
     * Refreshes the category chips display based on selected categories.
     */
    private void refreshCategoryChips(com.google.android.material.chip.ChipGroup chipGroup) {
        chipGroup.removeAllViews();
        for (String internalCategory : filterCategories) {
            // Find display name for internal category
            String displayName = null;
            for (Map.Entry<String, String> entry : CATEGORY_MAP.entrySet()) {
                if (entry.getValue().equals(internalCategory)) {
                    displayName = entry.getKey();
                    break;
                }
            }
            if (displayName == null) continue;
            
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
            chip.setText(displayName);
            chip.setChipBackgroundColorResource(R.color.surface_alt);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            chip.setCloseIconVisible(true);
            chip.setCloseIconTint(ContextCompat.getColorStateList(requireContext(), R.color.text_primary));
            chip.setOnCloseIconClickListener(v -> {
                filterCategories.remove(internalCategory);
                refreshCategoryChips(chipGroup);
            });
            chipGroup.addView(chip);
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

    private void loadUserEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String userId = currentUser.getUid();
        userRepository.getUserById(userId)
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || binding == null || adapter == null) return;
                    
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
                                    if (!isAdded() || adapter == null) return;
                                    Event event = doc.toObject(Event.class);
                                    if (event != null) {
                                        event.setEventId(doc.getId());
                                        items.add(event);
                                    }
                                })
                                .addOnCompleteListener(task -> {
                                    if (!isAdded() || adapter == null) return;
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
     * Refreshes the events list to show newly created events.
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
     * Gets the event adapter (for testing purposes).
     *
     * @return the event card adapter
     */
    public EventCardAdapter getAdapter() {
        return adapter;
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


