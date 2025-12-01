/**
 * NotificationsFragment.java
 * 
 * Purpose: Displays user profile information and provides logout functionality.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and Service Layer for business logic.
 * 
 * Outstanding Issues:
 * - Uses FragmentProfileBinding (binding name doesn't match fragment name)
 * - Pronouns field is not in the Users model and cannot be displayed
 * 
 * @version 1.0
 */
package com.example.arcane.ui.notifications;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import androidx.appcompat.app.AlertDialog;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentProfileBinding;
import com.example.arcane.model.Notification;
import com.example.arcane.model.Users;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.repository.WaitingListRepository;
import com.example.arcane.service.NotificationService;
import com.example.arcane.service.UserService;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import android.widget.LinearLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Notifications/Profile screen fragment.
 *
 * <p>Displays user profile information and provides logout functionality.</p>
 *
 * @version 1.0
 */
public class NotificationsFragment extends Fragment {

    private FragmentProfileBinding binding;
    private UserService userService;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private WaitingListRepository waitingListRepository;
    private DecisionRepository decisionRepository;

    /**
     * Creates and returns the view hierarchy for this fragment.
     *
     * @param inflater the layout inflater
     * @param container the parent view group
     * @param savedInstanceState the saved instance state
     * @return the root view
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        userService = new UserService();
        userRepository = new UserRepository();
        notificationService = new NotificationService();
        waitingListRepository = new WaitingListRepository();
        decisionRepository = new DecisionRepository();

        binding.logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            clearCachedUserRole();
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_welcome);
        });

        binding.deleteProfileButton.setOnClickListener(v -> showDeleteConfirmDialog());

        // Edit profile button click handler
        binding.editProfileButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_edit_profile);
        });

        // Notification toggle functionality
        binding.toggleNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateNotificationPreference(!isChecked);
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Load user profile data after view is created and fragment is attached
        // This ensures the fragment is properly attached before checking for user
        loadUserProfile();
        // Notifications are now shown in the Events section, not in profile
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile data when returning from edit screen
        if (binding != null) {
            loadUserProfile();
        }
    }

    /**
     * Loads the user profile from Firestore.
     */
    private void loadUserProfile() {
        // Ensure fragment is attached before checking user or navigating
        if (!isAdded() || getActivity() == null) {
            return;
        }
        
        // Check if user is already logged in - if so, load immediately
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is already authenticated, load profile data immediately
            loadUserData(currentUser);
            return;
        }
        
        // User not found immediately - use Handler with delay for tests/edge cases
        // This is especially important for tests where sign-in happens asynchronously
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (!isAdded() || getActivity() == null) {
                return;
            }
            
            // Check if we're still on the Profile destination before navigating away
            // This prevents navigation race conditions
            try {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                androidx.navigation.NavDestination currentDest = navController.getCurrentDestination();
                if (currentDest == null || currentDest.getId() != R.id.navigation_notifications) {
                    // We're no longer on Profile, don't navigate away
                    return;
                }
            } catch (Exception e) {
                // Can't check destination, continue anyway
            }
            
            FirebaseUser retryUser = FirebaseAuth.getInstance().getCurrentUser();
            if (retryUser != null) {
                // User found on retry, load profile
                loadUserData(retryUser);
            }
            // If still no user, just show empty state (user should be signed in before reaching Profile)
        }, 500); // Reduced delay to 500ms for edge cases only
    }
    
    private void loadUserData(FirebaseUser currentUser) {
        if (!isAdded() || getActivity() == null || binding == null) {
            return;
        }

        String userId = currentUser.getUid();
        userService.getUserById(userId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Users user = documentSnapshot.toObject(Users.class);
                        if (user != null) {
                            populateProfileFields(user);
                        }
                    } else {
                        Toast.makeText(requireContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Populates the profile fields with user data.
     *
     * @param user the user data to populate
     */
    private void populateProfileFields(Users user) {
        if (user.getName() != null) {
            binding.editName.setText(user.getName());
        }
        if (user.getEmail() != null) {
            binding.editEmail.setText(user.getEmail());
        }
        if (user.getPronouns() != null && !user.getPronouns().isEmpty()) {
            binding.editPronouns.setText(user.getPronouns());
        }
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            binding.editPhone.setText(user.getPhone());
        }
        
        // Load and display profile picture if available
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            loadProfilePicture(user.getProfilePictureUrl());
        } else {
            // Show placeholder when no profile picture is set
            binding.profilePicture.setVisibility(View.GONE);
            binding.profilePicturePlaceholder.setVisibility(View.VISIBLE);
        }
        
        // Show delete button only for non-organizer users
        String role = user.getRole();
        boolean isOrganizer = role != null && ("ORGANIZER".equalsIgnoreCase(role) || "ORGANISER".equalsIgnoreCase(role));
        boolean isAdmin = role != null && "ADMIN".equalsIgnoreCase(role);
        binding.deleteProfileButton.setVisibility(isOrganizer ? View.GONE : View.VISIBLE);

        // Hide toggle buttons for ADMIN and ORGANISER
        if (isAdmin || isOrganizer) {
            binding.toggleNotificationsContainer.setVisibility(View.GONE);
            binding.toggleGeolocationContainer.setVisibility(View.GONE);
        } else {
            binding.toggleNotificationsContainer.setVisibility(View.VISIBLE);
            binding.toggleGeolocationContainer.setVisibility(View.VISIBLE);
            
            // Set notification toggle state without triggering listener
            Boolean notificationOptOut = user.getNotificationOptOut();
            boolean notificationsEnabled = notificationOptOut == null || !notificationOptOut;
            binding.toggleNotifications.setOnCheckedChangeListener(null);
            binding.toggleNotifications.setChecked(notificationsEnabled);
            binding.toggleNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateNotificationPreference(!isChecked);
            });
        }
    }

    /**
     * Loads and displays a profile picture from a base64 string.
     *
     * @param base64String the base64 encoded image string
     */
    private void loadProfilePicture(String base64String) {
        try {
            byte[] imageBytes = android.util.Base64.decode(base64String, android.util.Base64.NO_WRAP);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap != null) {
                binding.profilePicture.setImageBitmap(bitmap);
                binding.profilePicture.setVisibility(View.VISIBLE);
                binding.profilePicturePlaceholder.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            android.util.Log.e("NotificationsFragment", "Error loading profile picture", e);
            // Show placeholder if image fails to load
            binding.profilePicture.setVisibility(View.GONE);
            binding.profilePicturePlaceholder.setVisibility(View.VISIBLE);
        }
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Profile")
            .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
            .setPositiveButton("Delete", (d, w) -> deleteProfile())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        
        // First, remove user from all events (waiting lists and decisions)
        cleanupUserEventRegistrations(userId)
            .continueWithTask(task -> {
                // Then delete user from Firestore
                return userRepository.deleteUser(userId);
            })
            .continueWithTask(task -> {
                // Finally delete from Firebase Auth
                return currentUser.delete();
            })
            .addOnSuccessListener(v -> {
                clearCachedUserRole();
                Toast.makeText(requireContext(), "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                navController.navigate(R.id.navigation_welcome);
            })
            .addOnFailureListener(e -> {
                // Check if error is due to requiring recent authentication
                if (e instanceof FirebaseAuthException) {
                    FirebaseAuthException authException = (FirebaseAuthException) e;
                    String errorCode = authException.getErrorCode();
                    if ("ERROR_REQUIRES_RECENT_LOGIN".equals(errorCode) || 
                        (e.getMessage() != null && e.getMessage().contains("requires recent authentication"))) {
                        // Prompt for password to re-authenticate
                        promptForPasswordAndDelete(userId);
                        return;
                    }
                }
                Toast.makeText(requireContext(), "Failed to delete profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    /**
     * Removes the user from all events they're registered for (waiting lists and decisions).
     * This ensures deleted users don't appear as "Unknown" in event entrant lists.
     *
     * @param userId the user ID to clean up
     * @return a Task that completes when all registrations are removed
     */
    private com.google.android.gms.tasks.Task<Void> cleanupUserEventRegistrations(String userId) {
        // Get all waiting list entries for this user
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> waitingListTask = 
            waitingListRepository.getWaitingListEntriesByUser(userId);
        
        // Get all decisions for this user
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> decisionsTask = 
            decisionRepository.getDecisionsByUser(userId);

        // Wait for both queries to complete
        return com.google.android.gms.tasks.Tasks.whenAll(waitingListTask, decisionsTask)
            .continueWithTask(combinedTask -> {
                // Delete all waiting list entries
                com.google.android.gms.tasks.Task<Void> deleteWaitingListsTask = 
                    waitingListTask.getResult().isEmpty() 
                        ? com.google.android.gms.tasks.Tasks.forResult(null)
                        : deleteAllWaitingListEntries(waitingListTask.getResult());

                // Delete all decisions
                com.google.android.gms.tasks.Task<Void> deleteDecisionsTask = 
                    decisionsTask.getResult().isEmpty()
                        ? com.google.android.gms.tasks.Tasks.forResult(null)
                        : deleteAllDecisions(decisionsTask.getResult());

                // Wait for both deletion operations to complete
                return com.google.android.gms.tasks.Tasks.whenAll(deleteWaitingListsTask, deleteDecisionsTask);
            })
            .continueWith(task -> null); // Convert to Task<Void>
    }

    /**
     * Deletes all waiting list entries from the query result.
     */
    private com.google.android.gms.tasks.Task<Void> deleteAllWaitingListEntries(com.google.firebase.firestore.QuerySnapshot snapshot) {
        List<com.google.android.gms.tasks.Task<Void>> deleteTasks = new java.util.ArrayList<>();
        
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
            // Extract eventId from document path: events/{eventId}/waitingList/{entryId}
            String path = doc.getReference().getPath();
            String[] pathParts = path.split("/");
            if (pathParts.length >= 2) {
                String eventId = pathParts[1];
                String entryId = doc.getId();
                deleteTasks.add(waitingListRepository.removeFromWaitingList(eventId, entryId));
            }
        }
        
        return com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
            .continueWith(task -> null);
    }

    /**
     * Deletes all decisions from the query result.
     */
    private com.google.android.gms.tasks.Task<Void> deleteAllDecisions(com.google.firebase.firestore.QuerySnapshot snapshot) {
        List<com.google.android.gms.tasks.Task<Void>> deleteTasks = new java.util.ArrayList<>();
        
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshot) {
            // Extract eventId from document path: events/{eventId}/decisions/{decisionId}
            String path = doc.getReference().getPath();
            String[] pathParts = path.split("/");
            if (pathParts.length >= 2) {
                String eventId = pathParts[1];
                String decisionId = doc.getId();
                deleteTasks.add(decisionRepository.deleteDecision(eventId, decisionId));
            }
        }
        
        return com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
            .continueWith(task -> null);
    }

    private void promptForPasswordAndDelete(String userId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(requireContext(), "Unable to re-authenticate. Please log out and log back in.", Toast.LENGTH_LONG).show();
            return;
        }

        // Create a dialog to get password
        android.widget.EditText passwordInput = new android.widget.EditText(requireContext());
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("Enter your password");

        new AlertDialog.Builder(requireContext())
            .setTitle("Re-authentication Required")
            .setMessage("For security, please enter your password to confirm account deletion.")
            .setView(passwordInput)
            .setPositiveButton("Confirm", (dialog, which) -> {
                String password = passwordInput.getText().toString().trim();
                if (password.isEmpty()) {
                    Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show();
                    return;
                }
                reAuthenticateAndDelete(currentUser, password, userId);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void reAuthenticateAndDelete(FirebaseUser currentUser, String password, String userId) {
        String email = currentUser.getEmail();
        if (email == null) {
            Toast.makeText(requireContext(), "Unable to get email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-authenticate with email and password
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        currentUser.reauthenticate(credential)
            .addOnSuccessListener(aVoid -> {
                // Re-authentication successful, now delete
                // First, remove user from all events (waiting lists and decisions)
                cleanupUserEventRegistrations(userId)
                    .continueWithTask(task -> {
                        // Then delete user from Firestore
                        return userRepository.deleteUser(userId);
                    })
                    .continueWithTask(task -> {
                        // Finally delete from Firebase Auth
                        return currentUser.delete();
                    })
                    .addOnSuccessListener(v -> {
                        clearCachedUserRole();
                        Toast.makeText(requireContext(), "Profile deleted successfully", Toast.LENGTH_SHORT).show();
                        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                        navController.navigate(R.id.navigation_welcome);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to delete profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                String errorMessage = "Authentication failed";
                if (e instanceof FirebaseAuthException) {
                    FirebaseAuthException authException = (FirebaseAuthException) e;
                    String errorCode = authException.getErrorCode();
                    if ("ERROR_WRONG_PASSWORD".equals(errorCode)) {
                        errorMessage = "Incorrect password. Please try again.";
                    } else if ("ERROR_INVALID_EMAIL".equals(errorCode)) {
                        errorMessage = "Invalid email address";
                    } else {
                        errorMessage = e.getMessage();
                    }
                } else if (e.getMessage() != null) {
                    errorMessage = e.getMessage();
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
            });
    }

    private void updateNotificationPreference(boolean optOut) {
        if (binding == null || !isAdded() || getContext() == null) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        android.content.Context context = getContext();
        String userId = currentUser.getUid();
        userRepository.getUserById(userId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded() || binding == null || context == null) {
                        return;
                    }
                    if (documentSnapshot.exists()) {
                        Users user = documentSnapshot.toObject(Users.class);
                        if (user != null) {
                            user.setId(documentSnapshot.getId());
                            user.setNotificationOptOut(optOut);
                            userRepository.updateUser(user)
                                    .addOnFailureListener(e -> {
                                        if (isAdded() && binding != null && context != null) {
                                            Toast.makeText(context, "Failed to update notification preference", Toast.LENGTH_SHORT).show();
                                            binding.toggleNotifications.setOnCheckedChangeListener(null);
                                            binding.toggleNotifications.setChecked(!optOut);
                                            binding.toggleNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                                updateNotificationPreference(!isChecked);
                                            });
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && binding != null && context != null) {
                        Toast.makeText(context, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                        binding.toggleNotifications.setOnCheckedChangeListener(null);
                        binding.toggleNotifications.setChecked(!optOut);
                        binding.toggleNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            updateNotificationPreference(!isChecked);
                        });
                    }
                });
    }

    // Notifications are now displayed in the Events section, not in profile
    // Keeping these methods commented out in case they're needed later
    /*
    private void loadNotifications() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String userId = currentUser.getUid();
        notificationService.getUserNotifications(userId)
                .addOnSuccessListener(querySnapshot -> {
                    displayNotifications(querySnapshot);
                })
                .addOnFailureListener(e -> {
                    // Silently fail as really, notifications are optional
                });
    }

    private void displayNotifications(QuerySnapshot querySnapshot) {
        if (binding == null || !isAdded()) {
            return;
        }

        LinearLayout notificationsContainer = binding.getRoot().findViewById(R.id.notifications_container);
        if (notificationsContainer == null) {
            // Container doesn't exist yet, skip displaying notifications for now
            return;
        }

        notificationsContainer.removeAllViews();

        final LinearLayout finalContainer = notificationsContainer;

        if (querySnapshot.isEmpty()) {
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        for (QueryDocumentSnapshot doc : querySnapshot) {
            Notification notification = doc.toObject(Notification.class);
            notification.setNotificationId(doc.getId());

            View notificationView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.layout_notification_banner, finalContainer, false);

            android.widget.TextView titleView = notificationView.findViewById(R.id.notification_title);
            android.widget.TextView messageView = notificationView.findViewById(R.id.notification_message);
            android.widget.TextView timestampView = notificationView.findViewById(R.id.notification_timestamp);
            android.widget.ImageButton closeButton = notificationView.findViewById(R.id.notification_close);

            titleView.setText(notification.getTitle());
            messageView.setText(notification.getMessage());
            
            if (notification.getTimestamp() != null) {
                timestampView.setText(dateFormat.format(notification.getTimestamp().toDate()));
            }

            notificationView.setOnClickListener(v -> {
                if (!Boolean.TRUE.equals(notification.getRead())) {
                    markNotificationAsRead(notification);
                }
            });

            closeButton.setOnClickListener(v -> {
                markNotificationAsRead(notification);
                finalContainer.removeView(notificationView);
            });

            if ("INVITED".equals(notification.getType())) {
                notificationView.setBackgroundColor(getResources().getColor(R.color.status_won_bg, null));
            } else if ("LOST".equals(notification.getType())) {
                notificationView.setBackgroundColor(getResources().getColor(R.color.status_lost_bg, null));
            }

            finalContainer.addView(notificationView);
        }
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
    */

    private void clearCachedUserRole() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("user_role");
        editor.apply();
        
        // Clear session location on logout
        com.example.arcane.util.SessionLocationManager.clearSessionLocation(requireContext());
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