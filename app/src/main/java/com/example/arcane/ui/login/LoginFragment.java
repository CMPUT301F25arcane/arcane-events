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

        // Load saved email if available
        loadSavedEmail();

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
                                // Save email if "Remember me" is checked
                                if (binding.rememberMeCheckbox.isChecked()) {
                                    saveEmail(email);
                                } else {
                                    clearSavedEmail();
                                }
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
                    
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    if (role != null) {
                        String r = role.toUpperCase();
                        if ("ORGANISER".equals(r) || "ORGANIZER".equals(r)) {
                            navController.navigate(R.id.navigation_home);
                            return;
                        }
                    }
                    navController.navigate(R.id.navigation_home);
                })
                .addOnFailureListener(e -> {
                    // Default to user events on failure, cache null role
                    cacheUserRole(null);
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.navigation_home);
                });
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
     * Saves the email address to SharedPreferences for device identification.
     *
     * @param email the email address to save
     */
    private void saveEmail(String email) {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("saved_email", email);
        editor.apply();
    }

    /**
     * Loads the saved email address from SharedPreferences and pre-fills the email field.
     */
    private void loadSavedEmail() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        String savedEmail = prefs.getString("saved_email", null);
        if (savedEmail != null && !savedEmail.isEmpty()) {
            binding.emailEditText.setText(savedEmail);
            binding.rememberMeCheckbox.setChecked(true);
        }
    }

    /**
     * Clears the saved email address from SharedPreferences.
     */
    private void clearSavedEmail() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("saved_email");
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


