package com.example.arcane.ui.createevent;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    private String selectedImageBase64;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private static final String TAG = "EditEventFragment";

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
        
        // Register image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleImageSelection
        );
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

        // Setup image upload
        binding.imageUploadCard.setOnClickListener(v -> openImagePicker());

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
        binding.descriptionInput.setText(currentEvent.getDescription() != null ? currentEvent.getDescription() : "");
        binding.locationInput.setText(currentEvent.getLocation());
        binding.costInput.setText(currentEvent.getCost() != null ? String.valueOf(currentEvent.getCost()) : "");
        binding.limitEntrantsInput.setText(currentEvent.getMaxEntrants() != null ? String.valueOf(currentEvent.getMaxEntrants()) : "");
        binding.winnersInput.setText(currentEvent.getNumberOfWinners() != null ? String.valueOf(currentEvent.getNumberOfWinners()) : "");
        binding.enableGeolocationCheckbox.setChecked(Boolean.TRUE.equals(currentEvent.getGeolocationRequired()));
        
        // Load existing image if available
        if (currentEvent.getPosterImageUrl() != null && !currentEvent.getPosterImageUrl().isEmpty()) {
            loadImageFromBase64(currentEvent.getPosterImageUrl());
            selectedImageBase64 = currentEvent.getPosterImageUrl();
        }

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
        String description = binding.descriptionInput.getText() != null
                ? binding.descriptionInput.getText().toString().trim() : "";
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
        currentEvent.setDescription(description);
        currentEvent.setLocation(locationName);
        currentEvent.setCost(cost);
        currentEvent.setMaxEntrants(maxEntrants);
        currentEvent.setNumberOfWinners(numberOfWinners);
        currentEvent.setGeolocationRequired(binding.enableGeolocationCheckbox.isChecked());
        currentEvent.setEventDate(new Timestamp(startDateCalendar.getTime()));
        currentEvent.setRegistrationEndDate(new Timestamp(registrationDeadlineCalendar.getTime()));
        
        // Update image if a new one was selected
        if (selectedImageBase64 != null) {
            currentEvent.setPosterImageUrl(selectedImageBase64);
        }

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

    /**
     * Opens the image picker to select a photo from the gallery.
     */
    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    /**
     * Handles the selected image from the gallery.
     *
     * @param imageUri the URI of the selected image
     */
    private void handleImageSelection(@Nullable Uri imageUri) {
        if (imageUri == null) {
            return;
        }

        try {
            // Read the image from URI
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(requireContext(), "Failed to read image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Decode the image
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                Toast.makeText(requireContext(), "Failed to decode image", Toast.LENGTH_SHORT).show();
                return;
            }

            // Resize image to reduce size (max 1024px on longest side)
            bitmap = resizeImage(bitmap, 1024);

            // Convert to base64
            selectedImageBase64 = bitmapToBase64(bitmap);

            // Display the image
            binding.uploadedImage.setImageBitmap(bitmap);
            binding.uploadedImage.setVisibility(View.VISIBLE);
            binding.uploadIcon.setVisibility(View.GONE);
            binding.uploadText.setVisibility(View.GONE);

            Toast.makeText(requireContext(), "Image selected successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error handling image selection", e);
            Toast.makeText(requireContext(), "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads and displays an image from a base64 string.
     *
     * @param base64String the base64 encoded image string
     */
    private void loadImageFromBase64(String base64String) {
        try {
            byte[] imageBytes = Base64.decode(base64String, Base64.NO_WRAP);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap != null) {
                binding.uploadedImage.setImageBitmap(bitmap);
                binding.uploadedImage.setVisibility(View.VISIBLE);
                binding.uploadIcon.setVisibility(View.GONE);
                binding.uploadText.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading image from base64", e);
        }
    }

    /**
     * Resizes a bitmap to a maximum dimension while maintaining aspect ratio.
     *
     * @param bitmap the original bitmap
     * @param maxDimension the maximum width or height
     * @return the resized bitmap
     */
    private Bitmap resizeImage(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap;
        }

        float scale = Math.min((float) maxDimension / width, (float) maxDimension / height);
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Converts a bitmap to a base64 encoded string.
     *
     * @param bitmap the bitmap to convert
     * @return the base64 encoded string
     */
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Use JPEG format with 85% quality to reduce file size
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

