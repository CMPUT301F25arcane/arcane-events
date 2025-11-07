package com.example.arcane.repository;

import com.example.arcane.model.Decision;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class DecisionRepository {
    private static final String SUBCOLLECTION_NAME = "decisions";
    private final FirebaseFirestore db;

    /**
     * Default constructor for production use
     */
    public DecisionRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructor for testing
     */
    public DecisionRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Create a decision for a user in an event
     */
    public Task<DocumentReference> createDecision(String eventId, Decision decision) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .add(decision);
    }

    /**
     * Get all decisions for an event
     */
    public Task<QuerySnapshot> getDecisionsForEvent(String eventId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .get();
    }

    /**
     * Get decision for a specific user in an event
     */
    public Task<QuerySnapshot> getDecisionForUser(String eventId, String entrantId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }

    /**
     * Get decision by ID
     */
    public Task<DocumentSnapshot> getDecisionById(String eventId, String decisionId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(decisionId)
                .get();
    }

    /**
     * Update decision status
     */
    public Task<Void> updateDecision(String eventId, String decisionId, Decision decision) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(decisionId)
                .set(decision);
    }

    /**
     * Delete decision
     */
    public Task<Void> deleteDecision(String eventId, String decisionId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(decisionId)
                .delete();
    }

    /**
     * Get all decisions for a user across all events
     */
    public Task<QuerySnapshot> getDecisionsByUser(String entrantId) {
        // Collection group query - requires index in Firebase Console
        return db.collectionGroup(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }

    /**
     * Get decisions by status for an event
     */
    public Task<QuerySnapshot> getDecisionsByStatus(String eventId, String status) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
    }
}

