/**
 * DecisionRepository.java
 * 
 * Purpose: Data access layer for decision record operations in Firestore.
 * 
 * Design Pattern: Repository pattern. Manages decision records stored as
 * subcollections under events in Firestore.
 * 
 * Outstanding Issues:
 * - Collection group queries require Firebase Console index configuration
 * 
 * @version 1.0
 */
package com.example.arcane.repository;

import com.example.arcane.model.Decision;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Repository class for managing decision records in Firestore.
 *
 * <p>Handles CRUD operations for decision records stored as subcollections
 * under events in Firestore.</p>
 *
 * @version 1.0
 */
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
     * Creates a decision for a user in an event.
     *
     * @param eventId the event ID
     * @param decision the decision to create
     * @return a Task that completes with the document reference of the created decision
     */
    public Task<DocumentReference> createDecision(String eventId, Decision decision) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .add(decision);
    }

    /**
     * Gets all decisions for an event.
     *
     * @param eventId the event ID
     * @return a Task that completes with the query snapshot of decisions
     */
    public Task<QuerySnapshot> getDecisionsForEvent(String eventId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .get();
    }

    /**
     * Gets the decision for a specific user in an event.
     *
     * @param eventId the event ID
     * @param entrantId the entrant's user ID
     * @return a Task that completes with the query snapshot of decisions
     */
    public Task<QuerySnapshot> getDecisionForUser(String eventId, String entrantId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }

    /**
     * Gets a decision by ID.
     *
     * @param eventId the event ID
     * @param decisionId the decision ID
     * @return a Task that completes with the document snapshot of the decision
     */
    public Task<DocumentSnapshot> getDecisionById(String eventId, String decisionId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(decisionId)
                .get();
    }

    /**
     * Updates a decision status.
     *
     * @param eventId the event ID
     * @param decisionId the decision ID to update
     * @param decision the updated decision
     * @return a Task that completes when the decision is updated
     */
    public Task<Void> updateDecision(String eventId, String decisionId, Decision decision) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(decisionId)
                .set(decision);
    }

    /**
     * Deletes a decision.
     *
     * @param eventId the event ID
     * @param decisionId the decision ID to delete
     * @return a Task that completes when the decision is deleted
     */
    public Task<Void> deleteDecision(String eventId, String decisionId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(decisionId)
                .delete();
    }

    /**
     * Gets all decisions for a user across all events.
     *
     * <p>Collection group query - requires index in Firebase Console.</p>
     *
     * @param entrantId the entrant's user ID
     * @return a Task that completes with the query snapshot of decisions
     */
    public Task<QuerySnapshot> getDecisionsByUser(String entrantId) {
        // Collection group query - requires index in Firebase Console
        return db.collectionGroup(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }

    /**
     * Gets decisions by status for an event.
     *
     * @param eventId the event ID
     * @param status the status to filter by
     * @return a Task that completes with the query snapshot of decisions
     */
    public Task<QuerySnapshot> getDecisionsByStatus(String eventId, String status) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
    }
}

