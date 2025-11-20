/**
 * NotificationRepository.java
 * 
 * Purpose: Repository for managing notification data in Firestore.
 * 
 * Design Pattern: Repository pattern. Provides direct Firebase operations
 * for Notification entities stored in user subcollections.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.repository;

import com.example.arcane.model.Notification;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Repository class for managing notifications in Firestore.
 *
 * <p>Handles CRUD operations for notifications stored in subcollections
 * under user documents: /users/{userId}/notifications/{notificationId}</p>
 *
 * @version 1.0
 */
public class NotificationRepository {
    private static final String COLLECTION_NAME = "users";
    private static final String SUBCOLLECTION_NAME = "notifications";
    private final FirebaseFirestore db;

    /**
     * Default constructor for production use.
     */
    public NotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructor with dependency injection for testing.
     *
     * @param db the Firestore instance
     */
    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }
}

