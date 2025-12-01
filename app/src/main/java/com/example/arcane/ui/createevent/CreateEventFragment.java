package com.example.arcane.ui.createevent;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentCreateEventBinding;
import com.example.arcane.model.Event;
import com.example.arcane.service.EventService;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;
import com.google.zxing.BarcodeFormat;
import com.example.arcane.util.QrCodeGenerator;
import com.google.zxing.WriterException;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CreateEventFragment.java
 * 
 * Purpose: Allows organizers to create new events with comprehensive event details.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Service Layer pattern for business logic.
 * Uses ViewBinding for type-safe view access and implements form validation.
 * 
 * Outstanding Issues:
 * - Geolocation is set to null even when geolocationRequired is true
 * - Date validation could be more robust (e.g., check if dates are in the past)
 * 
 * @version 1.0
 */
public class CreateEventFragment extends Fragment {
    private static final int QR_CODE_SIZE_PX = 512;
    private static final int QR_STYLE_VERSION = 1;
    private static final String TAG = "CreateEventFragment";

    private FragmentCreateEventBinding binding;
    private EventService eventService;
    private FirebaseAuth auth;
    private Calendar startDateCalendar;
    private Calendar endDateCalendar;
    private Calendar registrationDeadlineCalendar;
    private SimpleDateFormat dateTimeFormat;
    private String selectedImageBase64;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<android.content.Intent> placesAutocompleteLauncher;
    private GeoPoint selectedLocationGeoPoint; // Store selected location coordinates

    /**
     * Called when the fragment is created.
     *
     * @param savedInstanceState the saved instance state
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
        
        // Register image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleImageSelection
        );
        
        // Register Places Autocomplete launcher
        placesAutocompleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    android.content.Intent data = result.getData();
                    if (data != null) {
                        Place place = Autocomplete.getPlaceFromIntent(data);
                        handlePlaceSelection(place);
                    }
                } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                    Status status = Autocomplete.getStatusFromIntent(result.getData());
                    Log.e(TAG, "Places Autocomplete error: " + status.getStatusMessage());
                    Toast.makeText(requireContext(), "Error selecting location: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

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
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
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

        // Setup toolbar navigation
        binding.createEventToolbar.setNavigationOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigateUp();
        });

        // Setup date pickers
        setupDatePicker(binding.startDateInput, startDateCalendar, "Start Date & Time");
        setupDatePicker(binding.endDateInput, endDateCalendar, "End Date & Time");
        setupDatePicker(binding.registrationDeadlineInput, registrationDeadlineCalendar, "Registration Deadline");

        // Setup category dropdown
        setupCategoryDropdown();
        
        // Initialize Places SDK
        initializePlaces();
        
        // Setup location autocomplete
        setupLocationAutocomplete();

        // Setup image upload
        binding.imageUploadCard.setOnClickListener(v -> openImagePicker());

        // Setup create event button
        binding.createEventButton.setOnClickListener(v -> createEvent());
    }
    
    /**
     * Initializes Google Places SDK.
     * 
     * <p>Gets the API key from AndroidManifest.xml meta-data.
     * Note: API key must be set in AndroidManifest before this will work.</p>
     */
    private void initializePlaces() {
        if (!Places.isInitialized()) {
            try {
                android.content.pm.ApplicationInfo appInfo = requireContext().getPackageManager()
                    .getApplicationInfo(requireContext().getPackageName(), android.content.pm.PackageManager.GET_META_DATA);
                String apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY");
                if (apiKey != null && !apiKey.equals("YOUR_API_KEY")) {
                    Places.initialize(requireContext(), apiKey);
                } else {
                    Log.w(TAG, "Google Places API key not configured. Autocomplete will not work.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing Places SDK", e);
            }
        }
    }
    
    /**
     * Sets up location autocomplete functionality.
     * When user clicks on location input, opens Places Autocomplete if available.
     * If autocomplete is not available, allows manual typing.
     */
    private void setupLocationAutocomplete() {
        // Only trigger autocomplete on click if Places is initialized
        // Otherwise, allow manual typing
        binding.locationInput.setOnClickListener(v -> {
            if (Places.isInitialized()) {
                openPlacesAutocomplete();
            } else {
                // Places not initialized - allow manual typing
                // Just focus the field so user can type
                binding.locationInput.requestFocus();
            }
        });
        
        // On focus, only open autocomplete if Places is initialized and field is empty
        binding.locationInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && Places.isInitialized()) {
                String currentText = binding.locationInput.getText() != null 
                    ? binding.locationInput.getText().toString() 
                    : "";
                // Only open autocomplete if field is empty
                if (currentText.isEmpty()) {
                    openPlacesAutocomplete();
                }
            }
        });
    }
    
    /**
     * Opens Google Places Autocomplete activity.
     */
    private void openPlacesAutocomplete() {
        // Check if Places SDK is initialized
        if (!Places.isInitialized()) {
            Log.w(TAG, "Places SDK not initialized. Attempting to initialize...");
            initializePlaces();
            
            // If still not initialized, show error and allow manual typing
            if (!Places.isInitialized()) {
                Toast.makeText(requireContext(), 
                    "Places API not configured. You can type the location manually.", 
                    Toast.LENGTH_LONG).show();
                Log.e(TAG, "Cannot open autocomplete: Places SDK not initialized");
                return;
            }
        }
        
        try {
            // Specify the fields to be returned
            List<Place.Field> fields = java.util.Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            );
            
            // Build the autocomplete intent
            android.content.Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN,
                fields
            ).build(requireContext());
            
            // Launch the autocomplete activity
            placesAutocompleteLauncher.launch(intent);
        } catch (Exception e) {
            // Catch any exceptions and allow manual typing
            Log.e(TAG, "Error opening Places Autocomplete: " + e.getMessage(), e);
            Toast.makeText(requireContext(), 
                "Autocomplete unavailable. You can type the location manually.", 
                Toast.LENGTH_LONG).show();
            // Don't crash - allow user to type manually
        }
    }
    
    /**
     * Handles the selected place from Places Autocomplete.
     *
     * @param place the selected Place object
     */
    private void handlePlaceSelection(Place place) {
        // Set the address in the location input field
        String address = place.getAddress();
        if (address != null) {
            binding.locationInput.setText(address);
            binding.locationInputLayout.setError(null);
        }
        
        // Store the coordinates for later use (Commit 13 will save this to event.geolocation)
        if (place.getLatLng() != null) {
            selectedLocationGeoPoint = new GeoPoint(
                place.getLatLng().latitude,
                place.getLatLng().longitude
            );
            Log.d(TAG, "Location selected: " + address + " at " + 
                  selectedLocationGeoPoint.getLatitude() + ", " + 
                  selectedLocationGeoPoint.getLongitude());
        } else {
            selectedLocationGeoPoint = null;
        }
    }

    /**
     * Sets up the category dropdown with predefined categories.
     */
    private void setupCategoryDropdown() {
        String[] categories = {"Sports", "Entertainment", "Education", "Food & Dining", "Technology"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_dropdown_item_1line, categories);
        binding.categoryInput.setAdapter(adapter);
        
        // Show dropdown when clicked
        binding.categoryInput.setOnClickListener(v -> {
            binding.categoryInput.showDropDown();
        });
        
        // Also show dropdown when focused
        binding.categoryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.categoryInput.showDropDown();
            }
        });
    }

    /**
     * Sets up a date and time picker for the specified edit text field.
     *
     * @param editText the text input field to attach the picker to
     * @param calendar the calendar instance to store the selected date/time
     * @param title the title to display in the picker dialog
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
     * Validates form inputs and creates a new event.
     * 
     * <p>Performs validation on all required fields, parses numeric inputs,
     * validates date constraints, and creates the event via EventService.</p>
     */
    private void createEvent() {
        // Validate required fields
        String eventName = binding.eventNameInput.getText() != null ? 
                binding.eventNameInput.getText().toString().trim() : "";
        String locationName = binding.locationInput.getText() != null ? 
                binding.locationInput.getText().toString().trim() : "";
        String description = binding.descriptionInput.getText() != null ? 
                binding.descriptionInput.getText().toString().trim() : "";

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

        // Validate category
        String categoryDisplay = binding.categoryInput.getText() != null ? 
                binding.categoryInput.getText().toString().trim() : "";
        if (categoryDisplay.isEmpty()) {
            binding.categoryInputLayout.setError("Category is required");
            return;
        }
        binding.categoryInputLayout.setError(null);
        
        // Convert display category to internal format
        String category = convertCategoryToInternal(categoryDisplay);

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
        event.setDescription(description); // Description from form input
        event.setOrganizerId(organizerId);
        event.setLocation(locationName);
        event.setCategory(category);
        event.setCost(cost);
        event.setMaxEntrants(maxEntrants);
        event.setNumberOfWinners(numberOfWinners);
        event.setGeolocationRequired(binding.enableGeolocationCheckbox.isChecked());
        
        // Set event dates
        event.setEventDate(new Timestamp(startDateCalendar.getTime()));
        event.setEventEndDate(new Timestamp(endDateCalendar.getTime()));
        
        // Set registration dates
        // Registration start: current time
        event.setRegistrationStartDate(Timestamp.now());
        // Registration end: registration deadline
        event.setRegistrationEndDate(new Timestamp(registrationDeadlineCalendar.getTime()));
        
        // Set status to "OPEN" for new events
        event.setStatus("OPEN");
        
        // Set geolocation if address was selected via Places Autocomplete
        // selectedLocationGeoPoint is set in handlePlaceSelection() when organizer selects address
        event.setGeolocation(selectedLocationGeoPoint); // null if no address selected via autocomplete
        event.setPosterImageUrl(selectedImageBase64); // Set the base64 encoded image

        // Save to Firebase using EventService (per docs architecture)
        binding.createEventButton.setEnabled(false);
        binding.createEventButton.setText("Creating...");

        eventService.createEvent(event)
                .addOnSuccessListener(documentReference -> {
                    if (!isAdded() || binding == null) return;
                    generateAndPersistQrCode(documentReference.getId());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                    }
                    if (getActivity() != null) {
                        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
                        navController.navigateUp();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    binding.createEventButton.setEnabled(true);
                    binding.createEventButton.setText("Create Event");
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

    private void generateAndPersistQrCode(@Nullable String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return;
        }
        String qrData = "EVENT:" + eventId;
        String base64;
        try {
            base64 = QrCodeGenerator.generateBase64(qrData, QR_CODE_SIZE_PX);
        } catch (WriterException e) {
            Log.e(TAG, "QR generation failed", e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (base64 == null) {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("qrCodeImageBase64", base64);
        updates.put("qrStyleVersion", QR_STYLE_VERSION);

        eventService.updateEventFields(eventId, updates)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save QR code", e);
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "QR code couldn't be saved: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Converts display category name to internal category format.
     *
     * @param displayCategory the display category name
     * @return the internal category format
     */
    private String convertCategoryToInternal(String displayCategory) {
        switch (displayCategory) {
            case "Sports":
                return "SPORTS";
            case "Entertainment":
                return "ENTERTAINMENT";
            case "Education":
                return "EDUCATION";
            case "Food & Dining":
                return "FOOD_DINING";
            case "Technology":
                return "TECHNOLOGY";
            default:
                return displayCategory.toUpperCase().replace(" ", "_");
        }
    }

}

