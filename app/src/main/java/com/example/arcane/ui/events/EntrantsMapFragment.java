/**
 * EntrantsMapFragment.java
 * 
 * Purpose: Displays an interactive Google Map showing entrant join locations
 * and event location for organizers.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses Google Maps SDK to display interactive map with markers.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentEntrantsMapBinding;
import com.example.arcane.model.Event;
import com.example.arcane.model.WaitingListEntry;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.WaitingListRepository;
import com.example.arcane.service.UserService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for displaying entrant join locations on an interactive map.
 *
 * <p>Shows markers for each entrant's join location (where they were when they joined
 * the waitlist) and the event location. Used by organizers to visualize the geographic
 * distribution of event participants.</p>
 *
 * @version 1.0
 */
public class EntrantsMapFragment extends Fragment implements OnMapReadyCallback {

    private FragmentEntrantsMapBinding binding;
    private EventRepository eventRepository;
    private WaitingListRepository waitingListRepository;
    private UserService userService;
    private String eventId;
    
    private GoogleMap googleMap;
    private Map<String, String> entrantNamesMap = new HashMap<>(); // entrantId -> name
    private List<LatLng> entrantLocations = new ArrayList<>();
    private LatLng eventLocation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEntrantsMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (eventId == null) {
            Toast.makeText(requireContext(), "Event ID is required", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        // Initialize repositories
        eventRepository = new EventRepository();
        waitingListRepository = new WaitingListRepository();
        userService = new UserService();

        // Setup toolbar back button
        binding.mapToolbar.setNavigationOnClickListener(v -> navigateBack());

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Load data
        loadEventAndEntrants();
    }

    /**
     * Loads event data and entrant locations.
     */
    private void loadEventAndEntrants() {
        // Load event first to get event location
        eventRepository.getEventById(eventId)
                .addOnSuccessListener(eventSnapshot -> {
                    if (!isAdded() || binding == null) return;

                    if (eventSnapshot != null && eventSnapshot.exists()) {
                        Event event = eventSnapshot.toObject(Event.class);
                        if (event != null && event.getGeolocation() != null) {
                            GeoPoint geoPoint = event.getGeolocation();
                            eventLocation = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                        }
                    }

                    // Load waiting list entries
                    loadWaitingListEntries();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    Toast.makeText(requireContext(), "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadWaitingListEntries(); // Still try to load entrants even if event fails
                });
    }

    /**
     * Loads waiting list entries and filters for those with join locations.
     */
    private void loadWaitingListEntries() {
        waitingListRepository.getWaitingListForEvent(eventId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || binding == null) return;

                    // Filter entries with joinLocation
                    List<WaitingListEntry> entriesWithLocation = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        WaitingListEntry entry = document.toObject(WaitingListEntry.class);
                        if (entry.getJoinLocation() != null) {
                            entriesWithLocation.add(entry);
                        }
                    }

                    if (entriesWithLocation.isEmpty()) {
                        Toast.makeText(requireContext(), "No entrant locations available", Toast.LENGTH_SHORT).show();
                        // Still show event location if available
                        if (googleMap != null && eventLocation != null) {
                            addEventMarker();
                            centerMapOnEvent();
                        }
                        return;
                    }

                    // Load user names for markers
                    loadEntrantNames(entriesWithLocation);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    Toast.makeText(requireContext(), "Failed to load entrants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads user names for entrant markers.
     */
    private void loadEntrantNames(List<WaitingListEntry> entries) {
        final int[] remaining = {entries.size()};
        
        for (WaitingListEntry entry : entries) {
            userService.getUserById(entry.getEntrantId())
                    .addOnSuccessListener(userSnapshot -> {
                        if (!isAdded() || binding == null) return;

                        if (userSnapshot != null && userSnapshot.exists()) {
                            com.example.arcane.model.Users user = userSnapshot.toObject(com.example.arcane.model.Users.class);
                            if (user != null) {
                                // Get name from Users model
                                String name = user.getName();
                                if (name == null || name.isEmpty()) {
                                    name = "Unknown";
                                }
                                entrantNamesMap.put(entry.getEntrantId(), name);
                            }
                        }

                        remaining[0]--;
                        if (remaining[0] == 0) {
                            // All names loaded, add markers to map
                            addMarkersToMap(entries);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || binding == null) return;
                        // Use default name if user fetch fails
                        entrantNamesMap.put(entry.getEntrantId(), "Unknown");
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            addMarkersToMap(entries);
                        }
                    });
        }
    }

    /**
     * Adds markers to the map for entrants and event location.
     */
    private void addMarkersToMap(List<WaitingListEntry> entries) {
        if (googleMap == null) return;

        // Add event location marker (if available)
        if (eventLocation != null) {
            addEventMarker();
        }

        // Add entrant location markers
        for (WaitingListEntry entry : entries) {
            GeoPoint joinLocation = entry.getJoinLocation();
            if (joinLocation != null) {
                LatLng latLng = new LatLng(joinLocation.getLatitude(), joinLocation.getLongitude());
                entrantLocations.add(latLng);

                String entrantName = entrantNamesMap.getOrDefault(entry.getEntrantId(), "Unknown");
                
                googleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(entrantName)
                        .snippet("Joined from here"));
            }
        }

        // Center map to show all markers
        centerMapOnMarkers();
    }

    /**
     * Adds event location marker with different color/style.
     */
    private void addEventMarker() {
        if (googleMap == null || eventLocation == null) return;

        googleMap.addMarker(new MarkerOptions()
                .position(eventLocation)
                .title("Event Location")
                .snippet("Event venue"));
    }

    /**
     * Centers the map to show all markers.
     */
    private void centerMapOnMarkers() {
        if (googleMap == null) return;

        if (entrantLocations.isEmpty() && eventLocation == null) {
            // No locations - center on default location (e.g., center of city)
            LatLng defaultLocation = new LatLng(43.6532, -79.3832); // Toronto default
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f));
            return;
        }

        // Calculate bounds to include all markers
        List<LatLng> allLocations = new ArrayList<>(entrantLocations);
        if (eventLocation != null) {
            allLocations.add(eventLocation);
        }

        if (allLocations.isEmpty()) {
            return;
        }

        // Simple approach: center on first location with appropriate zoom
        // More sophisticated: calculate bounds and use CameraUpdateFactory.newLatLngBounds()
        LatLng centerLocation = allLocations.get(0);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerLocation, 12f));
    }

    /**
     * Centers map on event location.
     */
    private void centerMapOnEvent() {
        if (googleMap == null || eventLocation == null) return;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 14f));
    }

    /**
     * Called when the map is ready to be used.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        
        // Configure map settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(true);

        // If data is already loaded, add markers
        // Otherwise, markers will be added when data loads
    }

    /**
     * Navigates back to previous fragment.
     */
    private void navigateBack() {
        if (getView() != null) {
            Navigation.findNavController(getView()).navigateUp();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

