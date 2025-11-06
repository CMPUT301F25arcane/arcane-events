package com.example.arcane.ui.events;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentEventsBinding;
import com.example.arcane.model.Event;
import com.example.arcane.repository.EventRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEventsFragment extends Fragment {

    private FragmentEventsBinding binding;
    private EventCardAdapter adapter;
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

        eventRepository = new EventRepository();
        adapter = new EventCardAdapter(event -> {
            // Click handling will be added in later step
        });

        binding.eventsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.eventsRecyclerView.setAdapter(adapter);

        // Hide nav buttons container for organizer (only users need it)
        binding.navButtonsContainer.setVisibility(View.GONE);

        // Organizer can create events - show FAB
        binding.fabAddEvent.setVisibility(View.VISIBLE);
        binding.fabAddEvent.setOnClickListener(v -> {
            androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_create_event);
        });

        loadOrganizerEvents();
    }

    private void loadOrganizerEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String organizerId = currentUser.getUid();
        eventRepository.getEventsByOrganizer(organizerId)
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId());
                        items.add(event);
                    }
                    adapter.setItems(items);
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh events list when returning to this fragment (e.g., after creating an event)
        loadOrganizerEvents();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


