/**
 * NotificationService.java
 * 
 * Purpose: Service layer for managing notification operations.
 * 
 * Design Pattern: Service Layer pattern. Orchestrates notification-related
 * business logic and coordinates with repositories.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.service;

import com.example.arcane.model.Notification;
import com.example.arcane.repository.NotificationRepository;
import com.example.arcane.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Service class for managing notification operations.
 *
 * <p>Handles notification creation, retrieval, and read status updates.
 * Checks user notification preferences before sending notifications.</p>
 *
 * @version 1.0
 */
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Constructs a new NotificationService instance.
     */
    public NotificationService() {
        this(new NotificationRepository(), new UserRepository());
    }

    /**
     * Constructor for dependency injection (used in tests).
     *
     * @param notificationRepository the notification repository
     * @param userRepository the user repository
     */
    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Sends a notification to a user if they haven't opted out.
     *
     * @param userId the user ID
     * @param eventId the event ID
     * @param type the notification type ("INVITED" or "LOST")
     * @param title the notification title
     * @param message the notification message
     * @return a Task that completes with the document reference if sent, or null if user opted out
     */
    public Task<DocumentReference> sendNotification(String userId, String eventId, String type,
                                                    String title, String message) {
        // Check if user has opted out
        return userRepository.getUserById(userId)
                .continueWithTask(userTask -> {
                    if (!userTask.isSuccessful() || userTask.getResult() == null || !userTask.getResult().exists()) {
                        // User not found, don't send notification
                        return com.google.android.gms.tasks.Tasks.forResult((DocumentReference) null);
                    }

                    DocumentSnapshot userDoc = userTask.getResult();
                    Boolean notificationOptOut = userDoc.getBoolean("notificationOptOut");
                    
                    if (Boolean.TRUE.equals(notificationOptOut)) {
                        // User has opted out, don't send notification
                        return com.google.android.gms.tasks.Tasks.forResult((DocumentReference) null);
                    }

                    // User hasn't opted out, create notification
                    Notification notification = new Notification(
                            null,  // notificationId will be set by Firestore
                            userId,
                            eventId,
                            type,
                            title,
                            message,
                            Timestamp.now(),
                            false  // unread by default
                    );

                    return notificationRepository.createNotification(userId, notification);
                });
    }

    /**
     * Gets all notifications for a user.
     *
     * @param userId the user ID
     * @return a Task that completes with the query snapshot of notifications
     */
    public Task<QuerySnapshot> getUserNotifications(String userId) {
        return notificationRepository.getNotificationsForUser(userId);
    }

    /**
     * Gets unread notifications for a user.
     *
     * @param userId the user ID
     * @return a Task that completes with the query snapshot of unread notifications
     */
    public Task<QuerySnapshot> getUnreadNotifications(String userId) {
        return notificationRepository.getUnreadNotificationsForUser(userId);
    }

    /**
     * Marks a notification as read.
     *
     * @param userId the user ID
     * @param notificationId the notification ID
     * @return a Task that completes when the notification is marked as read
     */
    public Task<Void> markNotificationRead(String userId, String notificationId) {
        return notificationRepository.markAsRead(userId, notificationId);
    }
}

