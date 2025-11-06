/**
 * HomeViewModel.java
 * 
 * Purpose: ViewModel for the HomeFragment following MVVM architecture.
 * 
 * Design Pattern: MVVM (Model-View-ViewModel) pattern. Manages UI-related data
 * and survives configuration changes.
 * 
 * Outstanding Issues: Currently only provides placeholder text; should be extended
 * with actual home screen data management.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel for the HomeFragment.
 *
 * <p>Manages UI-related data for the home screen.</p>
 *
 * @version 1.0
 */
public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    /**
     * Constructs a new HomeViewModel.
     */
    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
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