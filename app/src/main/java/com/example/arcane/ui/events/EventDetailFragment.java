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

public class EventDetailFragment extends Fragment {

    private FragmentEventDetailBinding binding;
    private EventRepository eventRepository;
    private WaitingListRepository waitingListRepository;
    private DecisionRepository decisionRepository;
    private UserService userService;
    
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

        // Setup back button
        binding.backButton.setOnClickListener(v -> navigateBack());

        // Check user role and load event
        checkUserRoleAndLoadEvent();
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

        // Organizer name - TODO: Load organizer name from Users collection
        // For now, leave as default text

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
                                            // Decision status "PENDING" or "INVITED" means user is WAITING
                                            // Decision status "ACCEPTED" means user ACCEPTED
                                            // Decision status "DECLINED" means user DECLINED
                                            // Note: WON/LOST status will come from waitingList entry status (to be added in Step 10)
                                            if ("PENDING".equals(decisionStatus) || "INVITED".equals(decisionStatus)) {
                                                userStatus = "WAITING";
                                                userDecision = "none";
                                            } else if ("ACCEPTED".equals(decisionStatus)) {
                                                userStatus = "ACCEPTED";
                                                userDecision = "accepted";
                                            } else if ("DECLINED".equals(decisionStatus) || "CANCELLED".equals(decisionStatus)) {
                                                userStatus = "DECLINED";
                                                userDecision = "declined";
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
                        // Not joined
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

        // Set lottery status (simplified - can be enhanced)
        binding.lotteryStatusText.setText("Lottery Open");

        // Setup organizer buttons
        binding.entrantsButton.setOnClickListener(v -> {
            // TODO: Navigate to entrants fragment (Step 9)
            Toast.makeText(requireContext(), "Show Entrants - Coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.drawLotteryButton.setOnClickListener(v -> {
            // TODO: Implement draw lottery (Step 10)
            Toast.makeText(requireContext(), "Draw Lottery - Coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.sendNotificationButton.setOnClickListener(v -> {
            // TODO: Send notification functionality
            Toast.makeText(requireContext(), "Send Notification - Coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.editEventButton.setOnClickListener(v -> {
            // TODO: Navigate to edit event
            Toast.makeText(requireContext(), "Edit Event - Coming soon", Toast.LENGTH_SHORT).show();
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

            binding.joinButton.setOnClickListener(v -> {
                // TODO: Implement join waitlist (Step 8)
                Toast.makeText(requireContext(), "Join Waitlist - Coming soon", Toast.LENGTH_SHORT).show();
            });
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

        if ("WAITING".equals(userStatus)) {
            // Show Abandon button
            binding.abandonButtonContainer.setVisibility(View.VISIBLE);
            binding.abandonButton.setOnClickListener(v -> {
                // TODO: Implement abandon waitlist (Step 7)
                Toast.makeText(requireContext(), "Abandon Waitlist - Coming soon", Toast.LENGTH_SHORT).show();
            });
        } else if ("WON".equals(userStatus) && ("none".equals(userDecision) || userDecision == null)) {
            // Show Accept/Decline buttons
            binding.acceptDeclineButtonsContainer.setVisibility(View.VISIBLE);
            binding.acceptButton.setOnClickListener(v -> {
                // TODO: Implement accept (Step 6)
                Toast.makeText(requireContext(), "Accept - Coming soon", Toast.LENGTH_SHORT).show();
            });
            binding.declineButton.setOnClickListener(v -> {
                // TODO: Implement decline (Step 6)
                Toast.makeText(requireContext(), "Decline - Coming soon", Toast.LENGTH_SHORT).show();
            });
        }
        // For LOST, ACCEPTED, DECLINED, ABANDONED - no buttons shown
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

