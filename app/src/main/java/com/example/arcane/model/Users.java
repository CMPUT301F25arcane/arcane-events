/**
 * Users.java
 * 
 * Purpose: Represents a user in the system with authentication and profile information.
 * 
 * Design Pattern: Domain Model pattern. This class serves as the user data model
 * for Firestore persistence.
 * 
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

    /**
     * Required no-arg constructor for Firestore deserialization.
     */
    // Required public no-arg constructor (Firestore uses this)
    public Users() {}

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
}

