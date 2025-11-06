/**
 * UserProfile.java
 * 
 * Purpose: Represents a user profile in the system with extended profile information.
 * 
 * Design Pattern: Domain Model pattern. This class provides a more detailed user profile
 * compared to the Users class, including geolocation and notification preferences.
 * 
 * Outstanding Issues:
 * - The registeredEventIds field is a workaround for tracking user's registered events;
 *   ideally this should be managed through subcollections or queries
 * - Device ID field may not be necessary if using Firebase Auth
 * 
 * @version 1.0
 */
package com.example.arcane.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;

/**
 * Represents a user profile in the system.
 *
 * <p>Stores user information including device ID, personal details, role,
 * geolocation, notification preferences, and a list of registered event IDs.</p>
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
     * Required no-arg constructor for Firestore deserialization.
     */
    // Required no-arg constructor for Firestore
    public UserProfile() {}

    /**
     * Creates a fully-specified UserProfile.
     *
     * @param userId the user ID (Firestore document ID)
     * @param deviceId the device ID
     * @param name the user's name
     * @param email the user's email address
     * @param role the user's role ("ENTRANT", "ORGANIZER", "ADMIN")
     * @param geolocation the user's geographic location (nullable)
     * @param notificationOptOut whether the user has opted out of notifications
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

    // Getters and Setters
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
     * Gets the device ID.
     *
     * @return the device ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * Sets the device ID.
     *
     * @param deviceId the device ID to set
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Gets the user's name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the user's name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the user's email address.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     *
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's role.
     *
     * @return the role ("ENTRANT", "ORGANIZER", "ADMIN")
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the user's role.
     *
     * @param role the role to set ("ENTRANT", "ORGANIZER", "ADMIN")
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * Gets the user's geographic location.
     *
     * @return the geolocation, or null if not set
     */
    public GeoPoint getGeolocation() {
        return geolocation;
    }

    /**
     * Sets the user's geographic location.
     *
     * @param geolocation the geolocation to set (nullable)
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
     * @param notificationOptOut true if opted out, false otherwise
     */
    public void setNotificationOptOut(Boolean notificationOptOut) {
        this.notificationOptOut = notificationOptOut;
    }

    /**
     * Gets the list of registered event IDs.
     *
     * @return the list of registered event IDs
     */
    public List<String> getRegisteredEventIds() {
        return registeredEventIds;
    }

    /**
     * Sets the list of registered event IDs.
     *
     * @param registeredEventIds the list of registered event IDs to set
     */
    public void setRegisteredEventIds(List<String> registeredEventIds) {
        this.registeredEventIds = registeredEventIds;
    }
}

