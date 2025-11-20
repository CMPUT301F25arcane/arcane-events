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

    /**
     * Creates a new notification for a user.
     *
     * @param userId the user ID
     * @param notification the notification to create
     * @return a Task that completes with the document reference of the created notification
     */
    public Task<DocumentReference> createNotification(String userId, Notification notification) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .collection(SUBCOLLECTION_NAME)
                .add(notification);
    }

    /**
     * Gets all notifications for a user, ordered by timestamp descending.
     *
     * @param userId the user ID
     * @return a Task that completes with the query snapshot of notifications
     */
    public Task<QuerySnapshot> getNotificationsForUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .collection(SUBCOLLECTION_NAME)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Gets unread notifications for a user.
     *
     * @param userId the user ID
     * @return a Task that completes with the query snapshot of unread notifications
     */
    public Task<QuerySnapshot> getUnreadNotificationsForUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .collection(SUBCOLLECTION_NAME)
                .whereEqualTo("read", false)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Marks a notification as read.
     *
     * @param userId the user ID
     * @param notificationId the notification ID
     * @return a Task that completes when the notification is updated
     */
    public Task<Void> markAsRead(String userId, String notificationId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .collection(SUBCOLLECTION_NAME)
                .document(notificationId)
                .update("read", true);
    }

    /**
     * Gets a notification by ID.
     *
     * @param userId the user ID
     * @param notificationId the notification ID
     * @return a Task that completes with the notification document snapshot
     */
    public Task<DocumentSnapshot> getNotificationById(String userId, String notificationId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .collection(SUBCOLLECTION_NAME)
                .document(notificationId)
                .get();
    }
}

