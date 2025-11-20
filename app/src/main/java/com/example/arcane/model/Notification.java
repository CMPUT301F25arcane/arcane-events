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

    // Getters and Setters
    /**
     * Gets the notification document ID.
     *
     * @return the notification ID
     */
    public String getNotificationId() {
        return notificationId;
    }

    /**
     * Sets the notification document ID.
     *
     * @param notificationId the notification ID to set
     */
    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    /**
     * Gets the user ID.
     *
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     *
     * @param userId the user ID to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the event ID.
     *
     * @return the event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the event ID.
     *
     * @param eventId the event ID to set
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the notification type.
     *
     * @return the type ("INVITED" or "LOST")
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the notification type.
     *
     * @param type the type to set ("INVITED" or "LOST")
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the notification title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the notification title.
     *
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the notification message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the notification message.
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the notification timestamp.
     *
     * @return the timestamp
     */
    public Timestamp getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the notification timestamp.
     *
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets whether the notification has been read.
     *
     * @return true if read, false otherwise
     */
    public Boolean getRead() {
        return read;
    }

    /**
     * Sets whether the notification has been read.
     *
     * @param read true if read, false otherwise
     */
    public void setRead(Boolean read) {
        this.read = read;
    }
}

