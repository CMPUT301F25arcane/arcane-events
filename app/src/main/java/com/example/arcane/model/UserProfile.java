package com.example.arcane.model;

/**
 * This file defines the UserProfile class, which represents a user profile in the system.
 * Contains user information including name, email, role, geolocation, and a workaround list
 * of registered event IDs. This class is used as an alternative to Users model in some contexts.
 *
 * Design Pattern: Domain Model Pattern
 * - Represents the core domain entity for user profiles
 * - Follows Firestore data model with DocumentId annotation
 *
 * Outstanding Issues:
 * - registeredEventIds is a workaround for querying user events; should be replaced with proper subcollection queries
 * - Device ID necessity should be reviewed
 */
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;

/**
 * Domain model representing a user profile in the system.
 * Contains user profile information and registered event IDs.
 *
 * @version 1.0
 */
public class UserProfile {
    @DocumentId
    private String userId;
    
    private String deviceId;
    private String name;
    private String email;
    private String role;  // "ENTRANT", "ORGANIZER", "ADMIN"
    private GeoPoint geolocation;  // Optional
    private Boolean notificationOptOut;

    // List of event IDs user has registered for (saved to Firestore for workaround)
    private List<String> registeredEventIds;

    /**
     * Required no-arg constructor for Firestore serialization.
     *
     * @version 1.0
     */
    public UserProfile() {}

    /**
     * Constructs a new UserProfile with all fields.
     *
     * @param userId The user ID (typically Firebase Auth UID)
     * @param deviceId The user's device ID
     * @param name The user's name
     * @param email The user's email address
     * @param role The user's role (ENTRANT, ORGANIZER, ADMIN)
     * @param geolocation Optional geolocation coordinates
     * @param notificationOptOut Whether the user has opted out of notifications
     * @version 1.0
     */
    public UserProfile(String userId, String deviceId, String name, String email, 
                      String role, GeoPoint geolocation, Boolean notificationOptOut) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.geolocation = geolocation;
        this.notificationOptOut = notificationOptOut != null ? notificationOptOut : false;
    }

    /**
     * Gets the user ID.
     *
     * @return The user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     *
     * @param userId The user ID to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Gets the device ID.
     *
     * @return The device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the device ID.
     *
     * @param deviceId The device ID to set
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Gets the user's name.
     *
     * @return The user's name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's name.
     *
     * @param name The user's name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the user's email.
     *
     * @return The user's email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email.
     *
     * @param email The user's email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's role.
     *
     * @return The user's role (ENTRANT, ORGANIZER, ADMIN)
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the user's role.
     *
     * @param role The user's role to set
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the geolocation coordinates.
     *
     * @return The geolocation coordinates
     */
    public GeoPoint getGeolocation() {
        return geolocation;
    }

    /**
     * Sets the geolocation coordinates.
     *
     * @param geolocation The geolocation coordinates to set
     */
    public void setGeolocation(GeoPoint geolocation) {
        this.geolocation = geolocation;
    }

    /**
     * Gets whether the user has opted out of notifications.
     *
     * @return true if opted out, false otherwise
     */
    public Boolean getNotificationOptOut() {
        return notificationOptOut;
    }

    /**
     * Sets whether the user has opted out of notifications.
     *
     * @param notificationOptOut Whether the user has opted out
     */
    public void setNotificationOptOut(Boolean notificationOptOut) {
        this.notificationOptOut = notificationOptOut;
    }

    /**
     * Gets the list of registered event IDs (workaround for querying).
     *
     * @return The list of registered event IDs
     */
    public List<String> getRegisteredEventIds() {
        return registeredEventIds;
    }

    /**
     * Sets the list of registered event IDs (workaround for querying).
     *
     * @param registeredEventIds The list of registered event IDs to set
     */
    public void setRegisteredEventIds(List<String> registeredEventIds) {
        this.registeredEventIds = registeredEventIds;
    }
}

