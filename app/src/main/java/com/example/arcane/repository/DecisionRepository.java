package com.example.arcane.repository;

/**
 * This file defines the DecisionRepository class, which provides data access methods
 * for Decision documents stored as a subcollection under events in Firestore.
 * Handles CRUD operations for decisions including creating, reading, updating, and deleting.
 * Supports collection group queries to find all decisions for a user across all events.
 *
 * Design Pattern: Repository Pattern
 * - Encapsulates data access logic for Decision entities
 * - Provides abstraction over Firestore subcollection operations
 * - Handles Firestore-specific query patterns
 *
 * Outstanding Issues:
 * - Collection group queries require index creation in Firebase Console
 */
import com.example.arcane.model.Decision;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Repository class that provides data access methods for Decision entities.
 * Manages decisions stored as subcollections under events in Firestore.
 *
 * @version 1.0
 */
public class DecisionRepository {
    private static final String SUBCOLLECTION_NAME = "decisions";
    private final FirebaseFirestore db;

    /**
     * Constructs a new DecisionRepository with the default Firestore instance.
     *
     * @version 1.0
     */
    public DecisionRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructs a new DecisionRepository with the specified Firestore instance.
     * Used for dependency injection in tests.
     *
     * @param db The FirebaseFirestore instance to use
     * @version 1.0
     */
    public DecisionRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Creates a new decision document in the decisions subcollection for an event.
     *
     * @param eventId The unique identifier of the event
     * @param decision The Decision object to create
     * @return A Task that completes with the document reference of the created decision
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
     * @param eventId The unique identifier of the event
     * @return A Task that completes with a QuerySnapshot containing all decisions for the event
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
     * @param eventId The unique identifier of the event
     * @param entrantId The unique identifier of the user (entrant)
     * @return A Task that completes with a QuerySnapshot containing the decision(s) for the user
     */
    public Task<QuerySnapshot> getDecisionForUser(String eventId, String entrantId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }

    /**
     * Gets a decision document by its ID.
     *
     * @param eventId The unique identifier of the event
     * @param decisionId The unique identifier of the decision
     * @return A Task that completes with the decision document snapshot
     */
    public Task<DocumentSnapshot> getDecisionById(String eventId, String decisionId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(decisionId)
                .get();
    }

    /**
     * Updates a decision document with new data.
     *
     * @param eventId The unique identifier of the event
     * @param decisionId The unique identifier of the decision to update
     * @param decision The Decision object containing updated data
     * @return A Task that completes when the decision is updated
     */
    public Task<Void> updateDecision(String eventId, String decisionId, Decision decision) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(decisionId)
                .set(decision);
    }

    /**
     * Deletes a decision document.
     *
     * @param eventId The unique identifier of the event
     * @param decisionId The unique identifier of the decision to delete
     * @return A Task that completes when the decision is deleted
     */
    public Task<Void> deleteDecision(String eventId, String decisionId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(decisionId)
                .delete();
    }

    /**
     * Gets all decisions for a user across all events using a collection group query.
     * Note: This requires an index to be created in Firebase Console.
     *
     * @param entrantId The unique identifier of the user (entrant)
     * @return A Task that completes with a QuerySnapshot containing all decisions for the user
     */
    public Task<QuerySnapshot> getDecisionsByUser(String entrantId) {
        // Collection group query - requires index in Firebase Console
        return db.collectionGroup(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }

    /**
     * Gets all decisions with a specific status for an event.
     *
     * @param eventId The unique identifier of the event
     * @param status The decision status to filter by (e.g., "PENDING", "INVITED", "ACCEPTED", "DECLINED")
     * @return A Task that completes with a QuerySnapshot containing decisions with the specified status
     */
    public Task<QuerySnapshot> getDecisionsByStatus(String eventId, String status) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .whereEqualTo("status", status)
                .get();
    }
}

