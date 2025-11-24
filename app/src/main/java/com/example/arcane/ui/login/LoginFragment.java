/**
 * LoginFragment.java
 * 
 * Purpose: Handles user authentication and navigation based on user role.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Service Layer for business logic.
 * Uses ViewBinding for type-safe view access and Navigation Component for navigation.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.login;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentLoginBinding;
import com.example.arcane.model.Users;
import com.example.arcane.service.UserService;
import com.example.arcane.util.LocationPermissionHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Login screen fragment.
 *
 * <p>Handles user authentication and navigation based on user role.</p>
 *
 * @version 1.0
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

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
        binding = FragmentLoginBinding.inflate(inflater, container, false);
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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // If already logged in, route by role (this will also cache the role)
            routeByRole(currentUser);
            return;
        }

        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText() != null ? binding.emailEditText.getText().toString().trim() : "";
            String password = binding.passwordEditText.getText() != null ? binding.passwordEditText.getText().toString() : "";

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.loginButton.setEnabled(false);
            FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        binding.loginButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser signedIn = FirebaseAuth.getInstance().getCurrentUser();
                            if (signedIn != null) {
                                routeByRole(signedIn);
                            }
                        } else {
                            Exception e = task.getException();
                            String message = "Login failed";
                            if (e instanceof FirebaseNetworkException) {
                                message = "Network error. Check your internet connection.";
                            } else if (e instanceof FirebaseAuthException) {
                                String code = ((FirebaseAuthException) e).getErrorCode();
                                Log.e("LoginFragment", "Auth failed: code=" + code + ", msg=" + e.getMessage());
                                if ("ERROR_OPERATION_NOT_ALLOWED".equals(code) || (e.getMessage() != null && e.getMessage().contains("CONFIGURATION_NOT_FOUND"))) {
                                    message = "Email/Password sign-in is disabled for this Firebase project. Enable it in Firebase Console > Authentication.";
                                } else {
                                    message = e.getMessage();
                                }
                            } else if (e != null) {
                                message = e.getMessage();
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        binding.createAccountButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_create_account);
        });
    }

    /**
     * Routes the user to the appropriate screen based on their role.
     *
     * @param user the authenticated Firebase user
     */
    private void routeByRole(@NonNull FirebaseUser user) {
        UserService userService = new UserService();
        userService.getUserById(user.getUid())
                .addOnSuccessListener(snapshot -> {
                    String role = null;
                    Users userModel = null;
                    if (snapshot.exists()) {
                        userModel = snapshot.toObject(Users.class);
                        if (userModel != null) role = userModel.getRole();
                    }
                    
                    // Cache role in SharedPreferences for bottom nav routing
                    cacheUserRole(role);
                    
                    // Check if we should show location permission dialog
                    // Show dialog if field doesn't exist in Firestore (user hasn't been asked yet)
                    if (userModel != null && shouldShowLocationDialog(snapshot, userModel)) {
                        showLocationPermissionDialog(user, userModel, userService);
                    } else {
                        // Navigate directly if dialog not needed
                        navigateToHome(role);
                    }
                })
                .addOnFailureListener(e -> {
                    // Default to user events on failure, cache null role
                    cacheUserRole(null);
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.navigation_home);
                });
    }

    /**
     * Checks if the location permission dialog should be shown.
     *
     * <p>The dialog should only be shown if the user hasn't been asked before.
     * We check if the locationTrackingEnabled field exists in the Firestore document.
     * If it doesn't exist, the user hasn't been asked yet.</p>
     *
     * @param snapshot the Firestore document snapshot
     * @param user the user model
     * @return true if dialog should be shown, false otherwise
     */
    private boolean shouldShowLocationDialog(@NonNull DocumentSnapshot snapshot, @NonNull Users user) {
        // Show dialog if the field doesn't exist in Firestore (user hasn't been asked yet)
        // This handles both new users and existing users who haven't seen the dialog
        return !snapshot.contains("locationTrackingEnabled");
    }

    /**
     * Shows the location permission dialog to the user.
     *
     * @param firebaseUser the authenticated Firebase user
     * @param userModel the user model from Firestore
     * @param userService the user service for updating the user
     */
    private void showLocationPermissionDialog(@NonNull FirebaseUser firebaseUser, 
                                             @NonNull Users userModel, 
                                             @NonNull UserService userService) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Enable Location Tracking?")
                .setMessage("Would you like to enable location tracking? This allows us to show organizers where event participants joined from. You can change this later in your profile settings.")
                .setPositiveButton("Allow", (dialog, which) -> {
                    // User accepted - request location permission and update preference
                    handleLocationPermissionAccept(firebaseUser, userModel, userService);
                })
                .setNegativeButton("Don't Allow", (dialog, which) -> {
                    // User declined - just update preference to false and navigate
                    handleLocationPermissionDecline(firebaseUser, userModel, userService);
                })
                .setCancelable(false) // Prevent dismissing without choosing
                .show();
    }

    /**
     * Handles when user accepts location tracking.
     *
     * @param firebaseUser the authenticated Firebase user
     * @param userModel the user model
     * @param userService the user service
     */
    private void handleLocationPermissionAccept(@NonNull FirebaseUser firebaseUser,
                                               @NonNull Users userModel,
                                               @NonNull UserService userService) {
        // Update user preference to true
        userModel.setLocationTrackingEnabled(true);
        userService.updateUser(userModel)
                .addOnSuccessListener(unused -> {
                    // Request Android location permission
                    if (LocationPermissionHelper.hasLocationPermission(requireContext())) {
                        // Already has permission, just navigate
                        navigateToHome(userModel.getRole());
                    } else {
                        // Request permission - result will be handled in onRequestPermissionsResult
                        LocationPermissionHelper.requestLocationPermission(this);
                        // Store user info temporarily to update after permission result
                        pendingLocationUpdate = new PendingLocationUpdate(firebaseUser, userModel, userService, true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginFragment", "Failed to update location tracking preference", e);
                    Toast.makeText(requireContext(), "Failed to save preference", Toast.LENGTH_SHORT).show();
                    navigateToHome(userModel.getRole());
                });
    }

    /**
     * Handles when user declines location tracking.
     *
     * @param firebaseUser the authenticated Firebase user
     * @param userModel the user model
     * @param userService the user service
     */
    private void handleLocationPermissionDecline(@NonNull FirebaseUser firebaseUser,
                                                 @NonNull Users userModel,
                                                 @NonNull UserService userService) {
        // Update user preference to false
        userModel.setLocationTrackingEnabled(false);
        userService.updateUser(userModel)
                .addOnSuccessListener(unused -> {
                    navigateToHome(userModel.getRole());
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginFragment", "Failed to update location tracking preference", e);
                    Toast.makeText(requireContext(), "Failed to save preference", Toast.LENGTH_SHORT).show();
                    navigateToHome(userModel.getRole());
                });
    }

    /**
     * Helper class to store pending location update info.
     */
    private static class PendingLocationUpdate {
        final FirebaseUser firebaseUser;
        final Users userModel;
        final UserService userService;
        final boolean accepted;

        PendingLocationUpdate(FirebaseUser firebaseUser, Users userModel, UserService userService, boolean accepted) {
            this.firebaseUser = firebaseUser;
            this.userModel = userModel;
            this.userService = userService;
            this.accepted = accepted;
        }
    }

    private PendingLocationUpdate pendingLocationUpdate;

    /**
     * Navigates to the home screen based on user role.
     *
     * @param role the user's role
     */
    private void navigateToHome(@Nullable String role) {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        if (role != null) {
            String r = role.toUpperCase();
            if ("ORGANISER".equals(r) || "ORGANIZER".equals(r)) {
                navController.navigate(R.id.navigation_home);
                return;
            }
        }
        navController.navigate(R.id.navigation_home);
    }

    /**
     * Handles the result of location permission request.
     *
     * @param requestCode the request code
     * @param permissions the permissions array
     * @param grantResults the grant results array
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (LocationPermissionHelper.isLocationPermissionRequest(requestCode)) {
            if (LocationPermissionHelper.isLocationPermissionGranted(grantResults)) {
                // Permission granted - user preference already set to true
                if (pendingLocationUpdate != null) {
                    navigateToHome(pendingLocationUpdate.userModel.getRole());
                    pendingLocationUpdate = null;
                }
            } else {
                // Permission denied - but user preference is already set to true
                // This is okay - they can still enable tracking, just won't have permission yet
                Toast.makeText(requireContext(), "Location permission denied. You can enable it later in app settings.", Toast.LENGTH_LONG).show();
                if (pendingLocationUpdate != null) {
                    navigateToHome(pendingLocationUpdate.userModel.getRole());
                    pendingLocationUpdate = null;
                }
            }
        }
    }

    private void cacheUserRole(String role) {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (role != null) {
            editor.putString("user_role", role);
        } else {
            editor.remove("user_role");
        }
        editor.apply();
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


