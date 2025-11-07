package com.example.arcane.repository;

/**
 * This file defines the UserRepository class, which provides data access methods
 * for UserProfile documents stored in Firestore. Handles CRUD operations for users including
 * creating, reading, updating, and deleting. Supports querying users by device ID for authentication.
 *
 * Design Pattern: Repository Pattern
 * - Encapsulates data access logic for UserProfile entities
 * - Provides abstraction over Firestore collection operations
 * - Handles Firestore-specific query patterns
 *
 * Outstanding Issues:
 * - None identified at this time
 */
import com.example.arcane.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class that provides data access methods for UserProfile entities.
 * Manages user profiles stored in the users collection in Firestore.
 *
 * @version 1.0
 */
public class UserRepository {
    private static final String COLLECTION_NAME = "users";
    private final FirebaseFirestore db;

    /**
     * Constructs a new UserRepository with the default Firestore instance.
     *
     * @version 1.0
     */
    public UserRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructs a new UserRepository with the specified Firestore instance.
     * Used for dependency injection in tests.
     *
     * @param db The FirebaseFirestore instance to use
     * @version 1.0
     */
    public UserRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Creates a new user profile document in Firestore.
     * Uses the user's userId as the document ID.
     *
     * @param user The UserProfile object to create
     * @return A Task that completes when the user profile is created
     */
    public Task<Void> createUser(UserProfile user) {
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(user.getUserId());
        return docRef.set(user);
    }

    /**
     * Gets a user profile document by its ID.
     *
     * @param userId The unique identifier of the user (typically Firebase Auth UID)
     * @return A Task that completes with the user profile document snapshot
     */
    public Task<DocumentSnapshot> getUserById(String userId) {
        return db.collection(COLLECTION_NAME).document(userId).get();
    }

    /**
     * Updates a user profile document with new data.
     *
     * @param user The UserProfile object containing updated data (must have a valid userId)
     * @return A Task that completes when the user profile is updated
     */
    public Task<Void> updateUser(UserProfile user) {
        return db.collection(COLLECTION_NAME).document(user.getUserId()).set(user);
    }

    /**
     * Deletes a user profile document.
     *
     * @param userId The unique identifier of the user to delete
     * @return A Task that completes when the user profile is deleted
     */
    public Task<Void> deleteUser(String userId) {
        return db.collection(COLLECTION_NAME).document(userId).delete();
    }

    /**
     * Gets user profile(s) by device ID.
     * Used for device-based authentication. May return multiple users if device ID is not unique.
     *
     * @param deviceId The device ID to search for
     * @return A Task that completes with a QuerySnapshot containing user profiles with the specified device ID
     */
    public Task<QuerySnapshot> getUserByDeviceId(String deviceId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("deviceId", deviceId)
                .get();
    }
}

