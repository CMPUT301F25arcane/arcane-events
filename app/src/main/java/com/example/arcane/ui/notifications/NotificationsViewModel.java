package com.example.arcane.ui.notifications;

/**
 * This file defines the NotificationsViewModel class, which provides data for the
 * NotificationsFragment. Currently provides placeholder text data. This ViewModel follows
 * the MVVM architecture pattern to separate UI logic from data management.
 *
 * Design Pattern: MVVM (Model-View-ViewModel)
 * - Extends AndroidX ViewModel for lifecycle-aware data management
 * - Uses LiveData for reactive data updates
 *
 * Outstanding Issues:
 * - Currently only provides placeholder text, not actively used in NotificationsFragment
 * - Should be extended to provide actual notification data when notification functionality is implemented
 */
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel for the NotificationsFragment.
 * Provides data and business logic for the notifications/profile screen.
 *
 * @version 1.0
 */
public class NotificationsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    /**
     * Constructs a new NotificationsViewModel and initializes placeholder text.
     */
    public NotificationsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is notifications fragment");
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