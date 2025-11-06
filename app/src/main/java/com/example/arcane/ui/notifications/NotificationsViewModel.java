/**
 * NotificationsViewModel.java
 * 
 * Purpose: ViewModel for the NotificationsFragment following MVVM architecture.
 * 
 * Design Pattern: MVVM (Model-View-ViewModel) pattern. Manages UI-related data
 * and survives configuration changes.
 * 
 * Outstanding Issues: Currently only provides placeholder text; should be extended
 * with actual notification data management.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel for the NotificationsFragment.
 *
 * <p>Manages UI-related data for the notifications screen.</p>
 *
 * @version 1.0
 */
public class NotificationsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    /**
     * Constructs a new NotificationsViewModel.
     */
    public NotificationsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is notifications fragment");
    }

    /**
     * Gets the text LiveData.
     *
     * @return the text LiveData
     */
    public LiveData<String> getText() {
        return mText;
    }
}