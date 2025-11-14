package com.example.arcane.ui.createevent;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentCreateEventBinding;
import com.example.arcane.model.Event;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.service.EventService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Fragment for editing an existing event.
 */
public class EditEventFragment extends Fragment {

    private FragmentCreateEventBinding binding;
    private EventService eventService;
    private EventRepository eventRepository;
    private String eventId;
    private Event currentEvent;

    private Calendar startDateCalendar;
    private Calendar endDateCalendar;
    private Calendar registrationDeadlineCalendar;
    private SimpleDateFormat dateTimeFormat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventService = new EventService();
        eventRepository = new EventRepository();
        startDateCalendar = Calendar.getInstance();
        endDateCalendar = Calendar.getInstance();
        registrationDeadlineCalendar = Calendar.getInstance();
        dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.createEventToolbar.setTitle("Edit Event");
        binding.createEventButton.setText("Save Changes");

        binding.createEventToolbar.setNavigationOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigateUp();
        });

        setupDatePicker(binding.startDateInput, startDateCalendar, "Start Date & Time");
        setupDatePicker(binding.endDateInput, endDateCalendar, "End Date & Time");
        setupDatePicker(binding.registrationDeadlineInput, registrationDeadlineCalendar, "Registration Deadline");

        binding.createEventButton.setOnClickListener(v -> submitEdits());

        loadEvent();
    }

    private void loadEvent() {
        if (eventId == null) {
            Toast.makeText(requireContext(), "Event ID missing", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
            return;
        }

        eventRepository.getEventById(eventId)
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        currentEvent = snapshot.toObject(Event.class);
                        if (currentEvent != null) {
                            currentEvent.setEventId(snapshot.getId());
                            populateForm();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load event", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
                });
    }

    private void populateForm() {
        binding.eventNameInput.setText(currentEvent.getEventName());
        binding.locationInput.setText(currentEvent.getLocation());
        binding.costInput.setText(currentEvent.getCost() != null ? String.valueOf(currentEvent.getCost()) : "");
        binding.limitEntrantsInput.setText(currentEvent.getMaxEntrants() != null ? String.valueOf(currentEvent.getMaxEntrants()) : "");
        binding.winnersInput.setText(currentEvent.getNumberOfWinners() != null ? String.valueOf(currentEvent.getNumberOfWinners()) : "");
        binding.enableGeolocationCheckbox.setChecked(Boolean.TRUE.equals(currentEvent.getGeolocationRequired()));

        if (currentEvent.getEventDate() != null) {
            startDateCalendar.setTime(currentEvent.getEventDate().toDate());
            binding.startDateInput.setText(dateTimeFormat.format(startDateCalendar.getTime()));
        }
        if (currentEvent.getRegistrationEndDate() != null) {
            registrationDeadlineCalendar.setTime(currentEvent.getRegistrationEndDate().toDate());
            binding.registrationDeadlineInput.setText(dateTimeFormat.format(registrationDeadlineCalendar.getTime()));
        }
        binding.endDateInput.setText(binding.startDateInput.getText());
        endDateCalendar.setTime(startDateCalendar.getTime());
    }

    private void setupDatePicker(TextInputEditText editText, Calendar calendar, String title) {
        editText.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

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

    private void submitEdits() {
        if (currentEvent == null) return;

        String eventName = binding.eventNameInput.getText() != null
                ? binding.eventNameInput.getText().toString().trim() : "";
        String locationName = binding.locationInput.getText() != null
                ? binding.locationInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(eventName)) {
            binding.eventNameInputLayout.setError("Event name is required");
            return;
        }
        binding.eventNameInputLayout.setError(null);

        if (TextUtils.isEmpty(locationName)) {
            binding.locationInputLayout.setError("Location is required");
            return;
        }
        binding.locationInputLayout.setError(null);

        Double cost = parseDouble(binding.costInput.getText());
        if (cost != null && cost < 0) {
            binding.costInputLayout.setError("Cost cannot be negative");
            return;
        }

        Integer maxEntrants = parseInteger(binding.limitEntrantsInput.getText());
        if (maxEntrants != null && maxEntrants <= 0) {
            binding.limitEntrantsInputLayout.setError("Must be greater than 0");
            return;
        }
        binding.limitEntrantsInputLayout.setError(null);

        Integer numberOfWinners = parseInteger(binding.winnersInput.getText());
        if (numberOfWinners != null && numberOfWinners <= 0) {
            Toast.makeText(requireContext(), "Number of winners must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

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

        currentEvent.setEventName(eventName);
        currentEvent.setLocation(locationName);
        currentEvent.setCost(cost);
        currentEvent.setMaxEntrants(maxEntrants);
        currentEvent.setNumberOfWinners(numberOfWinners);
        currentEvent.setGeolocationRequired(binding.enableGeolocationCheckbox.isChecked());
        currentEvent.setEventDate(new Timestamp(startDateCalendar.getTime()));
        currentEvent.setRegistrationEndDate(new Timestamp(registrationDeadlineCalendar.getTime()));

        binding.createEventButton.setEnabled(false);
        binding.createEventButton.setText("Saving...");

        eventService.updateEvent(currentEvent)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Event updated!", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
                })
                .addOnFailureListener(e -> {
                    binding.createEventButton.setEnabled(true);
                    binding.createEventButton.setText("Save Changes");
                    Toast.makeText(requireContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private Double parseDouble(@Nullable CharSequence text) {
        if (text == null) return null;
        String value = text.toString().trim();
        if (value.isEmpty()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid cost", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private Integer parseInteger(@Nullable CharSequence text) {
        if (text == null) return null;
        String value = text.toString().trim();
        if (value.isEmpty()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid number", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

