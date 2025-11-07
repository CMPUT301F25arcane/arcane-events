/**
 * Decision.java
 * 
 * Purpose: Represents an invitation/decision record for a waiting-list entry.
 * 
 * Design Pattern: Domain Model pattern. This class models the decision/invitation
 * status for users who have joined an event's waiting list.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Represents an invitation/decision record for a waiting-list entry.
 *
 * <p>Tracks the entrant, the associated waiting-list entry, the current decision status,
 * and timestamps for responses and last updates.</p>
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

    // Required no-arg constructor for Firestore
    /**
     * Required no-arg constructor for Firestore deserialization.
     */
    public Decision() {}
    /**
     * Creates a fully-specified Decision.
     *
     * @param decisionId the Firestore document ID (may be null before creation)
     * @param entrantId the ID of the entrant (UserProfile reference)
     * @param entryId the ID of the waiting-list entry (WaitingListEntry reference)
     * @param status the decision status: "PENDING", "INVITED", "ACCEPTED", "DECLINED", or "CANCELLED"
     * @param respondedAt timestamp of entrant response (nullable if not yet responded)
     * @param updatedAt last update timestamp
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

    // Getters and Setters
    /**
     * Gets the decision document ID.
     *
     * @return the decision ID
     */
    public String getDecisionId() {
        return decisionId;
    }

    /**
     * Sets the decision document ID.
     *
     * @param decisionId the decision ID to set
     */
    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
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
     * Gets the waiting-list entry ID.
     *
     * @return the waiting-list entry ID
     */
    public String getEntryId() {
        return entryId;
    }

    /**
     * Sets the waiting-list entry ID.
     *
     * @param entryId the waiting-list entry ID to set
     */
    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    /**
     * Gets the decision status.
     *
     * @return the status ("PENDING", "INVITED", "ACCEPTED", "DECLINED", "CANCELLED")
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the decision status.
     *
     * @param status the status to set ("PENDING", "INVITED", "ACCEPTED", "DECLINED", "CANCELLED")
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the timestamp when the entrant responded.
     *
     * @return the response timestamp, or null if no response yet
     */
    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    /**
     * Sets the timestamp when the entrant responded.
     *
     * @param respondedAt the response timestamp to set (nullable)
     */
    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    /**
     * Gets the last update timestamp.
     *
     * @return the last update timestamp
     */
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp.
     *
     * @param updatedAt the last update timestamp to set
     */
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}

