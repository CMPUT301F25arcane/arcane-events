package com.example.arcane.service;

/**
 * This file defines the UserService class, which provides business logic for user-related
 * operations. It acts as a service layer between the UI and the UserRepository, handling
 * user creation and retrieval from Firestore. This service encapsulates user-related
 * business rules and validation.
 *
 * Design Pattern: Service Layer Pattern
 * - Provides business logic for user operations
 * - Acts as an intermediary between UI and repository layers
 * - Handles validation and error handling
 *
 * Outstanding Issues:
 * - None identified at this time
 */
import androidx.annotation.NonNull;

import com.example.arcane.model.Users;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Service class that provides business logic for user-related operations.
 * Handles user creation and retrieval from Firestore.
 *
 * @version 1.0
 */
public class UserService {

    private final FirebaseFirestore db;

    /**
     * Constructs a new UserService and initializes Firestore instance.
     *
     * @version 1.0
     */
    public UserService() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Creates a new user document in Firestore.
     * Validates that the user ID is not null before creating the document.
     *
     * @param user The Users object containing user information to create
     * @return A Task that completes when the user document is created
     * @throws IllegalArgumentException if the user ID is null
     */
    public Task<Void> createUser(@NonNull Users user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id (auth uid) cannot be null");
        }
        return db.collection("users").document(user.getId()).set(user);
    }

    /**
     * Retrieves a user document from Firestore by user ID.
     *
     * @param userId The unique identifier of the user (typically Firebase Auth UID)
     * @return A Task that completes with the user document snapshot
     */
    public Task<DocumentSnapshot> getUserById(@NonNull String userId) {
        return db.collection("users").document(userId).get();
    }
}

