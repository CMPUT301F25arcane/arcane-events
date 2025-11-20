/**
 * Notification.java
 * 
 * Purpose: Represents a notification sent to a user.
 * 
 * Design Pattern: Domain Model pattern. This class represents notification data
 * stored in Firestore subcollections under user documents.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Represents a notification sent to a user.
 *
 * <p>Stores notification information including event reference, type, message,
 * and read status. Notifications are stored in subcollections under user documents.</p>
 *
 * @version 1.0
 */
public class Notification {
    @DocumentId
    private String notificationId;

    private String userId;
    private String eventId;
    private String type;  // "INVITED", "LOST"
    private String title;
    private String message;
    private Timestamp timestamp;
    private Boolean read;

    /**
     * Required no-arg constructor for Firestore deserialization.
     */
    public Notification() {}

    /**
     * Creates a fully-specified Notification.
     *
     * @param notificationId the notification document ID (nullable before creation)
     * @param userId the user ID this notification is for
     * @param eventId the event ID this notification relates to
     * @param type the notification type ("INVITED" or "LOST")
     * @param title the notification title
     * @param message the notification message
     * @param timestamp when the notification was created
     * @param read whether the notification has been read
     */
    public Notification(String notificationId, String userId, String eventId, String type,
                       String title, String message, Timestamp timestamp, Boolean read) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.eventId = eventId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read != null ? read : false;
    }
}

