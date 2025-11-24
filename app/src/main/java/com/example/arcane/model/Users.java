/**
 * Users.java
 * Purpose: Represents a user in the system with authentication and profile information.
 * Design Pattern: Domain Model pattern. This class serves as the user data model
 * for Firestore persistence.
 * Outstanding Issues:
 * - The registeredEventIds field is a workaround for tracking user's registered events;
 *   ideally this should be managed through subcollections or queries
 * - Role field can be null and should have validation
 * 
 * @version 1.0
 */
package com.example.arcane.model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user in the system.
 *
 * <p>Stores user profile information including authentication ID, personal details,
 * role, and a list of registered event IDs (workaround for My Events functionality).</p>
 *
 * @version 1.0
 */
public class Users {

    private String id;         // use auth.uid as doc id
    private String name;
    private String email;
    private String phone;      // optional
    private String deviceId;   // optional
    private Timestamp createdAt;
    private String role;       // e.g., "USER" or "ORGANISER"
    private List<String> registeredEventIds; // workaround array for My Events
    private Boolean notificationOptOut;  // whether user has opted out of notifications
    private Boolean locationTrackingEnabled;  // whether user has enabled location tracking (default: false)
    private String pronouns;   // optional pronouns (e.g., "He/Him", "She/Her", "They/Them")
    private String profilePictureUrl;  // base64 encoded profile picture

    /**
     * Required no-arg constructor for Firestore deserialization.
     * 
     * <p>Initializes default values for fields that should never be null.
     * This ensures backward compatibility with existing Firestore documents
     * that may not have these fields.</p>
     */
    // Required public no-arg constructor (Firestore uses this)
    public Users() {
        this.registeredEventIds = new ArrayList<>();
        this.notificationOptOut = false;
        this.locationTrackingEnabled = false;  // Default to false for privacy-first approach
    }

    /**
     * Creates a new Users instance.
     *
     * @param id the user ID (typically Firebase Auth UID)
     * @param name the user's name
     * @param email the user's email address
     * @param phone the user's phone number (nullable)
     * @param deviceId the device ID (nullable)
     * @param createdAt the timestamp when the user was created
     */
    public Users(String id, String name, String email, String phone, String deviceId, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.deviceId = deviceId;
        this.createdAt = createdAt;
        this.role = null;
        this.registeredEventIds = new ArrayList<>();
        this.notificationOptOut = false;  // default to false (not opted out)
        this.locationTrackingEnabled = false;  // default to false (privacy-first: user must opt-in)
    }

    // Getters & setters
    /**
     * Gets the user ID.
     *
     * @return the user ID
     */
    public String getId() { return id; }

    /**
     * Sets the user ID.
     *
     * @param id the user ID to set
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the user's name.
     *
     * @return the name
     */
    public String getName() { return name; }

    /**
     * Sets the user's name.
     *
     * @param name the name to set
     */
    public void setName(String name) { this.name = name; }

    /**
     * Gets the user's email address.
     *
     * @return the email
     */
    public String getEmail() { return email; }

    /**
     * Sets the user's email address.
     *
     * @param email the email to set
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Gets the user's phone number.
     *
     * @return the phone number, or null if not set
     */
    public String getPhone() { return phone; }

    /**
     * Sets the user's phone number.
     *
     * @param phone the phone number to set (nullable)
     */
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * Gets the device ID.
     *
     * @return the device ID, or null if not set
     */
    public String getDeviceId() { return deviceId; }

    /**
     * Sets the device ID.
     *
     * @param deviceId the device ID to set (nullable)
     */
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    /**
     * Gets the creation timestamp.
     *
     * @return the creation timestamp
     */
    public Timestamp getCreatedAt() { return createdAt; }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt the creation timestamp to set
     */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /**
     * Gets the user's role.
     *
     * @return the role (e.g., "USER" or "ORGANISER")
     */
    public String getRole() { return role; }

    /**
     * Sets the user's role.
     *
     * @param role the role to set (e.g., "USER" or "ORGANISER")
     */
    public void setRole(String role) { this.role = role; }

    /**
     * Gets the list of registered event IDs.
     *
     * @return the list of registered event IDs
     */
    public List<String> getRegisteredEventIds() { return registeredEventIds; }

    /**
     * Sets the list of registered event IDs.
     *
     * @param registeredEventIds the list of registered event IDs to set
     */
    public void setRegisteredEventIds(List<String> registeredEventIds) { this.registeredEventIds = registeredEventIds; }

    /**
     * Gets whether the user has opted out of notifications.
     *
     * <p>Returns false if the field is null (for backward compatibility with
     * existing Firestore documents that don't have this field).</p>
     *
     * @return true if opted out, false otherwise
     */
    public Boolean getNotificationOptOut() {
        return notificationOptOut != null ? notificationOptOut : false;
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
     * Gets whether the user has enabled location tracking.
     *
     * <p>Returns false if the field is null (for backward compatibility with
     * existing Firestore documents that don't have this field).</p>
     *
     * @return true if location tracking is enabled, false otherwise
     */
    public Boolean getLocationTrackingEnabled() {
        return locationTrackingEnabled != null ? locationTrackingEnabled : false;
    }

    /**
     * Sets whether the user has enabled location tracking.
     *
     * @param locationTrackingEnabled true to enable location tracking, false to disable
     */
    public void setLocationTrackingEnabled(Boolean locationTrackingEnabled) {
        this.locationTrackingEnabled = locationTrackingEnabled != null ? locationTrackingEnabled : false;
    }

    /**
     * Gets the user's pronouns.
     *
     * @return the pronouns, or null if not set
     */
    public String getPronouns() {
        return pronouns;
    }

    /**
     * Sets the user's pronouns.
     *
     * @param pronouns the pronouns to set (nullable)
     */
    public void setPronouns(String pronouns) {
        this.pronouns = pronouns;
    }

    /**
     * Gets the profile picture URL (base64 encoded).
     *
     * @return the profile picture URL, or null if not set
     */
    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    /**
     * Sets the profile picture URL (base64 encoded).
     *
     * @param profilePictureUrl the profile picture URL to set (nullable)
     */
    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}

