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
import com.example.arcane.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.FirebaseNetworkException;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_home);
            return;
        }

        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText() != null ? binding.emailEditText.getText().toString().trim() : "";
            String password = binding.passwordEditText.getText() != null ? binding.passwordEditText.getText().toString() : "";

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.loginButton.setEnabled(false);
            FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        binding.loginButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                            navController.navigate(R.id.navigation_home);
                        } else {
                            Exception e = task.getException();
                            String message = "Login failed";
                            if (e instanceof FirebaseNetworkException) {
                                message = "Network error. Check your internet connection.";
                            } else if (e instanceof FirebaseAuthException) {
                                String code = ((FirebaseAuthException) e).getErrorCode();
                                Log.e("LoginFragment", "Auth failed: code=" + code + ", msg=" + e.getMessage());
                                if ("ERROR_OPERATION_NOT_ALLOWED".equals(code) || (e.getMessage() != null && e.getMessage().contains("CONFIGURATION_NOT_FOUND"))) {
                                    message = "Email/Password sign-in is disabled for this Firebase project. Enable it in Firebase Console > Authentication.";
                                } else {
                                    message = e.getMessage();
                                }
                            } else if (e != null) {
                                message = e.getMessage();
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        binding.createAccountButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_create_account);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}


