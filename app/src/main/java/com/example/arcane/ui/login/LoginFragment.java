package com.example.arcane.ui.login;

/**
 * This file defines the LoginFragment class, which handles user authentication
 * via email and password. It validates user input, authenticates with Firebase Auth,
 * and routes users to the appropriate screen based on their role (organizer or regular user).
 * If a user is already logged in, it automatically routes them without showing the login form.
 * Caches user role in SharedPreferences for use by other components.
 *
 * Design Pattern: MVVM (Model-View-ViewModel) - Fragment acts as View
 * - Uses FirebaseAuth for authentication
 * - Uses UserService to retrieve user role information
 * - Uses Navigation Component for routing
 * - Uses ViewBinding for type-safe view access
 * - Uses SharedPreferences to cache user role
 *
 * Outstanding Issues:
 * - Error handling could be improved with more specific error messages
 * - Role routing logic could be extracted to a separate utility class
 */
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
 * Fragment that handles user login via email and password authentication.
 * Routes users to appropriate screens based on their role after successful login.
 *
 * @version 1.0
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     * @return The root View for the fragment's layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored.
     * Sets up login button click listener, create account navigation, and checks if user is already logged in.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
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
     * Fetches the user's role from Firestore, caches it in SharedPreferences, and navigates accordingly.
     *
     * @param user The FirebaseUser object representing the logged-in user
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

    /**
     * Caches the user's role in SharedPreferences for use by other components.
     * Stores the role if provided, or removes it if null.
     *
     * @param role The user's role to cache (or null to remove)
     */
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
     * Called when the view hierarchy associated with the fragment is being removed.
     * Cleans up the binding reference to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


