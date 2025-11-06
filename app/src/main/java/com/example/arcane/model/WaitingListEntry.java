package com.example.arcane.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class WaitingListEntry {
    @DocumentId
    private String entryId;
    
    private String entrantId;  // Reference to UserProfile
    private Timestamp joinTimestamp;
    private Timestamp invitedAt;  // Optional

    // Required no-arg constructor for Firestore
    public WaitingListEntry() {}

    public WaitingListEntry(String entryId, String entrantId, Timestamp joinTimestamp, 
                           Timestamp invitedAt) {
        this.entryId = entryId;
        this.entrantId = entrantId;
        this.joinTimestamp = joinTimestamp;
        this.invitedAt = invitedAt;
    }

    // Getters and Setters
    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getEntrantId() {
        return entrantId;
    }

    public void setEntrantId(String entrantId) {
        this.entrantId = entrantId;
    }

    public Timestamp getJoinTimestamp() {
        return joinTimestamp;
    }

    public void setJoinTimestamp(Timestamp joinTimestamp) {
        this.joinTimestamp = joinTimestamp;
    }

    public Timestamp getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Timestamp invitedAt) {
        this.invitedAt = invitedAt;
    }
}

