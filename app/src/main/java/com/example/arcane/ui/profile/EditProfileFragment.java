package com.example.arcane.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.example.arcane.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;

/**
 * Fragment for editing user profile.
 * Similar to EditEventFragment, provides a full-screen edit form.
 */
public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private UserService userService;
    private Users currentUser;
    private String selectedProfilePictureBase64;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cropActivityLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userService = new UserService();
        
        // Register image picker launcher
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleImageSelection
        );
        
        // Register crop activity launcher
        cropActivityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (!isAdded() || getActivity() == null) {
                    return;
                }
                
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    try {
                        String base64 = result.getData().getStringExtra("croppedImageBase64");
                        if (base64 != null && !base64.isEmpty()) {
                            byte[] imageBytes = Base64.decode(base64, Base64.NO_WRAP);
                            if (imageBytes != null && imageBytes.length > 0) {
                                Bitmap croppedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                                if (croppedBitmap != null) {
                                    handleCroppedImage(croppedBitmap);
                                } else {
                                    Toast.makeText(requireContext(), "Failed to decode image", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    } catch (OutOfMemoryError e) {
                        android.util.Log.e("EditProfileFragment", "Out of memory loading cropped image", e);
                        Toast.makeText(requireContext(), "Image too large. Please try again.", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        android.util.Log.e("EditProfileFragment", "Error loading cropped image", e);
                        Toast.makeText(requireContext(), "Error loading cropped image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        userService = new UserService();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup profile picture click
        binding.profilePictureContainer.setOnClickListener(v -> openImagePicker());

        // Setup save button
        binding.saveProfileButton.setOnClickListener(v -> saveProfile());

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
            return;
        }

        userService.getUserById(firebaseUser.getUid())
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(Users.class);
                        if (currentUser != null) {
                            populateForm();
                        } else {
                            Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
                        }
                    } else {
                        Toast.makeText(requireContext(), "User profile not found", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main).navigateUp();
                });
    }

    private void populateForm() {
        if (currentUser == null || binding == null) {
            return;
        }

        // Populate text fields
        if (currentUser.getName() != null) {
            binding.editName.setText(currentUser.getName());
        }
        if (currentUser.getEmail() != null) {
            binding.editEmail.setText(currentUser.getEmail());
        }
        if (currentUser.getPhone() != null && !currentUser.getPhone().isEmpty()) {
            binding.editPhone.setText(currentUser.getPhone());
        }
        
        // Load profile picture
        if (currentUser.getProfilePictureUrl() != null && !currentUser.getProfilePictureUrl().isEmpty()) {
            loadProfilePicture(currentUser.getProfilePictureUrl());
            selectedProfilePictureBase64 = currentUser.getProfilePictureUrl();
        }
        
        // Load additional fields from Firestore (pronouns)
        // These are not in the Users model but stored in Firestore
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            userService.getUserById(firebaseUser.getUid())
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && binding != null) {
                            // Load pronouns if available
                            if (documentSnapshot.contains("pronouns")) {
                                String pronouns = documentSnapshot.getString("pronouns");
                                if (pronouns != null && !pronouns.isEmpty()) {
                                    binding.editPronouns.setText(pronouns);
                                }
                            }
                        }
                    });
        }
    }

    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    private void handleImageSelection(Uri imageUri) {
        if (imageUri == null) {
            return;
        }

        // Open circular crop activity
        Intent cropIntent = new Intent(requireContext(), CircularCropActivity.class);
        cropIntent.putExtra("imageUri", imageUri);
        cropActivityLauncher.launch(cropIntent);
    }

    private void handleCroppedImage(Bitmap croppedBitmap) {
        try {
            if (croppedBitmap == null) {
                Toast.makeText(requireContext(), "Invalid image", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Convert to base64
            selectedProfilePictureBase64 = bitmapToBase64(croppedBitmap);
            
            // Display the image
            binding.profilePicture.setImageBitmap(croppedBitmap);
        } catch (Exception e) {
            android.util.Log.e("EditProfileFragment", "Error handling cropped image", e);
            Toast.makeText(requireContext(), "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfilePicture(String base64String) {
        try {
            byte[] imageBytes = Base64.decode(base64String, Base64.NO_WRAP);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (bitmap != null) {
                binding.profilePicture.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            android.util.Log.e("EditProfileFragment", "Error loading profile picture", e);
        }
    }

    private void saveProfile() {
        if (currentUser == null) {
            return;
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = binding.editName.getText() != null ? binding.editName.getText().toString().trim() : "";
        String email = binding.editEmail.getText() != null ? binding.editEmail.getText().toString().trim() : "";
        String phone = binding.editPhone.getText() != null ? binding.editPhone.getText().toString().trim() : "";
        String pronouns = binding.editPronouns.getText() != null ? binding.editPronouns.getText().toString().trim() : "";

        // Validation
        if (TextUtils.isEmpty(name)) {
            binding.nameInputLayout.setError("Name is required");
            binding.editName.requestFocus();
            return;
        } else {
            binding.nameInputLayout.setError(null);
        }

        if (TextUtils.isEmpty(email)) {
            binding.emailInputLayout.setError("Email is required");
            binding.editEmail.requestFocus();
            return;
        } else {
            binding.emailInputLayout.setError(null);
        }

        // Build updates map
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        if (!phone.isEmpty()) {
            updates.put("phone", phone);
        }
        if (selectedProfilePictureBase64 != null && !selectedProfilePictureBase64.isEmpty()) {
            updates.put("profilePictureUrl", selectedProfilePictureBase64);
        }
        // Save additional fields that aren't in Users model
        if (!pronouns.isEmpty()) {
            updates.put("pronouns", pronouns);
        }

        // Update Firestore with all profile changes
        updateFirestoreProfile(firebaseUser.getUid(), updates);
    }

    private void updateFirestoreProfile(String userId, java.util.Map<String, Object> updates) {
        android.util.Log.d("EditProfileFragment", "Updating Firestore profile for userId: " + userId);
        android.util.Log.d("EditProfileFragment", "Updates map: " + updates.toString());
        
        userService.updateUserFields(userId, updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("EditProfileFragment", "Firestore profile updated successfully");
                    
                    Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_LONG).show();
                    // Navigate back
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigateUp();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("EditProfileFragment", "Failed to update profile in Firestore", e);
                    Toast.makeText(requireContext(), "Failed to update profile in Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
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
