package com.example.arcane.service;

import androidx.annotation.NonNull;
import com.example.arcane.model.Users;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserService {

    private final FirebaseFirestore db;

    /**
     * Default constructor for normal use
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

    public Task<Void> createUser(@NonNull Users user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id (auth uid) cannot be null");
        }
        return db.collection("users").document(user.getId()).set(user);
    }

    public Task<DocumentSnapshot> getUserById(@NonNull String userId) {
        return db.collection("users").document(userId).get();
    }
}

