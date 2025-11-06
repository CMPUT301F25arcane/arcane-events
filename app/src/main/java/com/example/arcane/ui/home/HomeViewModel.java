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