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
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private List<UserProfile> allUsers = new ArrayList<>(); // Store all users for filtering
    private String selectedFilter = "ALL"; // Current filter: ALL, USER, ORGANIZER

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

        setupFilters();
        loadAllUsers();
    }

    /**
     * Sets up the filter chips for role filtering.
     */
    private void setupFilters() {
        com.google.android.material.chip.ChipGroup chipGroup = binding.filterContainer;
        
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // If nothing is selected, select "All" by default
                binding.filterAll.setChecked(true);
                return;
            }
            
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.filter_all) {
                selectedFilter = "ALL";
            } else if (checkedId == R.id.filter_user) {
                selectedFilter = "USER";
            } else if (checkedId == R.id.filter_organizer) {
                selectedFilter = "ORGANIZER";
            }
            
            applyFilter();
        });
    }

    /**
     * Applies the current filter to the users list.
     */
    private void applyFilter() {
        if (adapter == null) return;

        List<UserProfile> filtered = new ArrayList<>();
        for (UserProfile user : allUsers) {
            String role = user.getRole();
            if (role == null) role = "";

            boolean shouldInclude = false;
            if ("ALL".equals(selectedFilter)) {
                shouldInclude = true;
            } else if ("USER".equals(selectedFilter)) {
                // Include users with role "USER", "ENTRANT", or null/empty
                String roleUpper = role.toUpperCase();
                shouldInclude = role.isEmpty() || 
                               "USER".equals(roleUpper) || 
                               "ENTRANT".equals(roleUpper);
            } else if ("ORGANIZER".equals(selectedFilter)) {
                // Include users with role "ORGANIZER" or "ORGANISER"
                String roleUpper = role.toUpperCase();
                shouldInclude = "ORGANIZER".equals(roleUpper) || 
                               "ORGANISER".equals(roleUpper);
            }

            if (shouldInclude) {
                filtered.add(user);
            }
        }

        adapter.setItems(filtered);
    }

    /**
     * Loads all users from the repository, excluding the currently logged in user.
     */
    private void loadAllUsers() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : null;
        
        userRepository.getAllUsers()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded() || binding == null || adapter == null) return;
                    
                    List<UserProfile> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Skip the current logged in user
                        String docId = doc.getId();
                        if (currentUserId != null && currentUserId.equals(docId)) {
                            continue;
                        }
                        
                        // Try UserProfile first
                        UserProfile user = doc.toObject(UserProfile.class);
                        if (user != null && user.getUserId() == null) {
                            user.setUserId(docId);
                        }
                        
                        // If UserProfile doesn't work, try Users model
                        if (user == null || user.getName() == null) {
                            Users usersModel = doc.toObject(Users.class);
                            if (usersModel != null) {
                                // Convert Users to UserProfile
                                user = new UserProfile();
                                user.setUserId(usersModel.getId() != null ? usersModel.getId() : docId);
                                user.setName(usersModel.getName());
                                user.setEmail(usersModel.getEmail());
                                user.setRole(usersModel.getRole());
                            }
                        }
                        
                        if (user != null) {
                            items.add(user);
                        }
                    }
                    // Store all users and apply current filter
                    allUsers = items;
                    applyFilter();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

