package com.example.arcane.ui.notifications;

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
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentEditProfileBinding;
import com.example.arcane.model.Users;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Fragment for editing user profile information.
 */
public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private UserService userService;
    private UserRepository userRepository;
    private Users currentUser;
    private String selectedImageBase64;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private static final String TAG = "EditProfileFragment";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userService = new UserService();
        userRepository = new UserRepository();
        
        // Register image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleImageSelection
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.editProfileToolbar.setNavigationOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigateUp();
        });

        // Setup image upload
        binding.profilePictureUploadCard.setOnClickListener(v -> openImagePicker());

        binding.saveChangesButton.setOnClickListener(v -> submitEdits());

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
            return;
        }

        String userId = firebaseUser.getUid();
        userService.getUserById(userId)
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || binding == null) return;
                    
                    if (snapshot != null && snapshot.exists()) {
                        currentUser = snapshot.toObject(Users.class);
                        if (currentUser != null) {
                            currentUser.setId(snapshot.getId());
                            
                            // Manually extract pronouns and phone from snapshot to ensure they're loaded correctly
                            // This handles cases where toObject() might not deserialize these fields properly
                            if (snapshot.contains("pronouns")) {
                                Object pronounsValue = snapshot.get("pronouns");
                                if (pronounsValue != null) {
                                    currentUser.setPronouns(pronounsValue.toString());
                                    Log.d(TAG, "  Manually set pronouns from snapshot: " + pronounsValue);
                                }
                            } else {
                                Log.d(TAG, "  Pronouns field does not exist in Firestore document");
                            }
                            
                            if (snapshot.contains("phone")) {
                                Object phoneValue = snapshot.get("phone");
                                if (phoneValue != null) {
                                    currentUser.setPhone(phoneValue.toString());
                                    Log.d(TAG, "  Manually set phone from snapshot: " + phoneValue);
                                }
                            } else {
                                Log.d(TAG, "  Phone field does not exist in Firestore document");
                            }
                            
                            // Debug: Log the data from Firestore
                            Log.d(TAG, "Loaded user data from Firestore:");
                            Log.d(TAG, "  Name: " + currentUser.getName());
                            Log.d(TAG, "  Email: " + currentUser.getEmail());
                            Log.d(TAG, "  Pronouns: " + currentUser.getPronouns());
                            Log.d(TAG, "  Phone: " + currentUser.getPhone());
                            Log.d(TAG, "  Has profile picture: " + (currentUser.getProfilePictureUrl() != null && !currentUser.getProfilePictureUrl().isEmpty()));
                            
                            populateForm();
                        }
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                        }
                        if (getActivity() != null) {
                            Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                    if (getActivity() != null) {
                        Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
                    }
                });
    }

    private void populateForm() {
        if (currentUser == null) {
            Log.e(TAG, "Cannot populate form: currentUser is null");
            return;
        }

        // Populate name
        String name = currentUser.getName();
        binding.nameInput.setText(name != null ? name : "");
        Log.d(TAG, "Populated name: " + (name != null ? name : "null"));

        // Populate email
        String email = currentUser.getEmail();
        binding.emailInput.setText(email != null ? email : "");
        Log.d(TAG, "Populated email: " + (email != null ? email : "null"));

        // Populate pronouns - handle null and empty strings the same way as other fields
        String pronouns = currentUser.getPronouns();
        binding.pronounsInput.setText(pronouns != null ? pronouns : "");
        Log.d(TAG, "Populated pronouns: " + (pronouns != null ? pronouns : "null"));

        // Populate phone
        String phone = currentUser.getPhone();
        binding.phoneInput.setText(phone != null ? phone : "");
        Log.d(TAG, "Populated phone: " + (phone != null ? phone : "null"));
        
        // Load existing profile picture if available
        String profilePictureUrl = currentUser.getProfilePictureUrl();
        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
            loadImageFromBase64(profilePictureUrl);
            selectedImageBase64 = profilePictureUrl;
            Log.d(TAG, "Loaded existing profile picture");
        } else {
            // Reset image state if no picture exists
            binding.uploadedProfilePicture.setVisibility(View.GONE);
            binding.uploadIcon.setVisibility(View.VISIBLE);
            binding.uploadText.setVisibility(View.VISIBLE);
            selectedImageBase64 = null;
            Log.d(TAG, "No profile picture found");
        }
    }

    private void submitEdits() {
        if (currentUser == null) return;

        String name = binding.nameInput.getText() != null
                ? binding.nameInput.getText().toString().trim() : "";
        String email = binding.emailInput.getText() != null
                ? binding.emailInput.getText().toString().trim() : "";
        String pronouns = binding.pronounsInput.getText() != null
                ? binding.pronounsInput.getText().toString().trim() : "";
        String phone = binding.phoneInput.getText() != null
                ? binding.phoneInput.getText().toString().trim() : "";

        // Validate required fields
        if (TextUtils.isEmpty(name)) {
            binding.nameInputLayout.setError("Name is required");
            return;
        }
        binding.nameInputLayout.setError(null);

        if (TextUtils.isEmpty(email)) {
            binding.emailInputLayout.setError("Email is required");
            return;
        }
        binding.emailInputLayout.setError(null);

        // Basic email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.setError("Invalid email address");
            return;
        }
        binding.emailInputLayout.setError(null);

        // Update user object
        currentUser.setName(name);
        currentUser.setEmail(email);
        // Save pronouns - keep empty string if provided, set to null only if truly empty
        currentUser.setPronouns(pronouns.isEmpty() ? null : pronouns.trim());
        // Save phone - trim and set to null only if truly empty
        currentUser.setPhone(phone.isEmpty() ? null : phone.trim());
        
        // Update profile picture if a new one was selected
        if (selectedImageBase64 != null) {
            currentUser.setProfilePictureUrl(selectedImageBase64);
        }

        // Debug: Log what we're about to save
        Log.d(TAG, "Saving user data:");
        Log.d(TAG, "  Name: " + currentUser.getName());
        Log.d(TAG, "  Email: " + currentUser.getEmail());
        Log.d(TAG, "  Pronouns: " + currentUser.getPronouns());
        Log.d(TAG, "  Phone: " + currentUser.getPhone());

        binding.saveChangesButton.setEnabled(false);
        binding.saveChangesButton.setText("Saving...");

        userService.updateUser(currentUser)
                .addOnSuccessListener(unused -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                    }
                    if (getActivity() != null) {
                        Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    binding.saveChangesButton.setEnabled(true);
                    binding.saveChangesButton.setText("Save Changes");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
            binding.uploadedProfilePicture.setImageBitmap(bitmap);
            binding.uploadedProfilePicture.setVisibility(View.VISIBLE);
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
                binding.uploadedProfilePicture.setImageBitmap(bitmap);
                binding.uploadedProfilePicture.setVisibility(View.VISIBLE);
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

