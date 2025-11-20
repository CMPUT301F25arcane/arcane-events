/**
 * UserService.java
 * 
 * Purpose: Service layer for managing user operations and business logic.
 * 
 * Design Pattern: Service Layer pattern. Provides a clean interface for user-related
 * operations, abstracting Firestore implementation details from UI components.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.service;

import androidx.annotation.NonNull;
import com.example.arcane.model.Users;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Service class for managing user operations.
 *
 * <p>Handles user creation and retrieval from Firestore.</p>
 *
 * @version 1.0
 */
public class UserService {

    private final FirebaseFirestore db;

    /**
     * Constructs a new UserService instance.

     */
    public UserService() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructor for testing
     */
    public UserService(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Creates a new user in Firestore.
     *
     * @param user the user to create
     * @return a Task that completes when the user is created
     * @throws IllegalArgumentException if the user ID is null
     */
    public Task<Void> createUser(@NonNull Users user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id (auth uid) cannot be null");
        }
        return db.collection("users").document(user.getId()).set(user);
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId the user ID to retrieve
     * @return a Task that completes with the user document snapshot
     */
    public Task<DocumentSnapshot> getUserById(@NonNull String userId) {
        return db.collection("users").document(userId).get();
    }

    /**
     * Updates specific fields of a user document.
     * Uses set with merge to ensure the document exists even if it wasn't created yet.
     *
     * @param userId the user ID to update
     * @param updates a map of field names to values to update
     * @return a Task that completes when the user is updated
     */
    public Task<Void> updateUserFields(@NonNull String userId, @NonNull java.util.Map<String, Object> updates) {
        // Use set with merge to ensure document exists even if it wasn't created yet
        // This handles edge cases where the document might not exist
        return db.collection("users").document(userId).get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        // Document exists, use update
                        return db.collection("users").document(userId).update(updates);
                    } else {
                        // Document doesn't exist, create it with the updates
                        // Add the id field to ensure it's set
                        java.util.Map<String, Object> newUserData = new java.util.HashMap<>(updates);
                        newUserData.put("id", userId);
                        return db.collection("users").document(userId).set(newUserData);
                    }
                });
    }

    /**
     * Finds a user by their email address in Firestore.
     * This is used as a workaround when Firebase Auth email hasn't been updated yet.
     *
     * @param email the email address to search for
     * @return a Task that completes with a query snapshot containing matching users
     */
    public com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> getUserByEmail(@NonNull String email) {
        return db.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get();
    }
}

