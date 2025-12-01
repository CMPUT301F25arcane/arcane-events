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
import com.example.arcane.model.Decision;
import com.example.arcane.model.Event;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.EventRepository;
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
 * GlobalEventsFragment.java
 * 
 * Purpose: Displays all available events in the system for browsing and registration.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and RecyclerView with adapter pattern for list display.
 * 
 * Outstanding Issues:
 * - Event click handling is not yet implemented (placeholder comment on line 43)
 * 
 * @version 1.0
 */
public class GlobalEventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventCardAdapter adapter;
    private EventRepository eventRepository;
    private DecisionRepository decisionRepository;
    private List<Event> allEvents = new ArrayList<>();
    
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

        // Show status chips for users (only if they've joined)
        adapter.setShowStatus(true);

        // No create in global view
        binding.fabAddEvent.setVisibility(View.GONE);

        // Back to My Events (User)
        binding.navButtonsContainer.setVisibility(View.VISIBLE);
        binding.primaryNavButton.setText("Back to My Events");
        binding.primaryNavButton.setOnClickListener(v -> {
            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), com.example.arcane.R.id.nav_host_fragment_activity_main);
            navController.navigate(com.example.arcane.R.id.navigation_home);
        });

        setupSearch();
        binding.filterButton.setOnClickListener(v -> showFilterDialog());
        loadAllEvents();
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
     * Loads all available events from the repository.
     */
    private void loadAllEvents() {
        eventRepository.getAllEvents()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        items.add(event);
                    }
                    // Store all events and apply current search filter
                    allEvents = items;
                    // Load user decisions to show status
                    loadUserDecisions();
                });
    }

    /**
     * Loads user decisions for all events to display status chips.
     *
     * <p>Uses a collection group query to retrieve all decisions for the current user
     * across all events, then maps them to event IDs for status display.</p>
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
     * Called when the fragment becomes visible to the user.
     * Reloads user decisions to refresh status chips after navigation.
     */
    @Override
    public void onResume() {
        super.onResume();
        // Refresh user decisions to update status chips (e.g., after abandoning waitlist)
        if (allEvents != null && !allEvents.isEmpty()) {
            loadUserDecisions();
        }
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


