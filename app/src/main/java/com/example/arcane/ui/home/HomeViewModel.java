package com.example.arcane.ui.home;

/**
 * This file defines the HomeViewModel class, which provides data for the HomeFragment.
 * Currently provides placeholder text data. This ViewModel follows the MVVM architecture
 * pattern to separate UI logic from data management.
 *
 * Design Pattern: MVVM (Model-View-ViewModel)
 * - Extends AndroidX ViewModel for lifecycle-aware data management
 * - Uses LiveData for reactive data updates
 *
 * Outstanding Issues:
 * - Currently only provides placeholder text, should be extended with actual home screen data
 */
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel for the HomeFragment.
 * Provides data and business logic for the home screen.
 *
 * @version 1.0
 */
public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    /**
     * Constructs a new HomeViewModel and initializes placeholder text.
     */
    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
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