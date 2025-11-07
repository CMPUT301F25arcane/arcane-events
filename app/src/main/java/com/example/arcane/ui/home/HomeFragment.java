package com.example.arcane.ui.home;

/**
 * This file defines the HomeFragment class, which serves as the home screen for organizers.
 * Displays placeholder text and provides a floating action button to navigate to event creation.
 * Uses HomeViewModel to manage data and follows MVVM architecture pattern.
 *
 * Design Pattern: MVVM (Model-View-ViewModel) - Fragment acts as View
 * - Uses HomeViewModel for data management
 * - Uses ViewBinding for type-safe view access
 * - Uses Navigation Component for routing
 *
 * Outstanding Issues:
 * - Currently displays placeholder text, should be enhanced with actual home screen content
 */
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentHomeBinding;

/**
 * Fragment that displays the home screen for organizers.
 * Provides navigation to event creation via floating action button.
 *
 * @version 1.0
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     * Initializes the ViewModel and observes text data.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     * @return The root View for the fragment's layout
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored.
     * Sets up the floating action button to navigate to event creation.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup FAB to navigate to create event
        binding.fabCreateEvent.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_create_event);
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