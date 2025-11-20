package com.example.arcane.ui.users;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentAllUsersBinding;
import com.example.arcane.model.UserProfile;
import com.example.arcane.model.Users;
import com.example.arcane.repository.UserRepository;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to display all users in the database (admin only).
 */
public class AllUsersFragment extends Fragment {

    private FragmentAllUsersBinding binding;
    private UserCardAdapter adapter;
    private UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAllUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userRepository = new UserRepository();
        adapter = new UserCardAdapter();

        binding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.usersRecyclerView.setAdapter(adapter);

        loadAllUsers();
    }

    /**
     * Loads all users from the repository.
     */
    private void loadAllUsers() {
        userRepository.getAllUsers()
                .addOnSuccessListener(querySnapshot -> {
                    List<UserProfile> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Try UserProfile first
                        UserProfile user = doc.toObject(UserProfile.class);
                        if (user != null && user.getUserId() == null) {
                            user.setUserId(doc.getId());
                        }
                        
                        // If UserProfile doesn't work, try Users model
                        if (user == null || user.getName() == null) {
                            Users usersModel = doc.toObject(Users.class);
                            if (usersModel != null) {
                                // Convert Users to UserProfile
                                user = new UserProfile();
                                user.setUserId(usersModel.getId() != null ? usersModel.getId() : doc.getId());
                                user.setName(usersModel.getName());
                                user.setEmail(usersModel.getEmail());
                                user.setRole(usersModel.getRole());
                            }
                        }
                        
                        if (user != null) {
                            items.add(user);
                        }
                    }
                    adapter.setItems(items);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

