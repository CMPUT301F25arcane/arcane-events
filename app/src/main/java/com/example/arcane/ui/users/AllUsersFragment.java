/**
 * AllUsersFragment.java
 * 
 * Purpose: Fragment to display all users in the database (admin only).
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and RecyclerView with adapter pattern for list display.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.users;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentAllUsersBinding;
import com.example.arcane.model.UserProfile;
import com.example.arcane.model.Users;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.repository.WaitingListRepository;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to display all users in the database (admin only).
 *
 * <p>Displays a list of all registered users with their profile information.
 * Admin users can view and delete users from this screen.</p>
 *
 * @version 1.0
 */
public class AllUsersFragment extends Fragment {

    private FragmentAllUsersBinding binding;
    private UserCardAdapter adapter;
    private UserRepository userRepository;
    private WaitingListRepository waitingListRepository;
    private DecisionRepository decisionRepository;

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
        waitingListRepository = new WaitingListRepository();
        decisionRepository = new DecisionRepository();
        adapter = new UserCardAdapter();
        adapter.setOnDeleteClickListener(this::handleDeleteUser);

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
                    if (!isAdded() || binding == null || adapter == null) return;
                    
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

    /**
     * Handles user deletion with confirmation dialog.
     */
    private void handleDeleteUser(UserProfile user) {
        String userName = user.getName() != null ? user.getName() : "this user";
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + userName + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUser(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes a user from the database.
     */
    private void deleteUser(UserProfile user) {
        if (user.getUserId() == null) {
            Toast.makeText(requireContext(), "Error: User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUserId();
        
        // First, remove user from all events (waiting lists and decisions)
        cleanupUserEventRegistrations(userId)
                .continueWithTask(task -> {
                    // Then delete user from Firestore
                    return userRepository.deleteUser(userId);
                })
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || binding == null || adapter == null) return;
                    Toast.makeText(requireContext(), "User deleted successfully", Toast.LENGTH_SHORT).show();
                    loadAllUsers(); // Refresh the list
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Error deleting user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Removes the user from all events they're registered for (waiting lists and decisions).
     * This ensures deleted users don't appear as "Unknown" in event entrant lists.
     *
     * @param userId the user ID to clean up
     * @return a Task that completes when all registrations are removed
     */
    private com.google.android.gms.tasks.Task<Void> cleanupUserEventRegistrations(String userId) {
        // Get all waiting list entries for this user
        com.google.android.gms.tasks.Task<QuerySnapshot> waitingListTask = 
            waitingListRepository.getWaitingListEntriesByUser(userId);
        
        // Get all decisions for this user
        com.google.android.gms.tasks.Task<QuerySnapshot> decisionsTask = 
            decisionRepository.getDecisionsByUser(userId);

        // Wait for both queries to complete
        return com.google.android.gms.tasks.Tasks.whenAll(waitingListTask, decisionsTask)
            .continueWithTask(combinedTask -> {
                // Delete all waiting list entries
                com.google.android.gms.tasks.Task<Void> deleteWaitingListsTask = 
                    waitingListTask.getResult().isEmpty() 
                        ? com.google.android.gms.tasks.Tasks.forResult(null)
                        : deleteAllWaitingListEntries(waitingListTask.getResult());

                // Delete all decisions
                com.google.android.gms.tasks.Task<Void> deleteDecisionsTask = 
                    decisionsTask.getResult().isEmpty()
                        ? com.google.android.gms.tasks.Tasks.forResult(null)
                        : deleteAllDecisions(decisionsTask.getResult());

                // Wait for both deletion operations to complete
                return com.google.android.gms.tasks.Tasks.whenAll(deleteWaitingListsTask, deleteDecisionsTask);
            })
            .continueWith(task -> null); // Convert to Task<Void>
    }

    /**
     * Deletes all waiting list entries from the query result.
     */
    private com.google.android.gms.tasks.Task<Void> deleteAllWaitingListEntries(QuerySnapshot snapshot) {
        List<com.google.android.gms.tasks.Task<Void>> deleteTasks = new ArrayList<>();
        
        for (QueryDocumentSnapshot doc : snapshot) {
            // Extract eventId from document path: events/{eventId}/waitingList/{entryId}
            String path = doc.getReference().getPath();
            String[] pathParts = path.split("/");
            if (pathParts.length >= 2) {
                String eventId = pathParts[1];
                String entryId = doc.getId();
                deleteTasks.add(waitingListRepository.removeFromWaitingList(eventId, entryId));
            }
        }
        
        return com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
            .continueWith(task -> null);
    }

    /**
     * Deletes all decisions from the query result.
     */
    private com.google.android.gms.tasks.Task<Void> deleteAllDecisions(QuerySnapshot snapshot) {
        List<com.google.android.gms.tasks.Task<Void>> deleteTasks = new ArrayList<>();
        
        for (QueryDocumentSnapshot doc : snapshot) {
            // Extract eventId from document path: events/{eventId}/decisions/{decisionId}
            String path = doc.getReference().getPath();
            String[] pathParts = path.split("/");
            if (pathParts.length >= 2) {
                String eventId = pathParts[1];
                String decisionId = doc.getId();
                deleteTasks.add(decisionRepository.deleteDecision(eventId, decisionId));
            }
        }
        
        return com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
            .continueWith(task -> null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

