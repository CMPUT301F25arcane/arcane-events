/**
 * BrowseImagesFragment.java
 * 
 * Purpose: Displays all event poster images in a gallery grid for admin users.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and RecyclerView with adapter pattern for grid display.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arcane.databinding.FragmentBrowseImagesBinding;
import com.example.arcane.model.Event;
import com.example.arcane.repository.EventRepository;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for browsing all event poster images in a gallery view.
 *
 * <p>This fragment is only accessible to admin users and displays all event
 * poster images in a grid layout. Images are loaded from the Event model's
 * posterImageUrl field.</p>
 *
 * @version 1.0
 */
public class BrowseImagesFragment extends Fragment {

    private FragmentBrowseImagesBinding binding;
    private GalleryImageAdapter adapter;
    private EventRepository eventRepository;

    /**
     * Creates and returns the view hierarchy for this fragment.
     *
     * @param inflater the layout inflater
     * @param container the parent view group
     * @param savedInstanceState the saved instance state
     * @return the root view
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBrowseImagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned.
     *
     * @param view the view returned by onCreateView
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = new EventRepository();
        adapter = new GalleryImageAdapter();

        // Setup RecyclerView with grid layout (2 columns)
        binding.imagesRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.imagesRecyclerView.setAdapter(adapter);

        // Setup toolbar back button
        binding.browseImagesToolbar.setNavigationOnClickListener(v -> 
            androidx.navigation.Navigation.findNavController(requireView()).navigateUp()
        );

        // Load all events with poster images
        loadAllEventImages();
    }

    /**
     * Loads all events and filters for those with poster images.
     */
    private void loadAllEventImages() {
        eventRepository.getAllEvents()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        // Only add events that have poster images
                        if (event.getPosterImageUrl() != null && !event.getPosterImageUrl().isEmpty()) {
                            events.add(event);
                        }
                    }
                    adapter.setItems(events);
                })
                .addOnFailureListener(e -> {
                    // Handle error - could show a toast or error message
                    android.util.Log.e("BrowseImagesFragment", "Error loading events: " + e.getMessage());
                });
    }

    /**
     * Called when the fragment becomes visible to the user.
     * Refreshes the images list.
     */
    @Override
    public void onResume() {
        super.onResume();
        // Refresh images list when returning to this fragment
        loadAllEventImages();
    }

    /**
     * Called when the view hierarchy is being removed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

