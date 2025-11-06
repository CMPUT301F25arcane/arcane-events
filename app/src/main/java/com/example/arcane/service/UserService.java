package com.example.arcane.service;

import androidx.annotation.NonNull;

import com.example.arcane.model.Users;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserService {

    private final FirebaseFirestore db;

    public UserService() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<Void> createUser(@NonNull Users user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id (auth uid) cannot be null");
        }
        return db.collection("users").document(user.getId()).set(user);
    }
}

