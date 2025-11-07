package com.example.arcane.model;

/**
 * This file defines the WaitingListEntry class, which represents an entry in an event's waiting list.
 * Tracks when a user joined the waiting list and when they were invited (if applicable).
 * Stored as a subcollection under events in Firestore.
 *
 * Design Pattern: Domain Model Pattern
 * - Represents the core domain entity for waiting list entries
 * - Follows Firestore data model with DocumentId annotation
 *
 * Outstanding Issues:
 * - None identified at this time
 */
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Domain model representing a waiting list entry for an event.
 * Tracks when a user joined and when they were invited.
 *
 * @version 1.0
 */
public class WaitingListEntry {
    @DocumentId
    private String entryId;
    
    private String entrantId;  // Reference to UserProfile
    private Timestamp joinTimestamp;
    private Timestamp invitedAt;  // Optional

    /**
     * Required no-arg constructor for Firestore serialization.
     *
     * @version 1.0
     */
    public WaitingListEntry() {}

    /**
     * Constructs a new WaitingListEntry with all fields.
     *
     * @param entryId The unique identifier for the waiting list entry
     * @param entrantId The ID of the user (entrant) who joined the waiting list
     * @param joinTimestamp The timestamp when the user joined the waiting list
     * @param invitedAt The timestamp when the user was invited (optional, null if not invited yet)
     * @version 1.0
     */
    public WaitingListEntry(String entryId, String entrantId, Timestamp joinTimestamp, 
                           Timestamp invitedAt) {
        this.entryId = entryId;
        this.entrantId = entrantId;
        this.joinTimestamp = joinTimestamp;
        this.invitedAt = invitedAt;
    }

    /**
     * Gets the entry ID.
     *
     * @return The entry ID
     */
    public String getEntryId() {
        return entryId;
    }

    /**
     * Sets the entry ID.
     *
     * @param entryId The entry ID to set
     */
    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    /**
     * Gets the entrant ID.
     *
     * @return The entrant ID
     */
    public String getEntrantId() {
        return entrantId;
    }

    /**
     * Sets the entrant ID.
     *
     * @param entrantId The entrant ID to set
     */
    public void setEntrantId(String entrantId) {
        this.entrantId = entrantId;
    }

    /**
     * Gets the join timestamp.
     *
     * @return The timestamp when the user joined the waiting list
     */
    public Timestamp getJoinTimestamp() {
        return joinTimestamp;
    }

    /**
     * Sets the join timestamp.
     *
     * @param joinTimestamp The join timestamp to set
     */
    public void setJoinTimestamp(Timestamp joinTimestamp) {
        this.joinTimestamp = joinTimestamp;
    }

    /**
     * Gets the invited at timestamp.
     *
     * @return The timestamp when the user was invited (null if not invited yet)
     */
    public Timestamp getInvitedAt() {
        return invitedAt;
    }

    /**
     * Sets the invited at timestamp.
     *
     * @param invitedAt The invited at timestamp to set
     */
    public void setInvitedAt(Timestamp invitedAt) {
        this.invitedAt = invitedAt;
    }
}

