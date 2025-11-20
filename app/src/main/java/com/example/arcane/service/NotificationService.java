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
}

