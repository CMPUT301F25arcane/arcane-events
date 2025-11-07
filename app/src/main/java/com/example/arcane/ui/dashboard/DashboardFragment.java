package com.example.arcane.ui.dashboard;

/**
 * This file defines the DashboardFragment class, which displays the dashboard screen.
 * Currently displays placeholder text. Uses DashboardViewModel to manage data
 * and follows MVVM architecture pattern.
 *
 * Design Pattern: MVVM (Model-View-ViewModel) - Fragment acts as View
 * - Uses DashboardViewModel for data management
 * - Uses ViewBinding for type-safe view access
 *
 * Outstanding Issues:
 * - Currently displays placeholder text, should be enhanced with actual dashboard content
 */
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.arcane.databinding.FragmentDashboardBinding;

/**
 * Fragment that displays the dashboard screen.
 * Currently shows placeholder text from ViewModel.
 *
 * @version 1.0
 */
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

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
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textDashboard;
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
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