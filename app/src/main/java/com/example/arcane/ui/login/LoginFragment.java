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
import com.example.arcane.util.LocationService;
import com.example.arcane.util.SessionLocationManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.FirebaseNetworkException;

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
     * Also automatically captures and stores location for the session.
     *
     * @param user the authenticated Firebase user
     */
    private void routeByRole(@NonNull FirebaseUser user) {
        UserService userService = new UserService();
        userService.getUserById(user.getUid())
                .addOnSuccessListener(snapshot -> {
                    String role = null;
                    if (snapshot.exists()) {
                        Users userModel = snapshot.toObject(Users.class);
                        if (userModel != null) role = userModel.getRole();
                    }
                    
                    // Cache role in SharedPreferences for bottom nav routing
                    cacheUserRole(role);
                    
                    // Automatically capture location for session (no user dialog)
                    captureAndStoreSessionLocation(role);
                })
                .addOnFailureListener(e -> {
                    // Default to user events on failure, cache null role
                    cacheUserRole(null);
                    // Still try to capture location even if user fetch fails
                    captureAndStoreSessionLocation(null);
                });
    }

    /**
     * Automatically captures and stores the user's location for the session.
     *
     * <p>Checks if location permission is granted. If yes, captures location immediately.
     * If no, requests permission and captures after permission is granted.</p>
     *
     * @param role the user's role (for navigation after location capture)
     */
    private void captureAndStoreSessionLocation(@Nullable String role) {
        boolean hasPermission = LocationPermissionHelper.hasLocationPermission(requireContext());
        Log.d("LoginFragment", "DEBUG: Location permission status: " + (hasPermission ? "GRANTED" : "DENIED"));
        
        if (hasPermission) {
            // Permission already granted - capture location immediately
            Log.d("LoginFragment", "DEBUG: Attempting to capture location...");
            captureLocation(role);
        } else {
            // Request permission - will capture location after permission is granted
            Log.d("LoginFragment", "DEBUG: Requesting location permission...");
            LocationPermissionHelper.requestLocationPermission(this);
            // Store role temporarily to navigate after permission result
            pendingRoleForLocation = role;
        }
    }

    private String pendingRoleForLocation;

    /**
     * Captures the current location and stores it in session.
     *
     * @param role the user's role (for navigation after location capture)
     */
    private void captureLocation(@Nullable String role) {
        LocationService.getCurrentLocation(requireContext(), new LocationService.LocationCallback() {
            @Override
            public void onLocationSuccess(@NonNull com.google.firebase.firestore.GeoPoint geoPoint) {
                // Store location in session
                SessionLocationManager.saveSessionLocation(requireContext(), geoPoint);
                Log.d("LoginFragment", "DEBUG: SUCCESS: Location = " + 
                      geoPoint.getLatitude() + ", " + geoPoint.getLongitude());
                Log.d("LoginFragment", "DEBUG: Session location saved: " + 
                      geoPoint.getLatitude() + ", " + geoPoint.getLongitude());
                // Navigate to home
                navigateToHome(role);
            }

            @Override
            public void onLocationFailure(@NonNull Exception exception) {
                // Location capture failed - log but continue (location is optional)
                Log.e("LoginFragment", "DEBUG: FAILURE: " + exception.getMessage());
                Log.w("LoginFragment", "Failed to capture location: " + exception.getMessage());
                // Navigate anyway - location is optional for session
                navigateToHome(role);
            }
        });
    }

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
     * <p>If permission is granted, captures location and stores it in session.
     * If denied, continues without location (location is optional).</p>
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
                // Permission granted - capture location now
                captureLocation(pendingRoleForLocation);
            } else {
                // Permission denied - continue without location (location is optional)
                Log.d("LoginFragment", "Location permission denied - continuing without location");
                navigateToHome(pendingRoleForLocation);
            }
            // Clear pending role
            pendingRoleForLocation = null;
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


