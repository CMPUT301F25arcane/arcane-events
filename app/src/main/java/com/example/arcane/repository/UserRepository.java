/**
 * UserRepository.java
 * 
 * Purpose: Data access layer for user profile operations in Firestore.
 * 
 * Design Pattern: Repository pattern. Encapsulates Firestore queries and operations
 * for user profiles, providing a clean abstraction for the service layer.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.repository;

import com.example.arcane.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for managing user profiles in Firestore.
 *
 * <p>Handles CRUD operations for user profiles stored in the "users" collection.</p>
 *
 * @version 1.0
 */
public class UserRepository {
    private static final String COLLECTION_NAME = "users";
    private final FirebaseFirestore db;

    /**
     * Default constructor for production use. Delegates to the injectable
     * constructor to simplify unit testing.
     */
    public UserRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructor with dependency injection for testing
     */
    public UserRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Creates a new user profile.
     *
     * @param user the user profile to create
     * @return a Task that completes when the user is created
     */
    public Task<Void> createUser(UserProfile user) {
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(user.getUserId());
        return docRef.set(user);
    }

    /**
     * Gets a user by ID.
     *
     * @param userId the user ID to retrieve
     * @return a Task that completes with the user document snapshot
     */
    public Task<DocumentSnapshot> getUserById(String userId) {
        return db.collection(COLLECTION_NAME).document(userId).get();
    }

    /**
     * Updates a user profile.
     *
     * @param user the user profile to update
     * @return a Task that completes when the user is updated
     */
    public Task<Void> updateUser(UserProfile user) {
        return db.collection(COLLECTION_NAME).document(user.getUserId()).set(user);
    }

    /**
     * Deletes a user profile.
     *
     * @param userId the user ID to delete
     * @return a Task that completes when the user is deleted
     */
    public Task<Void> deleteUser(String userId) {
        return db.collection(COLLECTION_NAME).document(userId).delete();
    }

    /**
     * Gets a user by device ID (for authentication).
     *
     * @param deviceId the device ID to search for
     * @return a Task that completes with the query snapshot of matching users
     */
    public Task<QuerySnapshot> getUserByDeviceId(String deviceId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("deviceId", deviceId)
                .get();
    }
}

