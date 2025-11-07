package com.example.arcane.ui.dashboard;

/**
 * This file defines the DashboardViewModel class, which provides data for the DashboardFragment.
 * Currently provides placeholder text data. This ViewModel follows the MVVM architecture
 * pattern to separate UI logic from data management.
 *
 * Design Pattern: MVVM (Model-View-ViewModel)
 * - Extends AndroidX ViewModel for lifecycle-aware data management
 * - Uses LiveData for reactive data updates
 *
 * Outstanding Issues:
 * - Currently only provides placeholder text, should be extended with actual dashboard data
 */
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel for the DashboardFragment.
 * Provides data and business logic for the dashboard screen.
 *
 * @version 1.0
 */
public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    /**
     * Constructs a new DashboardViewModel and initializes placeholder text.
     */
    public DashboardViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
    }

    /**
     * Returns the LiveData text observable.
     *
     * @return LiveData containing the text to display
     */
    public LiveData<String> getText() {
        return mText;
    }
}