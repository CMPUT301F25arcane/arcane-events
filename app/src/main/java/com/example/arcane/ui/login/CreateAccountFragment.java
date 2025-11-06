package com.example.arcane.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.util.Log;

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

public class CreateAccountFragment extends Fragment {

    private FragmentCreateAccountBinding binding;
    private String selectedRole = "USER"; // default

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

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

                        new UserService()
                                .createUser(profile)
                                .addOnCompleteListener(writeTask -> {
                                    binding.btnSubmit.setEnabled(true);
                                    if (writeTask.isSuccessful()) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


