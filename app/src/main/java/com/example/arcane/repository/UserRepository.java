package com.example.arcane.repository;

import com.example.arcane.model.UserProfile;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    private static final String COLLECTION_NAME = "users";
    private final FirebaseFirestore db;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a new user profile
     */
    public Task<Void> createUser(UserProfile user) {
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(user.getUserId());
        return docRef.set(user);
    }

    /**
     * Get user by ID
     */
    public Task<DocumentSnapshot> getUserById(String userId) {
        return db.collection(COLLECTION_NAME).document(userId).get();
    }

    /**
     * Update user profile
     */
    public Task<Void> updateUser(UserProfile user) {
        return db.collection(COLLECTION_NAME).document(user.getUserId()).set(user);
    }

    /**
     * Delete user profile
     */
    public Task<Void> deleteUser(String userId) {
        return db.collection(COLLECTION_NAME).document(userId).delete();
    }

    /**
     * Get user by device ID (for authentication)
     */
    public Task<QuerySnapshot> getUserByDeviceId(String deviceId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("deviceId", deviceId)
                .get();
    }
}

