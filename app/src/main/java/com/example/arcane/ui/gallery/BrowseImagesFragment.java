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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arcane.databinding.FragmentBrowseImagesBinding;
import com.example.arcane.model.Event;
import com.example.arcane.model.UserProfile;
import com.example.arcane.model.Users;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private boolean isAdmin = false;

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

        // Check admin status and setup delete listener
        checkAdminStatus();

        // Load all events with poster images
        loadAllEventImages();
    }

    /**
     * Loads all events and filters for those with poster images.
     */
    private void loadAllEventImages() {
        eventRepository.getAllEvents()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || binding == null || adapter == null) return;
                    
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
     * Checks if the current user is an admin.
     */
    private void checkAdminStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            isAdmin = false;
            return;
        }

        UserRepository userRepository = new UserRepository();
        userRepository.getUserById(currentUser.getUid())
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    
                    String role = null;
                    if (snapshot.exists()) {
                        // Try UserProfile first
                        UserProfile profile = snapshot.toObject(UserProfile.class);
                        if (profile != null && profile.getRole() != null) {
                            role = profile.getRole();
                        } else {
                            // Fallback to Users model
                            Users user = snapshot.toObject(Users.class);
                            if (user != null && user.getRole() != null) {
                                role = user.getRole();
                            }
                        }
                    }
                    
                    isAdmin = role != null && "ADMIN".equals(role.toUpperCase().trim());
                    
                    // Setup delete listener only if admin
                    if (isAdmin) {
                        adapter.setOnImageDeleteListener(this::handleDeleteRequest);
                    }
                })
                .addOnFailureListener(e -> {
                    isAdmin = false;
                });
    }

    /**
     * Handles delete request from the adapter.
     *
     * @param event the event containing the image to delete
     */
    private void handleDeleteRequest(Event event) {
        if (!isAdmin) {
            Toast.makeText(requireContext(), "Only administrators can delete images", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image? This will remove the image from the event: " + event.getEventName())
                .setPositiveButton("Delete", (dialog, which) -> deleteImage(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the image from the event.
     *
     * @param event the event to remove the image from
     */
    private void deleteImage(Event event) {
        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            Toast.makeText(requireContext(), "Error: Event ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        eventRepository.removeEventImage(event.getEventId())
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Image deleted successfully", Toast.LENGTH_SHORT).show();
                    // Remove from adapter
                    adapter.removeEvent(event.getEventId());
                    // Reload images to refresh the list
                    loadAllEventImages();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Error deleting image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

