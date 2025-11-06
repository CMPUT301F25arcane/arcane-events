package com.arcaneevents.model;

import com.google.firebase.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Notification model representing system and organizer messages to users.
 *
 * User Stories Covered:
 *  - US 01.04.01: Entrant notified when chosen ("WON")
 *  - US 01.04.02: Entrant notified when not chosen ("LOST")
 *  - US 01.04.03: Entrant can opt out of organizer/admin notifications
 *  - US 02.05.01: Organizer sends notification to chosen entrants
 *  - US 02.07.01–02.07.03: Organizer sends group notifications (waiting list, selected, cancelled)
 *  - US 03.08.01: Administrator reviews all notifications (audit logs)
 */
public class Notification {

    private String notificationId;
    private String recipientId;
    private String senderId;      // Organizer/Admin ID (optional)
    private String eventId;       // Event reference (optional)
    private String type;          // "WON", "LOST", "INVITED", etc.
    private String title;
    private String message;
    private boolean read;
    private Timestamp createdAt;

    // ===== Default constructor (required for Firebase) =====
    public Notification() {
        this.createdAt = Timestamp.now();
        this.read = false;
    }

    // ===== Full constructor =====
    public Notification(String notificationId, String recipientId, String senderId,
                        String eventId, String type, String title,
                        String message, boolean read, Timestamp createdAt) {
        this.notificationId = notificationId;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.eventId = eventId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt != null ? createdAt : Timestamp.now();
    }

    // ===== Getters =====
    public String getNotificationId() { return notificationId; }
    public String getRecipientId() { return recipientId; }
    public String getSenderId() { return senderId; }
    public String getEventId() { return eventId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public Timestamp getCreatedAt() { return createdAt; }

    // ===== Setters with Validation =====
    public void setNotificationId(String notificationId) {
        if (notificationId != null && !notificationId.trim().isEmpty()) {
            this.notificationId = notificationId.trim();
        }
    }

    public void setRecipientId(String recipientId) {
        if (recipientId != null && !recipientId.trim().isEmpty()) {
            this.recipientId = recipientId.trim();
        }
    }

    public void setSenderId(String senderId) {
        if (senderId != null && !senderId.trim().isEmpty()) {
            this.senderId = senderId.trim();
        }
    }

    public void setEventId(String eventId) {
        if (eventId != null && !eventId.trim().isEmpty()) {
            this.eventId = eventId.trim();
        }
    }

    public void setType(String type) {
        List<String> allowedTypes = Arrays.asList(
                "WON", "LOST", "INVITED", "DECLINED",
                "REPLACEMENT_DRAWN", "ORGANIZER_MESSAGE"
        );
        if (allowedTypes.contains(type)) {
            this.type = type;
        }
    }

    public void setTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title.trim();
        }
    }

    public void setMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            this.message = message.trim();
        }
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void markAsRead() {
        this.read = true;
    }

    public void markAsUnread() {
        this.read = false;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = (createdAt != null) ? createdAt : Timestamp.now();
    }

    // ===== Helper for debugging / admin logs =====
    @Override
    public String toString() {
        return "Notification{" +
                "notificationId='" + notificationId + '\'' +
                ", recipientId='" + recipientId + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", read=" + read +
                ", createdAt=" + createdAt +
                '}';
    }
}
