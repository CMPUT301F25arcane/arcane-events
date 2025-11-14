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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentEventDetailBinding;
import com.example.arcane.model.Decision;
import com.example.arcane.model.Event;
import com.example.arcane.model.WaitingListEntry;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.WaitingListRepository;
import com.example.arcane.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fragment for displaying event details with role-based views.
 *
 * <p>Shows different UI for organizers (lottery management, entrants list) and regular users
 * (join waitlist, accept/decline invitations). Handles event registration, waitlist management,
 * and lottery drawing functionality.</p>
 *
 * @version 1.0
 */
public class EventDetailFragment extends Fragment {

    private FragmentEventDetailBinding binding;
    private EventRepository eventRepository;
    private WaitingListRepository waitingListRepository;
    private DecisionRepository decisionRepository;
    private UserService userService;
    private com.example.arcane.service.EventService eventService;
    
    private String eventId;
    private Event currentEvent;
    private boolean isOrganizer = false;
    private boolean isUserJoined = false;
    private String userStatus = null; // WAITING, WON, LOST, ACCEPTED, DECLINED
    private String userDecision = null; // none, accepted, declined
    private String waitingListEntryId = null;
    private String decisionId = null;

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

        // Setup back button
        binding.backButton.setOnClickListener(v -> navigateBack());

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

    private void loadEvent() {
        eventRepository.getEventById(eventId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentEvent = documentSnapshot.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setEventId(documentSnapshot.getId());
                            populateEventDetails();
                            
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
                        } else {
                            Toast.makeText(requireContext(), "Failed to load event", Toast.LENGTH_SHORT).show();
                            navigateBack();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
                        navigateBack();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error loading event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    navigateBack();
                });
    }

    private void populateEventDetails() {
        if (currentEvent == null) return;

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

        // Location
        if (currentEvent.getLocation() != null) {
            binding.eventLocationText.setText(currentEvent.getLocation());
        }

        // Cost
        if (currentEvent.getCost() != null) {
            binding.eventPriceText.setText(String.format(Locale.getDefault(), "$%.2f CAD", currentEvent.getCost()));
        }

        // Load organizer name (for both user and organizer views)
        loadOrganizerName();

        // Image - placeholder for now
        // TODO: Load image from posterImageUrl when Glide/Picasso is integrated
    }

    private void loadUserStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Check if user is in waiting list
        waitingListRepository.checkUserInWaitingList(eventId, userId)
                .addOnSuccessListener(querySnapshot -> {
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
                        setupUserView();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error checking status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setupUserView();
                });
    }

    private void setupOrganizerView() {
        // Hide user-specific UI
        binding.statusDecisionContainer.setVisibility(View.GONE);
        binding.abandonButtonContainer.setVisibility(View.GONE);
        binding.acceptDeclineButtonsContainer.setVisibility(View.GONE);
        binding.joinButtonContainer.setVisibility(View.GONE);

        // Show organizer-specific UI
        binding.lotteryStatusText.setVisibility(View.VISIBLE);
        binding.editEventButton.setVisibility(View.VISIBLE);
        binding.sendNotificationContainer.setVisibility(View.VISIBLE);
        binding.organizerActionButtons.setVisibility(View.VISIBLE);

        // Set lottery status (simplified - can be enhanced based on event state)
        if (currentEvent != null) {
            // Check if lottery has been drawn (can enhance later based on waiting list/decisions)
            binding.lotteryStatusText.setText("Lottery Open");
        } else {
            binding.lotteryStatusText.setText("Lottery Open");
        }

        // Setup organizer buttons
        binding.qrCodeButton.setOnClickListener(v -> navigateToQrPage());

        binding.entrantsButton.setOnClickListener(v -> {
            if (eventId != null) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                Bundle args = new Bundle();
                args.putString("eventId", eventId);
                navController.navigate(R.id.navigation_entrants, args);
            }
        });

        binding.drawLotteryButton.setOnClickListener(v -> handleDrawLottery());

        binding.sendNotificationButton.setOnClickListener(v -> {
            // TODO: Send notification functionality
            Toast.makeText(requireContext(), "Send Notification - Coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.editEventButton.setOnClickListener(v -> {
            // TODO: Navigate to edit event
            Toast.makeText(requireContext(), "Edit Event - Coming soon", Toast.LENGTH_SHORT).show();
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

    private void loadOrganizerName() {
        if (currentEvent == null || currentEvent.getOrganizerId() == null) {
            return;
        }

        userService.getUserById(currentEvent.getOrganizerId())
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Try UserProfile first
                        com.example.arcane.model.UserProfile userProfile = documentSnapshot.toObject(com.example.arcane.model.UserProfile.class);
                        if (userProfile != null && userProfile.getName() != null) {
                            binding.organizerNameText.setText(userProfile.getName());
                            return;
                        }
                        
                        // Try Users model if UserProfile didn't work
                        com.example.arcane.model.Users users = documentSnapshot.toObject(com.example.arcane.model.Users.class);
                        if (users != null && users.getName() != null) {
                            binding.organizerNameText.setText(users.getName());
                            return;
                        }
                    }
                    // Fallback to "Organizer" if name not found
                    binding.organizerNameText.setText("Organizer");
                })
                .addOnFailureListener(e -> {
                    // On failure, show default text
                    binding.organizerNameText.setText("Organizer");
                });
    }

    private void setupUserView() {
        // Hide organizer-specific UI
        binding.lotteryStatusText.setVisibility(View.GONE);
        binding.editEventButton.setVisibility(View.GONE);
        binding.sendNotificationContainer.setVisibility(View.GONE);
        binding.organizerActionButtons.setVisibility(View.GONE);

        // Show user-specific UI
        binding.statusDecisionContainer.setVisibility(View.VISIBLE);

        if (isUserJoined) {
            // User has joined - show status and decision
            showUserStatus();
            setupUserActionButtons();
        } else {
            // User not joined - show Join button
            binding.abandonButtonContainer.setVisibility(View.GONE);
            binding.acceptDeclineButtonsContainer.setVisibility(View.GONE);
            binding.joinButtonContainer.setVisibility(View.VISIBLE);

            binding.joinButton.setOnClickListener(v -> handleJoinWaitlist());
        }
    }

    private void showUserStatus() {
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
            binding.acceptButton.setOnClickListener(v -> handleAcceptWin());
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

        String userId = currentUser.getUid();

        // Disable button during operation
        binding.joinButton.setEnabled(false);
        binding.joinButton.setText("Joining...");

        eventService.joinWaitingList(eventId, userId)
                .addOnSuccessListener(result -> {
                    String status = result.get("status");
                    if ("success".equals(status)) {
                        // Success - reload user status to update UI
                        Toast.makeText(requireContext(), "Successfully joined waitlist!", Toast.LENGTH_SHORT).show();
                        isUserJoined = true;
                        waitingListEntryId = result.get("entryId");
                        decisionId = result.get("decisionId");
                        
                        // Reload user status to update UI
                        loadUserStatus();
                    } else if ("already_exists".equals(status)) {
                        // User already in waiting list
                        Toast.makeText(requireContext(), "You are already on the waitlist", Toast.LENGTH_SHORT).show();
                        // Reload status in case UI is out of sync
                        loadUserStatus();
                    } else {
                        Toast.makeText(requireContext(), "Failed to join waitlist", Toast.LENGTH_SHORT).show();
                        binding.joinButton.setEnabled(true);
                        binding.joinButton.setText("Join Waitlist");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error joining waitlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    // Success - update UI to show not joined
                    Toast.makeText(requireContext(), "Left waitlist successfully", Toast.LENGTH_SHORT).show();
                    isUserJoined = false;
                    userStatus = null;
                    userDecision = null;
                    waitingListEntryId = null;
                    decisionId = null;
                    
                    // Update UI to show join button
                    setupUserView();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error leaving waitlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.abandonButton.setEnabled(true);
                    binding.abandonButton.setText("Abandon Waitlist");
                });
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
                    Toast.makeText(requireContext(), "Successfully accepted!", Toast.LENGTH_SHORT).show();
                    // Reload user status to update UI
                    loadUserStatus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error accepting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(requireContext(), "Successfully declined", Toast.LENGTH_SHORT).show();
                    // Reload user status to update UI
                    loadUserStatus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error declining: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.acceptButton.setEnabled(true);
                    binding.declineButton.setEnabled(true);
                    binding.declineButton.setText("Decline");
                });
    }

    private void handleDrawLottery() {
        if (eventId == null) {
            Toast.makeText(requireContext(), "Event ID is required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.drawLotteryButton.setEnabled(false);
        binding.drawLotteryButton.setText("Drawing...");

        eventService.drawLottery(eventId)
                .addOnSuccessListener(result -> {
                    String status = (String) result.get("status");
                    if ("success".equals(status)) {
                        Integer winnersCount = (Integer) result.get("winnersCount");
                        Integer losersCount = (Integer) result.get("losersCount");
                        String message = "Lottery drawn! " + winnersCount + " winners, " + losersCount + " losers.";
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        // Reload event to refresh UI (organizer view)
                        loadEvent();
                    } else {
                        String errorMsg = (String) result.get("message");
                        Toast.makeText(requireContext(), "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                        binding.drawLotteryButton.setEnabled(true);
                        binding.drawLotteryButton.setText("Draw Lottery!");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error drawing lottery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.drawLotteryButton.setEnabled(true);
                    binding.drawLotteryButton.setText("Draw Lottery!");
                });
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

