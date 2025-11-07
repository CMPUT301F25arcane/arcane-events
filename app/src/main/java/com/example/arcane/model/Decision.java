package com.example.arcane.model;

/**
 * This file defines the Decision class, which represents a decision made for a waiting list entry.
 * Tracks the status of a user's lottery outcome (PENDING, INVITED, ACCEPTED, DECLINED, CANCELLED)
 * and when they responded. Stored as a subcollection under events in Firestore.
 *
 * Design Pattern: Domain Model Pattern
 * - Represents the core domain entity for lottery decisions
 * - Follows Firestore data model with DocumentId annotation
 *
 * Outstanding Issues:
 * - None identified at this time
 */
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Domain model representing a decision for a waiting list entry.
 * Tracks lottery outcome and user response status.
 *
 * @version 1.0
 */
public class Decision {
    @DocumentId
    private String decisionId;
    
    private String entrantId;  // Reference to UserProfile
    private String entryId;  // Reference to WaitingListEntry
    private String status;  // "PENDING", "INVITED", "ACCEPTED", "DECLINED", "CANCELLED"
    private Timestamp respondedAt;  // Optional
    private Timestamp updatedAt;

    /**
     * Required no-arg constructor for Firestore serialization.
     *
     * @version 1.0
     */
    public Decision() {}

    /**
     * Constructs a new Decision with all fields.
     *
     * @param decisionId The unique identifier for the decision
     * @param entrantId The ID of the user (entrant) this decision is for
     * @param entryId The ID of the waiting list entry this decision relates to
     * @param status The decision status (PENDING, INVITED, ACCEPTED, DECLINED, CANCELLED)
     * @param respondedAt The timestamp when the user responded (optional)
     * @param updatedAt The timestamp when the decision was last updated
     * @version 1.0
     */
    public Decision(String decisionId, String entrantId, String entryId, String status, 
                   Timestamp respondedAt, Timestamp updatedAt) {
        this.decisionId = decisionId;
        this.entrantId = entrantId;
        this.entryId = entryId;
        this.status = status;
        this.respondedAt = respondedAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Gets the decision ID.
     *
     * @return The decision ID
     */
    public String getDecisionId() {
        return decisionId;
    }

    /**
     * Sets the decision ID.
     *
     * @param decisionId The decision ID to set
     */
    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
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
     * Gets the decision status.
     *
     * @return The decision status (PENDING, INVITED, ACCEPTED, DECLINED, CANCELLED)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the decision status.
     *
     * @param status The decision status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the timestamp when the user responded.
     *
     * @return The responded at timestamp
     */
    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    /**
     * Sets the timestamp when the user responded.
     *
     * @param respondedAt The responded at timestamp to set
     */
    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    /**
     * Gets the timestamp when the decision was last updated.
     *
     * @return The updated at timestamp
     */
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp when the decision was last updated.
     *
     * @param updatedAt The updated at timestamp to set
     */
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}

