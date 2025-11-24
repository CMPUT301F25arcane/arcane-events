/**
 * WaitingListEntry.java
 * 
 * Purpose: Represents an entry in the waiting list for an event.
 * 
 * Design Pattern: Domain Model pattern. Models the relationship between
 * an entrant and an event's waiting list.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;

/**
 * Represents an entry in the waiting list for an event.
 *
 * <p>Tracks the entrant who joined the waiting list, when they joined,
 * where they were when they joined (optional), and optionally when they were invited by the organizer.</p>
 *
 * @version 1.0
 */
public class WaitingListEntry {
    @DocumentId
    private String entryId;
    
    private String entrantId;  // Reference to UserProfile
    private Timestamp joinTimestamp;
    private Timestamp invitedAt;  // Optional
    private GeoPoint joinLocation;  // Optional - location where user joined the waitlist

    /**
     * Required no-arg constructor for Firestore deserialization.
     */
    // Required no-arg constructor for Firestore
    public WaitingListEntry() {}

    /**
     * Creates a fully-specified WaitingListEntry.
     *
     * @param entryId the Firestore document ID (may be null before creation)
     * @param entrantId the ID of the entrant (UserProfile reference)
     * @param joinTimestamp when the entrant joined the waiting list
     * @param invitedAt when the organizer invited the entrant (nullable)
     * @param joinLocation the geographic location where the entrant joined (nullable)
     */
    public WaitingListEntry(String entryId, String entrantId, Timestamp joinTimestamp, 
                           Timestamp invitedAt, GeoPoint joinLocation) {
        this.entryId = entryId;
        this.entrantId = entrantId;
        this.joinTimestamp = joinTimestamp;
        this.invitedAt = invitedAt;
        this.joinLocation = joinLocation;
    }

    // Getters and Setters
    /**
     * Gets the entry document ID.
     *
     * @return the entry ID
     */
    public String getEntryId() {
        return entryId;
    }

    /**
     * Sets the entry document ID.
     *
     * @param entryId the entry ID to set
     */
    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    /**
     * Gets the entrant (UserProfile) ID.
     *
     * @return the entrant ID
     */
    public String getEntrantId() {
        return entrantId;
    }

    /**
     * Sets the entrant (UserProfile) ID.
     *
     * @param entrantId the entrant ID to set
     */
    public void setEntrantId(String entrantId) {
        this.entrantId = entrantId;
    }

    /**
     * Gets the timestamp when the entrant joined the waiting list.
     *
     * @return the join timestamp
     */
    public Timestamp getJoinTimestamp() {
        return joinTimestamp;
    }

    /**
     * Sets the timestamp when the entrant joined the waiting list.
     *
     * @param joinTimestamp the join timestamp to set
     */
    public void setJoinTimestamp(Timestamp joinTimestamp) {
        this.joinTimestamp = joinTimestamp;
    }

    /**
     * Gets the timestamp when the organizer invited the entrant.
     *
     * @return the invitation timestamp, or null if not yet invited
     */
    public Timestamp getInvitedAt() {
        return invitedAt;
    }

    /**
     * Sets the timestamp when the organizer invited the entrant.
     *
     * @param invitedAt the invitation timestamp to set (nullable)
     */
    public void setInvitedAt(Timestamp invitedAt) {
        this.invitedAt = invitedAt;
    }

    /**
     * Gets the geographic location where the entrant joined the waiting list.
     *
     * @return the join location, or null if not set
     */
    public GeoPoint getJoinLocation() {
        return joinLocation;
    }

    /**
     * Sets the geographic location where the entrant joined the waiting list.
     *
     * @param joinLocation the join location to set (nullable)
     */
    public void setJoinLocation(GeoPoint joinLocation) {
        this.joinLocation = joinLocation;
    }
}

