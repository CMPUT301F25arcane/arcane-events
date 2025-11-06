package com.example.arcane.ui.welcome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentWelcomePageBinding;
import com.example.arcane.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeFragment extends Fragment {

    private FragmentWelcomePageBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWelcomePageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // If already signed in, route by role
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            routeByRole(currentUser);
            return;
        }

        binding.btnCreateAccount.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_create_account);
        });

        binding.btnLogin.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.navigation_login);
        });
    }

    private void routeByRole(@NonNull FirebaseUser user) {
        UserService userService = new UserService();
        userService.getUserById(user.getUid())
                .addOnSuccessListener(snapshot -> {
                    String role = null;
                    if (snapshot.exists()) {
                        com.example.arcane.model.Users u = snapshot.toObject(com.example.arcane.model.Users.class);
                        if (u != null) role = u.getRole();
                    }
                    NavController navController = NavHostFragment.findNavController(this);
                    if (role != null) {
                        String r = role.toUpperCase();
                        if ("ORGANISER".equals(r) || "ORGANIZER".equals(r)) {
                            navController.navigate(R.id.navigation_home);
                            return;
                        }
                    }
                    // Default to user events for USER role or if role is null
                    navController.navigate(R.id.navigation_user_events);
                })
                .addOnFailureListener(e -> {
                    // Default to user events on failure
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.navigate(R.id.navigation_user_events);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


