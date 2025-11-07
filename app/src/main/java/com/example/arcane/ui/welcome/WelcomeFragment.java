package com.example.arcane.ui.welcome;

/**
 * This file defines the WelcomeFragment class, which serves as the entry point of the application.
 * Displays welcome screen with options to create an account or login. If a user is already
 * signed in, automatically navigates to the home screen.
 *
 * Design Pattern: MVVM (Model-View-ViewModel) - Fragment acts as View
 * - Uses Navigation Component for routing
 * - Uses ViewBinding for type-safe view access
 * - Checks Firebase authentication state on view creation
 *
 * Outstanding Issues:
 * - None identified at this time
 */
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentWelcomePageBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Fragment that displays the welcome screen with login and create account options.
 * Automatically navigates to home if user is already signed in.
 *
 * @version 1.0
 */
public class WelcomeFragment extends Fragment {

    private FragmentWelcomePageBinding binding;

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
        binding = FragmentWelcomePageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored.
     * Checks if user is already signed in and sets up navigation buttons.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // If already signed in, go straight to Home
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_home);
            return;
        }

        binding.btnCreateAccount.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_create_account);
        });

        binding.btnLogin.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_login);
        });
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


