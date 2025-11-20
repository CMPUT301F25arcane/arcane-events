/**
 * CreateAccountFragment.java
 * 
 * Purpose: Handles user registration with role selection (USER or ORGANISER).
 * 
 * Design Pattern: Follows MVVM architecture pattern with Service Layer for business logic.
 * Uses ViewBinding for type-safe view access and Navigation Component for navigation.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentCreateAccountBinding;
import com.example.arcane.model.Users;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.FirebaseNetworkException;
import com.example.arcane.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;

/**
 * Create account screen fragment.
 *
 * <p>Handles user registration with role selection (USER or ORGANISER).</p>
 *
 * @version 1.0
 */
public class CreateAccountFragment extends Fragment {

    private FragmentCreateAccountBinding binding;
    private String selectedRole = "USER"; // default
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cropActivityLauncher;
    private String selectedProfilePictureBase64;

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
        binding = FragmentCreateAccountBinding.inflate(inflater, container, false);
        
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
                        android.util.Log.e("CreateAccountFragment", "Out of memory loading cropped image", e);
                        Toast.makeText(requireContext(), "Image too large. Please try again.", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        android.util.Log.e("CreateAccountFragment", "Error loading cropped image", e);
                        Toast.makeText(requireContext(), "Error loading cropped image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        // Profile picture click to upload
        binding.profilePictureCard.setOnClickListener(v -> openImagePicker());
        
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

        // Simple role selection toggle
        binding.btnUser.setOnClickListener(v1 -> {
            selectedRole = "USER";
            binding.btnUser.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3")));
            binding.btnOrganiser.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
        });
        binding.btnOrganiser.setOnClickListener(v12 -> {
            selectedRole = "ORGANISER";
            binding.btnOrganiser.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2196F3")));
            binding.btnUser.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
        });

        binding.btnSubmit.setOnClickListener(v -> {
            String name = binding.etName.getText() != null ? binding.etName.getText().toString().trim() : "";
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
            String phone = binding.etPhone.getText() != null ? binding.etPhone.getText().toString().trim() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnSubmit.setEnabled(false);
            FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            binding.btnSubmit.setEnabled(true);
                            Exception e = task.getException();
                            String message = "Sign up failed";
                            if (e instanceof FirebaseNetworkException) {
                                message = "Network error. Check your internet connection.";
                            } else if (e instanceof FirebaseAuthException) {
                                String code = ((FirebaseAuthException) e).getErrorCode();
                                Log.e("CreateAccount", "Auth failed: code=" + code + ", msg=" + e.getMessage());
                                if ("ERROR_OPERATION_NOT_ALLOWED".equals(code) || (e.getMessage() != null && e.getMessage().contains("CONFIGURATION_NOT_FOUND"))) {
                                    message = "Email/Password sign-in is disabled for this Firebase project. Enable it in Firebase Console > Authentication.";
                                } else {
                                    message = e.getMessage();
                                }
                            } else if (e != null) {
                                message = e.getMessage();
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                            return;
                        }

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null) {
                            binding.btnSubmit.setEnabled(true);
                            Toast.makeText(requireContext(), "Sign up failed: No user", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String uid = user.getUid();
                        Users profile = new Users(uid, name, email, phone, null, Timestamp.now());
                        profile.setRole(selectedRole);
                        List<String> empty = new ArrayList<>();
                        profile.setRegisteredEventIds(empty);
                        
                        // Set profile picture if selected
                        if (selectedProfilePictureBase64 != null && !selectedProfilePictureBase64.isEmpty()) {
                            profile.setProfilePictureUrl(selectedProfilePictureBase64);
                        }

                        new UserService()
                                .createUser(profile)
                                .addOnCompleteListener(writeTask -> {
                                    binding.btnSubmit.setEnabled(true);
                                    if (writeTask.isSuccessful()) {
                                        cacheUserRole(selectedRole);
                                        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                                        navController.navigate(R.id.navigation_home);
                                    } else {
                                        String msg = writeTask.getException() != null ? writeTask.getException().getMessage() : "Failed to save profile";
                                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                                    }
                                });
                    });
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
     * Opens the image picker to select a profile picture.
     */
    private void openImagePicker() {
        imagePickerLauncher.launch("image/*");
    }

    /**
     * Handles the selected image from the gallery.
     *
     * @param imageUri the URI of the selected image
     */
    private void handleImageSelection(Uri imageUri) {
        if (imageUri == null) {
            return;
        }

        // Open circular crop activity
        Intent cropIntent = new Intent(requireContext(), com.example.arcane.ui.profile.CircularCropActivity.class);
        cropIntent.putExtra("imageUri", imageUri);
        cropActivityLauncher.launch(cropIntent);
    }

    /**
     * Handles the cropped image from the crop activity.
     *
     * @param croppedBitmap the cropped bitmap
     */
    private void handleCroppedImage(Bitmap croppedBitmap) {
        // Resize to reasonable size (512x512 for profile picture)
        Bitmap resizedBitmap = resizeBitmap(croppedBitmap, 512);
        
        // Convert to base64
        selectedProfilePictureBase64 = bitmapToBase64(resizedBitmap);
        
        // Display the image
        binding.imgProfile.setImageBitmap(resizedBitmap);
        
        Toast.makeText(requireContext(), "Profile picture selected", Toast.LENGTH_SHORT).show();
    }

    /**
     * Resizes a bitmap to a maximum dimension while maintaining aspect ratio.
     *
     * @param bitmap the original bitmap
     * @param maxDimension the maximum width or height
     * @return the resized bitmap
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxDimension) {
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    private void cacheUserRole(@Nullable String role) {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (role != null) {
            editor.putString("user_role", role);
        } else {
            editor.remove("user_role");
        }
        editor.apply();
    }
}


