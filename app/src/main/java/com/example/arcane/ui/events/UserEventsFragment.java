package com.example.arcane.ui.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.arcane.databinding.FragmentEventsBinding;
import com.example.arcane.model.Event;
import com.example.arcane.model.UserProfile;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class UserEventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventCardAdapter adapter;
    private UserRepository userRepository;
    private EventRepository eventRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = new UserRepository();
        eventRepository = new EventRepository();
        adapter = new EventCardAdapter(event -> {
            // Click handling will be added later (show details / abandon / decision)
        });

        binding.eventsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.eventsRecyclerView.setAdapter(adapter);

        // Users cannot create; hide FAB
        binding.fabAddEvent.setVisibility(View.GONE);

        // Show nav button to Global Events
        binding.navButtonsContainer.setVisibility(View.VISIBLE);
        binding.primaryNavButton.setText("Go to Global Events");
        binding.primaryNavButton.setOnClickListener(v -> {
            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), com.example.arcane.R.id.nav_host_fragment_activity_main);
            navController.navigate(com.example.arcane.R.id.navigation_global_events);
        });

        loadUserEvents();
    }

    private void loadUserEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String userId = currentUser.getUid();
        userRepository.getUserById(userId)
                .addOnSuccessListener(snapshot -> {
                    UserProfile profile = snapshot.toObject(UserProfile.class);
                    if (profile == null || profile.getRegisteredEventIds() == null || profile.getRegisteredEventIds().isEmpty()) {
                        adapter.setItems(new ArrayList<>());
                        return;
                    }

                    List<String> eventIds = profile.getRegisteredEventIds();
                    List<Event> items = new ArrayList<>();

                    final int[] remaining = {eventIds.size()};
                    for (String eventId : eventIds) {
                        eventRepository.getEventById(eventId)
                                .addOnSuccessListener(doc -> {
                                    Event event = doc.toObject(Event.class);
                                    if (event != null) {
                                        event.setEventId(doc.getId());
                                        items.add(event);
                                    }
                                })
                                .addOnCompleteListener(task -> {
                                    remaining[0] -= 1;
                                    if (remaining[0] == 0) {
                                        adapter.setItems(items);
                                    }
                                });
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


