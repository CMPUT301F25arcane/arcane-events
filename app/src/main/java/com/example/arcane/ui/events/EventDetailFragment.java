/**
 * EventDetailFragment.java
 * 
 * Purpose: Displays detailed information about an event and provides different views
 * for organizers and regular users, including waitlist management and lottery functionality.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and Service Layer for business logic.
 * 
 * Outstanding Issues:
 * - QR code functionality is not yet implemented
 * - Send notification functionality is not yet implemented
 * - Edit event functionality is not yet implemented
 * - Poster image loading is not yet implemented (uses placeholder)
 * 
 * @version 1.0
 */
package com.example.arcane.ui.events;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.DialogSendNotificationBinding;
import com.example.arcane.databinding.FragmentEventDetailBinding;
import com.example.arcane.model.Decision;
import com.example.arcane.model.Event;
import com.example.arcane.model.WaitingListEntry;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.WaitingListRepository;
import com.example.arcane.service.UserService;
import com.example.arcane.util.SessionLocationManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment for displaying event details with role-based views.
 *
 * <p>Shows different UI for organizers (lottery management, entrants list) and regular users
 * (join waitlist, accept/decline invitations). Handles event registration, waitlist management,
 * and lottery drawing functionality.</p>
 *
 * @version 1.0
 */
public class EventDetailFragment extends Fragment implements OnMapReadyCallback {

    private FragmentEventDetailBinding binding;
    private EventRepository eventRepository;
    private WaitingListRepository waitingListRepository;
    private DecisionRepository decisionRepository;
    private UserService userService;
    private com.example.arcane.service.EventService eventService;
    
    private String eventId;
    private Event currentEvent;
    private boolean isOrganizer = false;
    private boolean isAdmin = false;
    private boolean isUserJoined = false;
    private boolean isWaitlistFull = false;
    private String userStatus = null; // WAITING, WON, LOST, ACCEPTED, DECLINED
    private String userDecision = null; // none, accepted, declined
    private String waitingListEntryId = null;
    private String decisionId = null;
    private boolean organizerViewSetup = false; // Prevent multiple listener setups
   
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEventDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (eventId == null) {
            Toast.makeText(requireContext(), "Event ID is required", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        eventRepository = new EventRepository();
        waitingListRepository = new WaitingListRepository();
        decisionRepository = new DecisionRepository();
        userService = new UserService();
        eventService = new com.example.arcane.service.EventService();

        // Add bottom padding to account for navbar
        setupBottomPadding();

        // Check user role and load event
        checkUserRoleAndLoadEvent();
    }

    private void setupBottomPadding() {
        // Apply window insets to add padding for bottom navigation bar
        // This ensures the content inside NestedScrollView has proper bottom padding
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int bottomInset = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
            // The root is NestedScrollView, get its first child (the LinearLayout with content)
            if (v instanceof androidx.core.widget.NestedScrollView) {
                androidx.core.widget.NestedScrollView scrollView = (androidx.core.widget.NestedScrollView) v;
                if (scrollView.getChildCount() > 0) {
                    View contentView = scrollView.getChildAt(0);
                    // Bottom nav bar is typically 56dp, add extra padding for comfort
                    int bottomNavHeight = (int) (56 * getResources().getDisplayMetrics().density);
                    int extraPadding = (int) (16 * getResources().getDisplayMetrics().density);
                    int totalBottomPadding = Math.max(bottomInset, bottomNavHeight) + extraPadding;
                    contentView.setPadding(
                        contentView.getPaddingLeft(),
                        contentView.getPaddingTop(),
                        contentView.getPaddingRight(),
                        totalBottomPadding
                    );
                }
            }
            return insets;
        });
    }

    private void checkUserRoleAndLoadEvent() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateBack();
            return;
        }

        // Check role from SharedPreferences (same pattern as EventsRouterFragment)
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        String role = prefs.getString("user_role", null);
        isOrganizer = isOrganizerRole(role);
        isAdmin = isAdminRole(role);

        // Load event - we'll check if user is organizer of this specific event after loading
        loadEvent();
    }

    private boolean isOrganizerRole(@Nullable String role) {
        if (role == null) {
            return false;
        }
        String roleUpper = role.toUpperCase();
        return "ORGANIZER".equals(roleUpper) || "ORGANISER".equals(roleUpper);
    }

    private boolean isAdminRole(@Nullable String role) {
        if (role == null) {
            return false;
        }
        String roleUpper = role.toUpperCase();
        return "ADMIN".equals(roleUpper);
    }

    private void loadEvent() {
        eventRepository.getEventById(eventId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded() || binding == null) return;
                    
                    if (documentSnapshot.exists()) {
                        currentEvent = documentSnapshot.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setEventId(documentSnapshot.getId());
                            populateEventDetails();
                            
                            // Check if current user is admin first
                            if (isAdmin) {
                                // Admin - show admin view with delete button
                                setupAdminView();
                            } else {
                                // Check if current user is the organizer of this event
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null && currentUser.getUid().equals(currentEvent.getOrganizerId())) {
                                    // User is the organizer - show organizer view
                                    isOrganizer = true;
                                    setupOrganizerView();
                                } else if (isOrganizer) {
                                    // User is organizer but not of this event - still show organizer view
                                    setupOrganizerView();
                                } else {
                                    // Regular user - load user status
                                    loadUserStatus();
                                }
                            }
                        } else {
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(getContext(), "Failed to load event", Toast.LENGTH_SHORT).show();
                            }
                            navigateBack();
                        }
                    } else {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(), "Event not found", Toast.LENGTH_SHORT).show();
                        }
                        navigateBack();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    navigateBack();
                });
    }

    private void populateEventDetails() {
        if (currentEvent == null || binding == null || !isAdded()) return;

        // Title
        binding.eventTitle.setText(currentEvent.getEventName() != null ? currentEvent.getEventName() : "Untitled Event");

        // Description
        if (currentEvent.getDescription() != null) {
            binding.descriptionBody.setText(currentEvent.getDescription());
        }

        // Date
        if (currentEvent.getEventDate() != null) {
            Date date = currentEvent.getEventDate().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            binding.eventDateText.setText(sdf.format(date));
        }

        // Location - show "Unknown" for legacy events without location
        String location = currentEvent.getLocation();
        if (location != null && !location.isEmpty()) {
            binding.eventLocationText.setText(location);
        } else {
            binding.eventLocationText.setText("Unknown");
        }

        // Cost
        if (currentEvent.getCost() != null) {
            binding.eventPriceText.setText(String.format(Locale.getDefault(), "$%.2f CAD", currentEvent.getCost()));
        }

        // Load organizer name (for both user and organizer views)
        // Note: Organizer name field removed from UI to match design
        // loadOrganizerName();

        // Load event image
        loadEventImage();

        // Setup map if event has geolocation
        setupEventMap();
    }

    /**
     * Sets up the event location map if geolocation is available.
     */
    private void setupEventMap() {
        if (currentEvent == null || binding == null || !isAdded()) return;

        // Check if event has geolocation
        if (currentEvent.getGeolocation() == null) {
            // Hide map card if no geolocation
            binding.eventMapCard.setVisibility(View.GONE);
            return;
        }

        // Show map card
        binding.eventMapCard.setVisibility(View.VISIBLE);

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.eventMapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Called when the map is ready to be used.
     * Displays the event location on the map.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        if (currentEvent == null || currentEvent.getGeolocation() == null) return;

        GeoPoint geoPoint = currentEvent.getGeolocation();
        LatLng eventLatLng = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());

        // Add marker for event location
        googleMap.addMarker(new MarkerOptions()
                .position(eventLatLng)
                .title(currentEvent.getEventName() != null ? currentEvent.getEventName() : "Event Location")
                .snippet("Event venue")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Center map on event location with appropriate zoom
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLatLng, 15f));

        // Configure map settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);
    }

    private void loadEventImage() {
        if (currentEvent == null || currentEvent.getPosterImageUrl() == null || currentEvent.getPosterImageUrl().isEmpty()) {
            return;
        }
        if (binding == null || !isAdded()) return;

        try {
            // Check if it's a base64 string (starts with data:image or is a long base64 string)
            String imageData = currentEvent.getPosterImageUrl();
            
            // If it's a base64 string, decode and display it
            if (!imageData.startsWith("http://") && !imageData.startsWith("https://")) {
                byte[] imageBytes = android.util.Base64.decode(imageData, android.util.Base64.NO_WRAP);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null && binding != null && isAdded()) {
                    binding.headerImage.setImageBitmap(bitmap);
                }
            } else {
                // If it's a URL, you could use Glide/Picasso here
                // For now, we'll just handle base64
            }
        } catch (Exception e) {
            // If decoding fails, image might be in a different format
            // Silently fail - image will just not display
        }
    }

    private void loadUserStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Check if user is in waiting list
        waitingListRepository.checkUserInWaitingList(eventId, userId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || binding == null) return;
                    
                    if (!querySnapshot.isEmpty()) {
                        isUserJoined = true;
                        // Get the entry ID
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                            waitingListEntryId = doc.getId();
                            break; // Take first match
                        }

                        // Now check decision/status
                        // Note: According to spec, status should be in waitingList entry,
                        // but current model uses Decision. We'll check Decision for now.
                        decisionRepository.getDecisionForUser(eventId, userId)
                                .addOnSuccessListener(decisionSnapshot -> {
                                    if (!isAdded() || binding == null) return;
                                    
                                    if (!decisionSnapshot.isEmpty()) {
                                        for (QueryDocumentSnapshot doc : decisionSnapshot) {
                                            Decision decision = doc.toObject(Decision.class);
                                            decisionId = doc.getId();
                                            String decisionStatus = decision.getStatus(); // PENDING, INVITED, ACCEPTED, DECLINED
                                            
                                            // Map Decision status to user status per spec:
                                            // Decision status "PENDING" means user is WAITING
                                            // Decision status "INVITED" means user WON (needs to accept/decline)
                                            // Decision status "ACCEPTED" means user ACCEPTED
                                            // Decision status "DECLINED" means user DECLINED
                                            // Note: WON/LOST status should come from waitingList entry status (to be added in Step 10)
                                            // For now, we use Decision status "INVITED" to represent "WON"
                                            if ("PENDING".equals(decisionStatus)) {
                                                userStatus = "WAITING";
                                                userDecision = "none";
                                            } else if ("INVITED".equals(decisionStatus)) {
                                                // INVITED means user won the lottery - they need to accept/decline
                                                userStatus = "WON";
                                                userDecision = "none";
                                            } else if ("ACCEPTED".equals(decisionStatus)) {
                                                userStatus = "ACCEPTED";
                                                userDecision = "accepted";
                                            } else if ("DECLINED".equals(decisionStatus) || "CANCELLED".equals(decisionStatus)) {
                                                userStatus = "DECLINED";
                                                userDecision = "declined";
                                            } else if ("LOST".equals(decisionStatus)) {
                                                userStatus = "LOST";
                                                userDecision = "none";
                                            } else {
                                                // Default to WAITING if unknown status
                                                userStatus = "WAITING";
                                                userDecision = "none";
                                            }
                                            break;
                                        }
                                    } else {
                                        // In waiting list but no decision yet - default to WAITING
                                        userStatus = "WAITING";
                                        userDecision = "none";
                                    }
                                    setupUserView();
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded() || binding == null) return;
                                    // If decision check fails, default to WAITING
                                    userStatus = "WAITING";
                                    userDecision = "none";
                                    setupUserView();
                                });
                    } else {
                        // Not in waiting list - make sure entryId and decisionId are cleared
                        waitingListEntryId = null;
                        decisionId = null;
                        isUserJoined = false;
                        // Check if waitlist is full before showing join button
                        checkWaitlistFull();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error checking status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    setupUserView();
                });
    }

    private void setupOrganizerView() {
        if (binding == null || !isAdded()) return;
        
        // Hide user-specific UI
        binding.statusDecisionContainer.setVisibility(View.GONE);
        binding.abandonButtonContainer.setVisibility(View.GONE);
        binding.acceptDeclineButtonsContainer.setVisibility(View.GONE);
        binding.joinButtonContainer.setVisibility(View.GONE);
        binding.totalEntrantsContainer.setVisibility(View.GONE);

        // Show organizer-specific UI
        // Lottery status and send notification are in the same horizontal layout
        binding.lotteryStatusAndNotificationContainer.setVisibility(View.VISIBLE);
        binding.editEventButton.setVisibility(View.VISIBLE);
        // Hide standalone send notification container (we use the one in the horizontal layout)
        binding.sendNotificationContainer.setVisibility(View.GONE);
        binding.organizerActionButtons.setVisibility(View.VISIBLE);

        // Set lottery status with colored text
        updateLotteryStatusDisplay();

        // Setup organizer buttons only once to prevent multiple listeners
        if (!organizerViewSetup) {
            binding.qrCodeButton.setOnClickListener(v -> navigateToQrPage());

            binding.entrantsButton.setOnClickListener(v -> {
                if (eventId != null) {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    Bundle args = new Bundle();
                    args.putString("eventId", eventId);
                    navController.navigate(R.id.navigation_entrants, args);
                }
            });

            binding.drawLotteryButton.setOnClickListener(v -> {
                android.util.Log.d("EventDetailFragment", "Draw Lottery button clicked");
                handleDrawLottery();
            });
            
            organizerViewSetup = true;
        }

        // Send notification button (in horizontal layout with lottery status)
        binding.sendNotificationButton.setOnClickListener(v -> showSendNotificationDialog());

        // Also handle standalone send notification button if it exists
        if (binding.sendNotificationButtonStandalone != null) {
            binding.sendNotificationButtonStandalone.setOnClickListener(v -> showSendNotificationDialog());
        }

        // Edit button is now a MaterialCardView, set click listener
        binding.editEventButton.setOnClickListener(v -> {
            if (eventId == null) {
                Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            navController.navigate(R.id.navigation_edit_event, args);
        });
    }

    private void navigateToQrPage() {
        if (eventId == null) {
            Toast.makeText(requireContext(), "Event not loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        navController.navigate(R.id.navigation_qr_code, args);
    }

    // Organizer name field removed from UI to match design
    // private void loadOrganizerName() {
    //     if (currentEvent == null || currentEvent.getOrganizerId() == null) {
    //         return;
    //     }
    //
    //     userService.getUserById(currentEvent.getOrganizerId())
    //             .addOnSuccessListener(documentSnapshot -> {
    //                 if (documentSnapshot.exists()) {
    //                     // Try UserProfile first
    //                     com.example.arcane.model.UserProfile userProfile = documentSnapshot.toObject(com.example.arcane.model.UserProfile.class);
    //                     if (userProfile != null && userProfile.getName() != null) {
    //                         binding.organizerNameText.setText(userProfile.getName());
    //                         return;
    //                     }
    //
    //                     // Try Users model if UserProfile didn't work
    //                     com.example.arcane.model.Users users = documentSnapshot.toObject(com.example.arcane.model.Users.class);
    //                     if (users != null && users.getName() != null) {
    //                         binding.organizerNameText.setText(users.getName());
    //                         return;
    //                     }
    //                 }
    //                 // Fallback to "Organizer" if name not found
    //                 binding.organizerNameText.setText("Organizer");
    //             })
    //             .addOnFailureListener(e -> {
    //                 // On failure, show default text
    //                 binding.organizerNameText.setText("Organizer");
    //             });
    // }

    private void setupAdminView() {
        if (binding == null || !isAdded()) return;
        
        // Hide organizer-specific UI
        binding.lotteryStatusAndNotificationContainer.setVisibility(View.GONE);
        binding.editEventButton.setVisibility(View.GONE);
        binding.sendNotificationContainer.setVisibility(View.GONE);
        binding.organizerActionButtons.setVisibility(View.GONE);
        
        // Hide user-specific UI
        binding.statusDecisionContainer.setVisibility(View.GONE);
        binding.abandonButtonContainer.setVisibility(View.GONE);
        binding.acceptDeclineButtonsContainer.setVisibility(View.GONE);
        binding.totalEntrantsContainer.setVisibility(View.GONE);
        
        // Show delete button for admin
        binding.joinButtonContainer.setVisibility(View.VISIBLE);
        binding.joinButton.setText("Delete Event");
        binding.joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
            getResources().getColor(R.color.lottery_status_closed, null)));
        binding.joinButton.setOnClickListener(v -> handleDeleteEvent());
    }

    private void setupUserView() {
        if (binding == null || !isAdded()) return;
        
        // Only show user view for regular users (entrants), not organizers or admins
        if (isOrganizer || isAdmin) {
            // If somehow we got here as organizer/admin, hide join button
            binding.joinButtonContainer.setVisibility(View.GONE);
            return;
        }
        
        // Hide organizer-specific UI
        binding.lotteryStatusAndNotificationContainer.setVisibility(View.GONE);
        binding.editEventButton.setVisibility(View.GONE);
        binding.sendNotificationContainer.setVisibility(View.GONE);
        binding.organizerActionButtons.setVisibility(View.GONE);

        // Show user-specific UI
        binding.statusDecisionContainer.setVisibility(View.VISIBLE);
        
        // Show total entrants count for entrants
        binding.totalEntrantsContainer.setVisibility(View.VISIBLE);
        loadTotalEntrantsCount();

        if (isUserJoined) {
            // User has joined - show status and decision
            showUserStatus();
            setupUserActionButtons();
        } else {
            // User not joined - show Join button or Waitlist Full button
            binding.abandonButtonContainer.setVisibility(View.GONE);
            binding.acceptDeclineButtonsContainer.setVisibility(View.GONE);
            binding.joinButtonContainer.setVisibility(View.VISIBLE);

            // Check if lottery has been drawn
            checkIfLotteryDrawnAndUpdateUI();
        }
    }

    private void checkIfLotteryDrawnAndUpdateUI() {
        if (binding == null || !isAdded()) return;
        
        // Check if lottery has been drawn by checking for INVITED or LOST decisions
        decisionRepository.getDecisionsByStatus(eventId, "INVITED")
                .continueWithTask(invitedTask -> {
                    boolean hasInvited = invitedTask.isSuccessful() && !invitedTask.getResult().isEmpty();
                    if (hasInvited) {
                        // Lottery has been drawn
                        return com.google.android.gms.tasks.Tasks.forResult(true);
                    }
                    // Check for LOST status as well
                    return decisionRepository.getDecisionsByStatus(eventId, "LOST")
                            .continueWith(lostTask -> {
                                boolean hasLost = lostTask.isSuccessful() && !lostTask.getResult().isEmpty();
                                return hasInvited || hasLost;
                            });
                })
                .addOnSuccessListener(lotteryDrawn -> {
                    if (binding == null || !isAdded()) return;
                    
                    if (lotteryDrawn != null && lotteryDrawn) {
                        // Lottery has been drawn - disable join button
                        binding.joinButton.setText("Lottery Drawn");
                        binding.joinButton.setEnabled(false);
                        binding.joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.lottery_status_closed, null)));
                        binding.joinButton.setOnClickListener(null);
                    } else {
                        // Lottery not drawn - check waitlist full status
                        if (isWaitlistFull) {
                            // Waitlist is full - show orange disabled button
                            binding.joinButton.setText("Waitlist Full");
                            binding.joinButton.setEnabled(false);
                            binding.joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                getResources().getColor(R.color.status_declined, null)));
                            binding.joinButton.setOnClickListener(null);
                        } else {
                            // Waitlist has space - show blue Join button
                            binding.joinButton.setText("Join Waitlist");
                            binding.joinButton.setEnabled(true);
                            binding.joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                getResources().getColor(R.color.brand_primary, null)));
                            binding.joinButton.setOnClickListener(v -> handleJoinWaitlist());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // On failure, fall back to waitlist full check
                    if (binding == null || !isAdded()) return;
                    
                    if (isWaitlistFull) {
                        binding.joinButton.setText("Waitlist Full");
                        binding.joinButton.setEnabled(false);
                        binding.joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.status_declined, null)));
                        binding.joinButton.setOnClickListener(null);
                    } else {
                        binding.joinButton.setText("Join Waitlist");
                        binding.joinButton.setEnabled(true);
                        binding.joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            getResources().getColor(R.color.brand_primary, null)));
                        binding.joinButton.setOnClickListener(v -> handleJoinWaitlist());
                    }
                });
    }

    private void checkWaitlistFull() {
        if (currentEvent == null) {
            isWaitlistFull = false;
            // Still need to show the button if user hasn't joined
            if (!isUserJoined) {
                setupUserView();
            }
            return;
        }

        // Check if maxEntrants is set
        if (currentEvent.getMaxEntrants() == null || currentEvent.getMaxEntrants() <= 0) {
            isWaitlistFull = false;
            // Still need to show the button if user hasn't joined
            if (!isUserJoined) {
                setupUserView();
            }
            return;
        }

        // Get current waiting list size (only count valid users)
        eventService.getValidWaitingListCount(eventId)
                .addOnSuccessListener(validCount -> {
                    isWaitlistFull = validCount >= currentEvent.getMaxEntrants();
                    // Update UI if already set up
                    if (!isUserJoined) {
                        setupUserView();
                    }
                })
                .addOnFailureListener(e -> {
                    // On failure, assume not full
                    isWaitlistFull = false;
                    // Still need to show the button if user hasn't joined
                    if (!isUserJoined) {
                        setupUserView();
                    }
                });
    }

    /**
     * Loads and displays the total number of entrants on the waiting list.
     * This is shown to entrants when they view an event.
     */
    private void loadTotalEntrantsCount() {
        if (eventId == null || binding == null || !isAdded()) {
            return;
        }

        // Get current waiting list count (only count valid users)
        eventService.getValidWaitingListCount(eventId)
                .addOnSuccessListener(count -> {
                    if (binding != null && isAdded()) {
                        binding.totalEntrantsCount.setText(String.valueOf(count));
                    }
                })
                .addOnFailureListener(e -> {
                    // On failure, show 0 or error message
                    if (binding != null && isAdded()) {
                        binding.totalEntrantsCount.setText("0");
                    }
                });
    }

    private void showUserStatus() {
        if (binding == null || !isAdded()) return;
        
        if (userStatus == null) {
            binding.statusChip.setVisibility(View.GONE);
            binding.statusLabel.setVisibility(View.GONE);
            return;
        }

        // Set status chip
        String statusText = userStatus.toUpperCase();
        binding.statusChip.setText(statusText);
        binding.statusChip.setVisibility(View.VISIBLE);

        // Set status color based on status
        switch (userStatus.toUpperCase()) {
            case "WAITING":
                binding.statusChip.setTextColor(getResources().getColor(R.color.status_waiting, null));
                binding.statusChip.setBackgroundColor(getResources().getColor(R.color.status_waiting_bg, null));
                break;
            case "WON":
                binding.statusChip.setTextColor(getResources().getColor(R.color.status_won, null));
                binding.statusChip.setBackgroundColor(getResources().getColor(R.color.status_won_bg, null));
                break;
            case "LOST":
                binding.statusChip.setTextColor(getResources().getColor(R.color.status_lost, null));
                binding.statusChip.setBackgroundColor(getResources().getColor(R.color.status_lost_bg, null));
                break;
            case "ACCEPTED":
                binding.statusChip.setTextColor(getResources().getColor(R.color.status_accepted, null));
                binding.statusChip.setBackgroundColor(getResources().getColor(R.color.status_accepted_bg, null));
                break;
            case "DECLINED":
                binding.statusChip.setTextColor(getResources().getColor(R.color.status_declined, null));
                binding.statusChip.setBackgroundColor(getResources().getColor(R.color.status_declined_bg, null));
                break;
            case "ABANDONED":
                binding.statusChip.setTextColor(getResources().getColor(R.color.status_abandoned, null));
                binding.statusChip.setBackgroundColor(getResources().getColor(R.color.status_abandoned_bg, null));
                break;
            default:
                binding.statusChip.setTextColor(getResources().getColor(R.color.status_pending, null));
                binding.statusChip.setBackgroundColor(getResources().getColor(R.color.status_pending_bg, null));
        }

        // Show decision if applicable
        if (userDecision != null && !"none".equals(userDecision)) {
            binding.decisionRow.setVisibility(View.VISIBLE);
            String decisionText = userDecision.toUpperCase();
            binding.decisionChip.setText(decisionText);
        } else {
            binding.decisionRow.setVisibility(View.GONE);
        }
    }

    private void setupUserActionButtons() {
        if (binding == null || !isAdded()) return;
        
        // Hide all button containers first
        binding.joinButtonContainer.setVisibility(View.GONE);
        binding.abandonButtonContainer.setVisibility(View.GONE);
        binding.acceptDeclineButtonsContainer.setVisibility(View.GONE);

        // Reset button states
        binding.joinButton.setEnabled(true);
        binding.joinButton.setText("Join Waitlist");
        binding.abandonButton.setEnabled(true);
        binding.abandonButton.setText("Abandon Waitlist");

        if ("WAITING".equals(userStatus)) {
            // Show Abandon button
            binding.abandonButtonContainer.setVisibility(View.VISIBLE);
            binding.abandonButton.setOnClickListener(v -> handleAbandonWaitlist());
        } else if ("WON".equals(userStatus) && ("none".equals(userDecision) || userDecision == null)) {
            // Show Accept/Decline buttons (actions to be implemented later)
            binding.acceptDeclineButtonsContainer.setVisibility(View.VISIBLE);
            binding.acceptButton.setOnClickListener(v -> showAcceptInvitationDialog());
            binding.declineButton.setOnClickListener(v -> handleDeclineWin());
        }
        // For LOST, ACCEPTED, DECLINED, ABANDONED - no buttons shown
    }

    private void handleJoinWaitlist() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in to join", Toast.LENGTH_SHORT).show();
            return;
        }

        showLotteryGuidelinesDialog();
    }

    private void showLotteryGuidelinesDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_lottery_guidelines, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(R.id.got_it_button).setOnClickListener(v -> {
            dialog.dismiss();
            proceedWithJoinWaitlist();
        });

        dialog.show();
    }

    private void proceedWithJoinWaitlist() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in to join", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Check if lottery has already been drawn
        // If there are any decisions with status "INVITED" or "LOST", the lottery has been drawn
        decisionRepository.getDecisionsByStatus(eventId, "INVITED")
                .continueWithTask(invitedTask -> {
                    boolean hasInvited = invitedTask.isSuccessful() && !invitedTask.getResult().isEmpty();
                    if (hasInvited) {
                        // Lottery has been drawn (winners exist)
                        return com.google.android.gms.tasks.Tasks.forResult(true);
                    }
                    // Check for LOST status as well
                    return decisionRepository.getDecisionsByStatus(eventId, "LOST")
                            .continueWith(lostTask -> {
                                boolean hasLost = lostTask.isSuccessful() && !lostTask.getResult().isEmpty();
                                return hasInvited || hasLost;
                            });
                })
                .addOnSuccessListener(lotteryDrawn -> {
                    if (lotteryDrawn != null && lotteryDrawn) {
                        // Lottery has been drawn, prevent joining
                        Toast.makeText(requireContext(), "The lottery has already been drawn. You cannot join this event.", Toast.LENGTH_LONG).show();
                        if (binding != null && isAdded()) {
                            binding.joinButton.setEnabled(true);
                            binding.joinButton.setText("Join Waitlist");
                        }
                        return;
                    }
                    
                    // Lottery not drawn yet, proceed with joining
                    proceedWithJoinWaitlistInternal(userId);
                })
                .addOnFailureListener(e -> {
                    // If check fails, allow joining (fail open for better UX)
                    proceedWithJoinWaitlistInternal(userId);
                });
    }

    private void proceedWithJoinWaitlistInternal(String userId) {
        // Get session location (captured at login)
        com.google.firebase.firestore.GeoPoint sessionLocation = SessionLocationManager.getSessionLocation(requireContext());
        Log.d("EventDetailFragment", "DEBUG: Session location: " + (sessionLocation != null ? "EXISTS" : "NULL"));
        if (sessionLocation != null) {
            Log.d("EventDetailFragment", "DEBUG: Session location coordinates: " + 
                  sessionLocation.getLatitude() + ", " + sessionLocation.getLongitude());
        }
        
        // Log event geolocation requirement
        if (currentEvent != null) {
            Log.d("EventDetailFragment", "DEBUG: Event geolocationRequired: " + currentEvent.getGeolocationRequired());
        }

        // Disable button during operation
        binding.joinButton.setEnabled(false);
        binding.joinButton.setText("Joining...");

        eventService.joinWaitingList(eventId, userId, sessionLocation)
                .addOnSuccessListener(result -> {
                    if (!isAdded() || binding == null) return;
                    
                    String status = result.get("status");
                    if ("success".equals(status)) {
                        // Success - reload user status to update UI
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Successfully joined waitlist!", Toast.LENGTH_SHORT).show();
                        }
                        isUserJoined = true;
                        waitingListEntryId = result.get("entryId");
                        decisionId = result.get("decisionId");
                        
                        // Reload user status to update UI (this will also refresh total entrants count)
                        loadUserStatus();
                    } else if ("already_exists".equals(status)) {
                        // User already in waiting list
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "You are already on the waitlist", Toast.LENGTH_SHORT).show();
                        }
                        // Reload status in case UI is out of sync
                        loadUserStatus();
                    } else if ("limit_reached".equals(status)) {
                        // Waiting list limit reached
                        Toast.makeText(requireContext(), "Waiting list is full", Toast.LENGTH_SHORT).show();
                        binding.joinButton.setEnabled(true);
                        binding.joinButton.setText("Join Waitlist");
                        loadUserStatus();
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                        }
                        if (binding != null && isAdded()) {
                            binding.joinButton.setEnabled(true);
                            binding.joinButton.setText("Join Waitlist");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error joining waitlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    binding.joinButton.setEnabled(true);
                    binding.joinButton.setText("Join Waitlist");
                });
    }

    private void handleAbandonWaitlist() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (waitingListEntryId == null) {
            Toast.makeText(requireContext(), "Unable to find waitlist entry", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Disable button during operation
        binding.abandonButton.setEnabled(false);
        binding.abandonButton.setText("Leaving...");

        eventService.leaveWaitingList(eventId, userId, waitingListEntryId, decisionId)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || binding == null) return;
                    
                    // Success - update UI to show not joined
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Left waitlist successfully", Toast.LENGTH_SHORT).show();
                    }
                    isUserJoined = false;
                    userStatus = null;
                    userDecision = null;
                    waitingListEntryId = null;
                    decisionId = null;
                    
                    // Immediately hide status chip (status is now "pending" = no chip)
                    showUserStatus();
                    
                    // Update UI to show join button (this will also refresh total entrants count)
                    setupUserView();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error leaving waitlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    binding.abandonButton.setEnabled(true);
                    binding.abandonButton.setText("Abandon Waitlist");
                });
    }

    private void showAcceptInvitationDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_accept_invitation, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        com.google.android.material.textfield.TextInputEditText nameInput = dialogView.findViewById(R.id.etEntrantName);
        CheckBox termsCheckbox = dialogView.findViewById(R.id.checkboxTermsConditions);
        com.google.android.material.button.MaterialButton submitButton = dialogView.findViewById(R.id.btnAcceptAndSubmit);

        submitButton.setOnClickListener(v -> {
            String entrantName = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            boolean termsAccepted = termsCheckbox.isChecked();

            // Validate form
            if (entrantName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter the entrant name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!termsAccepted) {
                Toast.makeText(requireContext(), "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
                return;
            }

            // Close dialog and proceed with acceptance
            dialog.dismiss();
            handleAcceptWin();
        });

        dialog.show();
    }

    private void handleAcceptWin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || decisionId == null) {
            Toast.makeText(requireContext(), "Unable to accept", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        binding.acceptButton.setEnabled(false);
        binding.declineButton.setEnabled(false);
        binding.acceptButton.setText("Accepting...");

        eventService.acceptWin(eventId, userId, decisionId)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Successfully accepted!", Toast.LENGTH_SHORT).show();
                    }
                    // Reload user status to update UI
                    loadUserStatus();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error accepting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    binding.acceptButton.setEnabled(true);
                    binding.declineButton.setEnabled(true);
                    binding.acceptButton.setText("Accept");
                });
    }

    private void handleDeclineWin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || decisionId == null) {
            Toast.makeText(requireContext(), "Unable to decline", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        binding.acceptButton.setEnabled(false);
        binding.declineButton.setEnabled(false);
        binding.declineButton.setText("Declining...");

        eventService.declineWin(eventId, userId, decisionId)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Successfully declined", Toast.LENGTH_SHORT).show();
                    }
                    // Reload user status to update UI
                    loadUserStatus();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error declining: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    binding.acceptButton.setEnabled(true);
                    binding.declineButton.setEnabled(true);
                    binding.declineButton.setText("Decline");
                });
    }

    private void handleDrawLottery() {
        android.util.Log.d("EventDetailFragment", "handleDrawLottery called for eventId: " + eventId);
        if (eventId == null) {
            Toast.makeText(requireContext(), "Event ID is required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.drawLotteryButton.setEnabled(false);
        binding.drawLotteryButton.setText("Drawing...");

        eventService.drawLottery(eventId)
                .addOnSuccessListener(result -> {
                    if (!isAdded() || binding == null) return;
                    
                    String status = (String) result.get("status");
                    if ("success".equals(status)) {
                        Integer winnersCount = (Integer) result.get("winnersCount");
                        Integer losersCount = (Integer) result.get("losersCount");
                        String message = "Lottery drawn! " + winnersCount + " winners, " + losersCount + " losers.";
                        if (getContext() != null) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        }
                        // Reload event to refresh UI (organizer view)
                        loadEvent();
                    } else {
                        String errorMsg = (String) result.get("message");
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                        binding.drawLotteryButton.setEnabled(true);
                        binding.drawLotteryButton.setText("Draw Lottery!");
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error drawing lottery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    binding.drawLotteryButton.setEnabled(true);
                    binding.drawLotteryButton.setText("Draw Lottery!");
                });
    }

    private void showSendNotificationDialog() {
        if (eventId == null || currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogSendNotificationBinding dialogBinding = DialogSendNotificationBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.buttonReset.setOnClickListener(v -> {
            dialogBinding.checkboxInvited.setChecked(false);
            dialogBinding.checkboxCancelled.setChecked(false);
            dialogBinding.checkboxEnrolled.setChecked(false);
            dialogBinding.checkboxLost.setChecked(false);
        });

        dialogBinding.buttonOk.setOnClickListener(v -> {
            List<String> selectedStatuses = new ArrayList<>();
            boolean sendToWaitingList = false;
            
            if (dialogBinding.checkboxInvited.isChecked()) {
                selectedStatuses.add("INVITED");
            }
            if (dialogBinding.checkboxEnrolled.isChecked()) {
                // "Enrolled" means waiting list entrants, not ACCEPTED status
                sendToWaitingList = true;
            }
            if (dialogBinding.checkboxLost.isChecked()) {
                selectedStatuses.add("LOST");
            }
            if (dialogBinding.checkboxCancelled.isChecked()) {
                selectedStatuses.add("CANCELLED");
            }

            if (selectedStatuses.isEmpty() && !sendToWaitingList) {
                Toast.makeText(requireContext(), "Please select at least one group", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            sendNotificationsToSelectedGroups(selectedStatuses, sendToWaitingList);
        });

        dialog.show();
    }

    private void sendNotificationsToSelectedGroups(List<String> statuses, boolean sendToWaitingList) {
        if (eventId == null || currentEvent == null) {
            return;
        }

        String eventName = currentEvent.getEventName();
        
        // Send notifications with status-specific messages
        List<com.google.android.gms.tasks.Task<Map<String, Object>>> statusTasks = new ArrayList<>();
        
        // Handle waiting list entrants (Enrolled)
        if (sendToWaitingList) {
            String title = "Event update";
            String message = "You have an update regarding " + eventName + ".";
            com.google.android.gms.tasks.Task<Map<String, Object>> waitingListTask = eventService.sendNotificationsToWaitingListEntrants(eventId, title, message);
            statusTasks.add(waitingListTask);
        }
        
        // Handle status-based notifications
        for (String status : statuses) {
            String title;
            String message;
            
            // Set appropriate title and message based on status
            if ("INVITED".equals(status)) {
                title = "You won the lottery!";
                message = "Congratulations! You have been selected for " + eventName + ". Please accept or decline your invitation.";
            } else if ("LOST".equals(status)) {
                title = "Lottery results";
                message = "Unfortunately, you were not selected for " + eventName + ". You may still have a chance if someone declines.";
            } else if ("ACCEPTED".equals(status)) {
                title = "Registration confirmed";
                message = "Your registration for " + eventName + " has been confirmed. We look forward to seeing you!";
            } else if ("CANCELLED".equals(status)) {
                title = "Event update";
                message = "Your participation in " + eventName + " has been cancelled.";
            } else {
                // Default for any other status
                title = "Update for " + eventName;
                message = "You have an update regarding " + eventName + ".";
            }
            
            com.google.android.gms.tasks.Task<Map<String, Object>> statusTask = eventService.sendNotificationsToEntrantsByStatus(eventId, status, title, message);
            statusTasks.add(statusTask);
        }
        
        // Wait for all notifications to be sent
        com.google.android.gms.tasks.Tasks.whenAll(statusTasks)
                .addOnSuccessListener(allTasks -> {
                    if (!isAdded() || getContext() == null) return;
                    
                    int totalSent = 0;
                    for (com.google.android.gms.tasks.Task<Map<String, Object>> task : statusTasks) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Map<String, Object> result = task.getResult();
                            Integer count = (Integer) result.get("count");
                            if (count != null) {
                                totalSent += count;
                            }
                        }
                    }
                    
                    if (totalSent > 0) {
                        Toast.makeText(getContext(), "Sent " + totalSent + " notifications", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "No notifications sent", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(getContext(), "Failed to send notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Updates the lottery status display with colored text.
     * Shows "Lottery Status : Open" (green) or "Lottery Status : Closed" (red).
     */
    private void updateLotteryStatusDisplay() {
        if (currentEvent == null || binding == null || !isAdded()) {
            return;
        }

        String eventStatus = currentEvent.getStatus();
        boolean isOpen = "OPEN".equals(eventStatus);
        
        String statusText = isOpen ? "Open" : "Closed";
        int statusColor = isOpen 
            ? getResources().getColor(com.example.arcane.R.color.lottery_status_open, null)
            : getResources().getColor(com.example.arcane.R.color.lottery_status_closed, null);

        // Create the full text: "Lottery Status : Open" or "Lottery Status : Closed"
        String fullText = "Lottery Status : " + statusText;
        SpannableString spannableString = new SpannableString(fullText);
        
        // Find the start position of the status word (after "Lottery Status : ")
        int statusStart = "Lottery Status : ".length();
        int statusEnd = fullText.length();
        
        // Apply color to the status word only
        spannableString.setSpan(
            new ForegroundColorSpan(statusColor),
            statusStart,
            statusEnd,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        binding.lotteryStatusText.setText(spannableString);
    }

    private void handleDeleteEvent() {
        // Security check: Only admin can delete events
        if (!isAdmin) {
            Toast.makeText(requireContext(), "Only administrators can delete events", Toast.LENGTH_SHORT).show();
            return;
        }

        if (eventId == null || currentEvent == null) {
            Toast.makeText(requireContext(), "Event not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete \"" + currentEvent.getEventName() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Disable button during deletion
                    binding.joinButton.setEnabled(false);
                    binding.joinButton.setText("Deleting...");

                    eventRepository.deleteEvent(eventId)
                            .addOnSuccessListener(aVoid -> {
                                if (!isAdded() || binding == null) return;
                                
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Event deleted successfully", Toast.LENGTH_SHORT).show();
                                }
                                // Navigate back after deletion
                                navigateBack();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded() || binding == null) return;
                                
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Error deleting event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                                binding.joinButton.setEnabled(true);
                                binding.joinButton.setText("Delete Event");
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateBack() {
        if (!isAdded() || getView() == null) return;
        try {
            NavController navController = Navigation.findNavController(getView());
            navController.navigateUp();
        } catch (Exception e) {
            // Fragment may be detached, ignore
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        organizerViewSetup = false; // Reset flag when view is destroyed
        binding = null;
    }
}

