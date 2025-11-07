package com.example.arcane.ui.createevent;

/**
 * This file defines the CreateEventFragment class, which allows organizers to create new events.
 * Provides a form for entering event details including name, location, dates, cost, max entrants,
 * and number of winners. Validates input and creates events in Firestore via EventService.
 *
 * Design Pattern: MVVM (Model-View-ViewModel) - Fragment acts as View
 * - Uses EventService for business logic and data persistence
 * - Uses ViewBinding for type-safe view access
 * - Uses Navigation Component for routing
 *
 * Outstanding Issues:
 * - Description field is not included in the form (set to empty string)
 * - Image upload functionality not implemented (posterImageUrl set to null)
 * - Geolocation field not fully implemented (set to null even if checkbox is checked)
 * - Date validation could be improved
 */
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentCreateEventBinding;
import com.example.arcane.model.Event;
import com.example.arcane.service.EventService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Fragment that allows organizers to create new events.
 * Provides form validation and event creation functionality.
 *
 * @version 1.0
 */
public class CreateEventFragment extends Fragment {

    private FragmentCreateEventBinding binding;
    private EventService eventService;
    private FirebaseAuth auth;
    private Calendar startDateCalendar;
    private Calendar endDateCalendar;
    private Calendar registrationDeadlineCalendar;
    private SimpleDateFormat dateTimeFormat;

    /**
     * Initializes the fragment and sets up services and date formatting.
     *
     * @param savedInstanceState If the fragment is being recreated from a previous saved state, this is the state
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventService = new EventService();
        auth = FirebaseAuth.getInstance();
        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
        registrationDeadlineCalendar = Calendar.getInstance();
        dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     * @return The root View for the fragment's layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored.
     * Sets up toolbar navigation, date pickers, and create event button.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup toolbar navigation
        binding.createEventToolbar.setNavigationOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigateUp();
        });

        // Setup date pickers
        setupDatePicker(binding.startDateInput, startDateCalendar, "Start Date & Time");
        setupDatePicker(binding.endDateInput, endDateCalendar, "End Date & Time");
        setupDatePicker(binding.registrationDeadlineInput, registrationDeadlineCalendar, "Registration Deadline");

        // Setup create event button
        binding.createEventButton.setOnClickListener(v -> createEvent());
    }

    /**
     * Sets up a date and time picker for the specified EditText field.
     * Shows a date picker first, then a time picker, and updates the EditText with formatted date/time.
     *
     * @param editText The TextInputEditText to display the selected date/time
     * @param calendar The Calendar object to store the selected date/time
     * @param title The title to display in the date/time picker dialogs
     */
    private void setupDatePicker(TextInputEditText editText, Calendar calendar, String title) {
        editText.setOnClickListener(v -> {
            // First pick date
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        // Then pick time
                        TimePickerDialog timePickerDialog = new TimePickerDialog(
                                requireContext(),
                                (timeView, hourOfDay, minute) -> {
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    calendar.set(Calendar.MINUTE, minute);
                                    editText.setText(dateTimeFormat.format(calendar.getTime()));
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                false
                        );
                        timePickerDialog.setTitle(title);
                        timePickerDialog.show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.setTitle(title);
            datePickerDialog.show();
        });
    }

    /**
     * Validates form input and creates a new event in Firestore.
     * Validates required fields, dates, and numeric inputs before creating the event.
     * Shows error messages for validation failures and success message on completion.
     */
    private void createEvent() {
        // Validate required fields
        String eventName = binding.eventNameInput.getText() != null ? 
                binding.eventNameInput.getText().toString().trim() : "";
        String locationName = binding.locationInput.getText() != null ? 
                binding.locationInput.getText().toString().trim() : "";

        if (eventName.isEmpty()) {
            binding.eventNameInputLayout.setError("Event name is required");
            return;
        }
        binding.eventNameInputLayout.setError(null);

        if (locationName.isEmpty()) {
            binding.locationInputLayout.setError("Location is required");
            return;
        }
        binding.locationInputLayout.setError(null);

        // Get current user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "You must be logged in to create an event", Toast.LENGTH_SHORT).show();
            return;
        }
        String organizerId = currentUser.getUid();

        // Parse cost
        String costStr = binding.costInput.getText() != null ? 
                binding.costInput.getText().toString().trim() : "";
        Double cost = null;
        if (!costStr.isEmpty()) {
            try {
                cost = Double.parseDouble(costStr);
                if (cost < 0) {
                    binding.costInputLayout.setError("Cost cannot be negative");
                    return;
                }
            } catch (NumberFormatException e) {
                binding.costInputLayout.setError("Invalid cost");
                return;
            }
        }

        // Parse max entrants (limit entrants)
        String maxEntrantsStr = binding.limitEntrantsInput.getText() != null ? 
                binding.limitEntrantsInput.getText().toString().trim() : "";
        Integer maxEntrants = null;
        if (!maxEntrantsStr.isEmpty()) {
            try {
                maxEntrants = Integer.parseInt(maxEntrantsStr);
                if (maxEntrants <= 0) {
                    binding.limitEntrantsInputLayout.setError("Must be greater than 0");
                    return;
                }
            } catch (NumberFormatException e) {
                binding.limitEntrantsInputLayout.setError("Invalid number");
                return;
            }
        }

        // Parse number of winners
        String winnersStr = binding.winnersInput.getText() != null ? 
                binding.winnersInput.getText().toString().trim() : "";
        Integer numberOfWinners = null;
        if (!winnersStr.isEmpty()) {
            try {
                numberOfWinners = Integer.parseInt(winnersStr);
                if (numberOfWinners <= 0) {
                    // Set error on winners input layout
                    Toast.makeText(requireContext(), "Number of winners must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid number of winners", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Validate dates
        long currentTime = System.currentTimeMillis();
        if (startDateCalendar.getTimeInMillis() >= endDateCalendar.getTimeInMillis()) {
            Toast.makeText(requireContext(), "End date must be after start date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (registrationDeadlineCalendar.getTimeInMillis() < currentTime) {
            Toast.makeText(requireContext(), "Registration deadline must be in the future", Toast.LENGTH_SHORT).show();
            return;
        }
        if (registrationDeadlineCalendar.getTimeInMillis() > endDateCalendar.getTimeInMillis()) {
            Toast.makeText(requireContext(), "Registration deadline should be before event end date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create Event object
        Event event = new Event();
        event.setEventName(eventName);
        event.setDescription(""); // Description field not in form, can be added later
        event.setOrganizerId(organizerId);
        event.setLocation(locationName);
        event.setCost(cost);
        event.setMaxEntrants(maxEntrants);
        event.setNumberOfWinners(numberOfWinners);
        event.setGeolocationRequired(binding.enableGeolocationCheckbox.isChecked());
        
        // Set event date (using start date as event date)
        event.setEventDate(new Timestamp(startDateCalendar.getTime()));
        
        // Set registration dates
        // Registration start: current time
        event.setRegistrationStartDate(Timestamp.now());
        // Registration end: registration deadline
        event.setRegistrationEndDate(new Timestamp(registrationDeadlineCalendar.getTime()));
        
        // Set status to "OPEN" for new events
        event.setStatus("OPEN");
        
        // Optional fields
        event.setGeolocation(null); // Can be set later if geolocation is enabled
        event.setPosterImageUrl(null); // Can be set later when image upload is implemented

        // Save to Firebase using EventService (per docs architecture)
        binding.createEventButton.setEnabled(false);
        binding.createEventButton.setText("Creating...");

        eventService.createEvent(event)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigateUp();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.createEventButton.setEnabled(true);
                    binding.createEventButton.setText("Create Event");
                });
    }

    /**
     * Called when the view hierarchy associated with the fragment is being removed.
     * Cleans up the binding reference to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

