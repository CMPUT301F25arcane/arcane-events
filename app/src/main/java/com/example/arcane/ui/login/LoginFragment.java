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
            
            // First, try normal Firebase Auth login
            FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Normal login succeeded
                            binding.loginButton.setEnabled(true);
                            FirebaseUser signedIn = FirebaseAuth.getInstance().getCurrentUser();
                            if (signedIn != null) {
                                // Check if profile exists, if not, try to find by email
                                verifyAndRouteUser(signedIn, email);
                            }
                        } else {
                            // Login failed - check if email was recently changed in Firestore
                            final Exception e = task.getException();
                            final String errorCode;
                            final String errorMessage;
                            if (e instanceof FirebaseAuthException) {
                                errorCode = ((FirebaseAuthException) e).getErrorCode();
                                errorMessage = e.getMessage();
                            } else {
                                errorCode = null;
                                errorMessage = e != null ? e.getMessage() : null;
                            }
                            
                            android.util.Log.d("LoginFragment", "Login failed. Error code: " + errorCode + ", Message: " + errorMessage);
                            
                            // Check Firestore for user with this email (might be recently changed)
                            // This handles: ERROR_USER_NOT_FOUND, ERROR_INVALID_EMAIL, ERROR_WRONG_PASSWORD, ERROR_INVALID_CREDENTIALS
                            com.example.arcane.service.UserService userService = new com.example.arcane.service.UserService();
                            userService.getUserByEmail(email)
                                    .addOnSuccessListener(querySnapshot -> {
                                        binding.loginButton.setEnabled(true);
                                        if (!querySnapshot.isEmpty()) {
                                            // Found user in Firestore with this email
                                            com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                            com.example.arcane.model.Users user = doc.toObject(com.example.arcane.model.Users.class);
                                            
                                            if (user != null && user.getId() != null) {
                                                android.util.Log.d("LoginFragment", "Found user in Firestore with email: " + email + ", UID: " + user.getId());
                                                
                                                // User exists in Firestore with this email, but login failed
                                                // This means Firebase Auth email wasn't updated, or password is wrong
                                                if ("ERROR_WRONG_PASSWORD".equals(errorCode) || 
                                                    "ERROR_INVALID_CREDENTIALS".equals(errorCode) ||
                                                    (errorMessage != null && errorMessage.toLowerCase().contains("password"))) {
                                                    // Password might be wrong, or Firebase Auth still has old email
                                                    Toast.makeText(requireContext(), 
                                                        "Login failed. Your email was recently changed in the database, but Firebase Auth may not have updated yet. Please try logging in with your previous email address, or wait a moment and try again.", 
                                                        Toast.LENGTH_LONG).show();
                                                } else {
                                                    // User not found in Firebase Auth - email change issue
                                                    Toast.makeText(requireContext(), 
                                                        "Email was recently changed. Please use your previous email address to log in, or wait for email verification to complete.", 
                                                        Toast.LENGTH_LONG).show();
                                                }
                                            } else {
                                                showLoginError(e);
                                            }
                                        } else {
                                            // Not found in Firestore - show normal error
                                            android.util.Log.d("LoginFragment", "User not found in Firestore with email: " + email);
                                            showLoginError(e);
                                        }
                                    })
                                    .addOnFailureListener(firestoreError -> {
                                        android.util.Log.e("LoginFragment", "Error checking Firestore for email", firestoreError);
                                        binding.loginButton.setEnabled(true);
                                        showLoginError(e);
                                    });
                        }
                    });
        });

        binding.createAccountButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_create_account);
        });
    }

    /**
     * Verifies the user profile exists and routes by role.
     * If profile not found by UID, tries to find by email (in case email was recently changed).
     *
     * @param user the authenticated Firebase user
     * @param loginEmail the email used to log in
     */
    private void verifyAndRouteUser(@NonNull FirebaseUser user, String loginEmail) {
        com.example.arcane.service.UserService userService = new com.example.arcane.service.UserService();
        
        // First, try to get profile by UID (normal case)
        userService.getUserById(user.getUid())
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Profile found by UID - normal case
                        com.example.arcane.model.Users u = snapshot.toObject(com.example.arcane.model.Users.class);
                        String role = (u != null) ? u.getRole() : null;
                        cacheUserRole(role);
                        routeToHome();
                    } else {
                        // Profile not found by UID - might be email change issue
                        // Try to find by email
                        android.util.Log.d("LoginFragment", "Profile not found by UID: " + user.getUid() + ", trying email: " + loginEmail);
                        userService.getUserByEmail(loginEmail)
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        // Found user in Firestore with this email
                                        com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                        com.example.arcane.model.Users firestoreUser = doc.toObject(com.example.arcane.model.Users.class);
                                        
                                        if (firestoreUser != null) {
                                            String firestoreUid = firestoreUser.getId();
                                            String authUid = user.getUid();
                                            
                                            android.util.Log.d("LoginFragment", "Found user in Firestore. Firestore UID: " + firestoreUid + ", Auth UID: " + authUid);
                                            
                                            if (firestoreUid != null && firestoreUid.equals(authUid)) {
                                                // UIDs match - profile should have been found, but wasn't
                                                // This might be a timing issue, proceed anyway
                                                android.util.Log.d("LoginFragment", "UIDs match, proceeding with login");
                                                String role = firestoreUser.getRole();
                                                cacheUserRole(role);
                                                routeToHome();
                                            } else {
                                                // UIDs don't match - this means the email update in Firebase Auth failed
                                                // but Firestore was updated. The user needs to use their old email.
                                                android.util.Log.w("LoginFragment", "UID mismatch! Firestore UID: " + firestoreUid + ", Auth UID: " + authUid);
                                                
                                                // Get the old email from Firestore user data
                                                String oldEmailInFirestore = firestoreUser.getEmail();
                                                
                                                // Sign out the new account
                                                FirebaseAuth.getInstance().signOut();
                                                
                                                // Show clear message
                                                String message = "Your email was recently changed, but the change wasn't completed in Firebase Auth. ";
                                                if (oldEmailInFirestore != null && !oldEmailInFirestore.equals(loginEmail)) {
                                                    message += "Please log in with your previous email: " + oldEmailInFirestore;
                                                } else {
                                                    message += "Please log in with your previous email address.";
                                                }
                                                
                                                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                                                binding.loginButton.setEnabled(true);
                                            }
                                        } else {
                                            // User object is null
                                            cacheUserRole(null);
                                            routeToHome();
                                        }
                                    } else {
                                        // Not found in Firestore either - new account or data issue
                                        android.util.Log.w("LoginFragment", "User not found in Firestore by email either");
                                        Toast.makeText(requireContext(), 
                                            "Profile not found. If you recently changed your email, please log in with your previous email address.", 
                                            Toast.LENGTH_LONG).show();
                                        cacheUserRole(null);
                                        routeToHome();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("LoginFragment", "Error searching Firestore by email", e);
                                    cacheUserRole(null);
                                    routeToHome();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LoginFragment", "Error getting user by UID", e);
                    cacheUserRole(null);
                    routeToHome();
                });
    }

    /**
     * Routes the user to the appropriate screen based on their role.
     *
     * @param user the authenticated Firebase user
     */
    private void routeByRole(@NonNull FirebaseUser user) {
        com.example.arcane.service.UserService userService = new com.example.arcane.service.UserService();
        userService.getUserById(user.getUid())
                .addOnSuccessListener(snapshot -> {
                    String role = null;
                    if (snapshot.exists()) {
                        com.example.arcane.model.Users u = snapshot.toObject(com.example.arcane.model.Users.class);
                        if (u != null) role = u.getRole();
                    }
                    
                    // Cache role in SharedPreferences for bottom nav routing
                    cacheUserRole(role);
                    
                    routeToHome();
                })
                .addOnFailureListener(e -> {
                    // Default to user events on failure, cache null role
                    cacheUserRole(null);
                    routeToHome();
                });
    }

    /**
     * Navigates to the home screen.
     */
    private void routeToHome() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_home);
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
     * Shows login error message to user.
     */
    private void showLoginError(Exception e) {
        String message = "Login failed";
        if (e instanceof FirebaseNetworkException) {
            message = "Network error. Check your internet connection.";
        } else if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            Log.e("LoginFragment", "Auth failed: code=" + code + ", msg=" + e.getMessage());
            if ("ERROR_OPERATION_NOT_ALLOWED".equals(code) || (e.getMessage() != null && e.getMessage().contains("CONFIGURATION_NOT_FOUND"))) {
                message = "Email/Password sign-in is disabled for this Firebase project. Enable it in Firebase Console > Authentication.";
            } else if ("ERROR_WRONG_PASSWORD".equals(code)) {
                message = "Incorrect password.";
            } else if ("ERROR_USER_NOT_FOUND".equals(code)) {
                message = "No account found with this email address.";
            } else {
                message = e.getMessage();
            }
        } else if (e != null) {
            message = e.getMessage();
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
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


