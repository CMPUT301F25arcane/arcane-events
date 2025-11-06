package com.example.arcane.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class Decision {
    @DocumentId
    private String decisionId;
    
    private String entrantId;  // Reference to UserProfile
    private String entryId;  // Reference to WaitingListEntry
    private String status;  // "PENDING", "INVITED", "ACCEPTED", "DECLINED", "CANCELLED"
    private Timestamp respondedAt;  // Optional
    private Timestamp updatedAt;

    // Required no-arg constructor for Firestore
    public Decision() {}

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
    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public String getEntrantId() {
        return entrantId;
    }

    public void setEntrantId(String entrantId) {
        this.entrantId = entrantId;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}

