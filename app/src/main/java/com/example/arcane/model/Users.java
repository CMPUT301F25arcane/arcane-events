package com.example.arcane.model;

/**
 * This file defines the Users class, which represents a user in the system.
 * Contains user information including name, email, phone, role, and a workaround list
 * of registered event IDs. This class is used for Firestore user documents.
 *
 * Design Pattern: Domain Model Pattern
 * - Represents the core domain entity for users
 * - Follows Firestore data model requirements
 *
 * Outstanding Issues:
 * - registeredEventIds is a workaround for querying user events; should be replaced with proper subcollection queries
 * - Role validation could be improved
 */
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a user in the system.
 * Contains user profile information and registered event IDs.
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
     * Required public no-arg constructor for Firestore serialization.
     *
     * @version 1.0
     */
    public Users() {}

    /**
     * Constructs a new Users object with basic user information.
     *
     * @param id The user ID (typically Firebase Auth UID)
     * @param name The user's name
     * @param email The user's email address
     * @param phone The user's phone number (optional)
     * @param deviceId The user's device ID (optional)
     * @param createdAt The timestamp when the user was created
     * @version 1.0
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

    /**
     * Gets the user ID.
     *
     * @return The user ID
     */
    public String getId() { return id; }

    /**
     * Sets the user ID.
     *
     * @param id The user ID to set
     */
    public void setId(String id) { this.id = id; }

    /**
     * Gets the user's name.
     *
     * @return The user's name
     */
    public String getName() { return name; }

    /**
     * Sets the user's name.
     *
     * @param name The user's name to set
     */
    public void setName(String name) { this.name = name; }

    /**
     * Gets the user's email.
     *
     * @return The user's email
     */
    public String getEmail() { return email; }

    /**
     * Sets the user's email.
     *
     * @param email The user's email to set
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Gets the user's phone number.
     *
     * @return The user's phone number
     */
    public String getPhone() { return phone; }

    /**
     * Sets the user's phone number.
     *
     * @param phone The user's phone number to set
     */
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * Gets the user's device ID.
     *
     * @return The user's device ID
     */
    public String getDeviceId() { return deviceId; }

    /**
     * Sets the user's device ID.
     *
     * @param deviceId The user's device ID to set
     */
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    /**
     * Gets the creation timestamp.
     *
     * @return The creation timestamp
     */
    public Timestamp getCreatedAt() { return createdAt; }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt The creation timestamp to set
     */
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /**
     * Gets the user's role.
     *
     * @return The user's role (e.g., "USER" or "ORGANISER")
     */
    public String getRole() { return role; }

    /**
     * Sets the user's role.
     *
     * @param role The user's role to set
     */
    public void setRole(String role) { this.role = role; }

    /**
     * Gets the list of registered event IDs (workaround for querying).
     *
     * @return The list of registered event IDs
     */
    public List<String> getRegisteredEventIds() { return registeredEventIds; }

    /**
     * Sets the list of registered event IDs (workaround for querying).
     *
     * @param registeredEventIds The list of registered event IDs to set
     */
    public void setRegisteredEventIds(List<String> registeredEventIds) { this.registeredEventIds = registeredEventIds; }
}

