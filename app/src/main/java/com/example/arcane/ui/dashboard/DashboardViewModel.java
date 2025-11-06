package com.example.arcane.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel for the DashboardFragment.
 *
 * <p>Manages UI-related data for the dashboard screen.</p>
 *
 * @version 1.0
 */
public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    /**
     * Constructs a new DashboardViewModel.
     */
    public DashboardViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is dashboard fragment");
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