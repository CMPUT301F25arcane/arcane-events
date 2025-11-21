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
     * Updates an existing user in Firestore.
     *
     * @param user the user to update
     * @return a Task that completes when the user is updated
     * @throws IllegalArgumentException if the user ID is null
     */
    public Task<Void> updateUser(@NonNull Users user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id (auth uid) cannot be null");
        }
        return db.collection("users").document(user.getId()).set(user);
    }
}

