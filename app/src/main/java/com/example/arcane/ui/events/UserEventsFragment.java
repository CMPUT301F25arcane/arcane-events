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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentEventsBinding;
import com.example.arcane.model.Decision;
import com.example.arcane.model.Event;
import com.example.arcane.model.Notification;
import com.example.arcane.model.UserProfile;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.service.NotificationService;
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
  
    private NotificationService notificationService;
    private List<Event> allEvents = new ArrayList<>(); // Store all events for filtering
    private List<String> registeredEventIds = new ArrayList<>(); // Store registered event IDs for notification filtering
    private List<Notification> shownNotifications = new ArrayList<>(); // Track which notifications have been shown

    // Filter state
    private String filterLocation = null;
    private Date filterDateFrom = null;
    private Date filterDateTo = null;

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
        notificationService = new NotificationService();
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

        setupSearch();
        binding.filterButton.setOnClickListener(v -> showFilterDialog());
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
     * Performs search and applies filters.
     */
    private void performSearch() {
        String query = binding.searchEditText.getText() != null ? 
                binding.searchEditText.getText().toString().trim() : "";
        String queryLower = query.isEmpty() ? null : query.toLowerCase();
        
        List<Event> filtered = new ArrayList<>();
        for (Event event : allEvents) {
            // Text search
            if (queryLower != null && 
                (event.getEventName() == null || !event.getEventName().toLowerCase().contains(queryLower))) {
                continue;
            }
            
            // Location filter
            if (filterLocation != null && !filterLocation.isEmpty()) {
                String loc = event.getLocation();
                if (loc == null || !loc.toLowerCase().contains(filterLocation.toLowerCase())) {
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
     * Shows a simple filter dialog for location and date filtering.
     */
    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter_events, null);
        
        com.google.android.material.textfield.TextInputEditText locationInput = 
            dialogView.findViewById(R.id.filter_location_edit_text);
        com.google.android.material.button.MaterialButton fromDateBtn = 
            dialogView.findViewById(R.id.filter_date_from_button);
        com.google.android.material.button.MaterialButton toDateBtn = 
            dialogView.findViewById(R.id.filter_date_to_button);
        
        // Set current values
        if (filterLocation != null) {
            locationInput.setText(filterLocation);
        }
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
                String loc = locationInput.getText() != null ? locationInput.getText().toString().trim() : "";
                filterLocation = loc.isEmpty() ? null : loc;
                if (filterDateFrom != null && filterDateTo != null && filterDateFrom.after(filterDateTo)) {
                    Toast.makeText(requireContext(), "From date must be before To date", Toast.LENGTH_SHORT).show();
                    return;
                }
                performSearch();
            })
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Clear", (d, w) -> {
                filterLocation = null;
                filterDateFrom = null;
                filterDateTo = null;
                performSearch();
            })
            .create();
        
        dialog.show();
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
                    if (!isAdded() || binding == null || adapter == null) return;
                    
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null || profile.getRegisteredEventIds() == null || profile.getRegisteredEventIds().isEmpty()) {
                        allEvents = new ArrayList<>();
                        registeredEventIds = new ArrayList<>();
                        adapter.setItems(allEvents);
                        // Still check for notifications even if no events (show all unread)
                        checkAndShowNotifications();
                        return;
                    }

                    List<String> eventIds = profile.getRegisteredEventIds();
                    registeredEventIds = new ArrayList<>(eventIds); // Store for notification filtering
                    List<Event> items = new ArrayList<>();

                    final int[] remaining = {eventIds.size()};
                    if (remaining[0] == 0) {
                        // No events to load, check notifications immediately
                        checkAndShowNotifications();
                        return;
                    }
                    
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
                    if (!isAdded() || binding == null || adapter == null) return;
                    
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
                    // Check for notifications after events are loaded
                    checkAndShowNotifications();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null || adapter == null) return;
                    // On failure, just show events without status
                    performSearch();
                    // Still check for notifications even if decisions fail
                    checkAndShowNotifications();
                });
    }

    private void checkAndShowNotifications() {
        android.util.Log.d("UserEventsFragment", "checkAndShowNotifications called, isAdded: " + isAdded());
        
        if (!isAdded()) {
            android.util.Log.d("UserEventsFragment", "Skipping notification check - not added");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            android.util.Log.d("UserEventsFragment", "No current user");
            return;
        }

        String userId = currentUser.getUid();
        android.util.Log.d("UserEventsFragment", "Checking notifications for user: " + userId + ", registeredEventIds: " + registeredEventIds.size());
        
        // Get unread notifications
        notificationService.getUnreadNotifications(userId)
                .addOnSuccessListener(querySnapshot -> {
                    android.util.Log.d("UserEventsFragment", "Got notifications, count: " + querySnapshot.size());
                    
                    if (!isAdded() || binding == null) {
                        android.util.Log.d("UserEventsFragment", "Fragment not added or binding null");
                        return;
                    }
                    
                    // Show all unread notifications for events the user is registered for
                    List<Notification> notificationsToShow = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Notification notification = doc.toObject(Notification.class);
                        notification.setNotificationId(doc.getId());
                        
                        android.util.Log.d("UserEventsFragment", "Checking notification: " + notification.getNotificationId() + 
                                ", eventId: " + notification.getEventId() + 
                                ", read: " + notification.getRead() + 
                                ", in registered list: " + registeredEventIds.contains(notification.getEventId()));
                        
                        // Only show notifications for events the user is registered for
                        // If registeredEventIds is empty, show all notifications (user might have notifications but no events loaded yet)
                        boolean shouldShow = !Boolean.TRUE.equals(notification.getRead()) 
                                && notification.getEventId() != null
                                && (registeredEventIds.isEmpty() || registeredEventIds.contains(notification.getEventId()))
                                && !shownNotifications.contains(notification); // Don't show if already shown
                        
                        if (shouldShow) {
                            notificationsToShow.add(notification);
                        }
                    }
                    
                    // Show all notifications
                    for (Notification notification : notificationsToShow) {
                        showNotificationBanner(notification);
                        shownNotifications.add(notification);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UserEventsFragment", "Failed to get notifications: " + e.getMessage());
                });
    }

    private void showNotificationBanner(Notification notification) {
        android.util.Log.d("UserEventsFragment", "showNotificationBanner called");
        
        // Ensure we're on the main thread and view is ready
        if (getActivity() == null || !isAdded() || binding == null) {
            android.util.Log.d("UserEventsFragment", "Cannot show notification - activity, fragment, or binding not ready");
            return;
        }

        // Use handler to ensure we're on main thread and view is ready
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (!isAdded() || binding == null) {
                android.util.Log.d("UserEventsFragment", "Cannot show notification - not added or binding null");
                return;
            }

            android.util.Log.d("UserEventsFragment", "Showing notification banner with title: " + notification.getTitle());
            
            try {
                // Get the notification container
                android.widget.LinearLayout notificationContainer = binding.getRoot().findViewById(R.id.notification_container);
                if (notificationContainer == null) {
                    android.util.Log.e("UserEventsFragment", "Notification container not found in layout");
                    return;
                }
                
                // Inflate a new notification banner view
                View notificationBanner = LayoutInflater.from(getContext())
                        .inflate(R.layout.layout_notification_banner, notificationContainer, false);
                
                android.widget.TextView titleView = notificationBanner.findViewById(R.id.notification_title);
                android.widget.TextView messageView = notificationBanner.findViewById(R.id.notification_message);
                android.widget.TextView timestampView = notificationBanner.findViewById(R.id.notification_timestamp);
                android.widget.ImageButton closeButton = notificationBanner.findViewById(R.id.notification_close);
                
                // Customize display based on notification type/status
                String notificationType = notification.getType();
                String displayTitle = notification.getTitle() != null ? notification.getTitle() : "Notification";
                String displayMessage = notification.getMessage() != null ? notification.getMessage() : "";
                int backgroundColor = getResources().getColor(R.color.brand_primary, null);
                
                // Set appropriate background color based on type
                // Title and message should already be set appropriately from the notification
                if ("INVITED".equals(notificationType)) {
                    backgroundColor = getResources().getColor(R.color.status_won_bg, null);
                } else if ("LOST".equals(notificationType)) {
                    backgroundColor = getResources().getColor(R.color.status_lost_bg, null);
                } else if ("ACCEPTED".equals(notificationType)) {
                    backgroundColor = getResources().getColor(R.color.status_accepted_bg, null);
                } else if ("CANCELLED".equals(notificationType)) {
                    backgroundColor = getResources().getColor(R.color.status_lost_bg, null);
                }
                
                if (titleView != null) titleView.setText(displayTitle);
                if (messageView != null) messageView.setText(displayMessage);
                
                if (timestampView != null && notification.getTimestamp() != null) {
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
                    timestampView.setText(dateFormat.format(notification.getTimestamp().toDate()));
                }
                
                // Set background color based on notification type
                notificationBanner.setBackgroundColor(backgroundColor);
                
                // Store notification ID in view tag for later reference
                notificationBanner.setTag(notification.getNotificationId());
                
                // Handle close button click
                if (closeButton != null) {
                    closeButton.setOnClickListener(v -> {
                        android.util.Log.d("UserEventsFragment", "Notification close button clicked");
                        markNotificationAsRead(notification);
                        removeNotificationBanner(notificationBanner);
                    });
                }
                
                // Handle clicking on the notification itself
                notificationBanner.setOnClickListener(v -> {
                    android.util.Log.d("UserEventsFragment", "Notification clicked");
                    markNotificationAsRead(notification);
                    removeNotificationBanner(notificationBanner);
                });
                
                // Add to container
                notificationContainer.addView(notificationBanner);
                
                // Show container if it was hidden
                if (notificationContainer.getVisibility() != View.VISIBLE) {
                    notificationContainer.setVisibility(View.VISIBLE);
                }
                
                // Animate in
                notificationBanner.setAlpha(0f);
                notificationBanner.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start();
                
                android.util.Log.d("UserEventsFragment", "Notification banner shown successfully");
            } catch (Exception e) {
                android.util.Log.e("UserEventsFragment", "Error showing notification: " + e.getMessage(), e);
            }
        });
    }
    
    private void removeNotificationBanner(View notificationBanner) {
        if (binding == null || !isAdded() || notificationBanner == null) return;
        
        android.widget.LinearLayout notificationContainer = binding.getRoot().findViewById(R.id.notification_container);
        if (notificationContainer == null) return;
        
        notificationBanner.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    notificationContainer.removeView(notificationBanner);
                    // Hide container if no more notifications
                    if (notificationContainer.getChildCount() == 0) {
                        notificationContainer.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void markNotificationAsRead(Notification notification) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || notification.getNotificationId() == null) {
            return;
        }

        String userId = currentUser.getUid();
        notificationService.markNotificationRead(userId, notification.getNotificationId())
                .addOnSuccessListener(aVoid -> {
                    notification.setRead(true);
                });
    }

    /**
     * Called when the view hierarchy is being removed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        shownNotifications.clear(); // Clear shown notifications when view is destroyed
        binding = null;
    }
}


