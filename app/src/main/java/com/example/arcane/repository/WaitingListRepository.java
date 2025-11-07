package com.example.arcane.repository;

/**
 * This file defines the WaitingListRepository class, which provides data access methods
 * for WaitingListEntry documents stored as a subcollection under events in Firestore.
 * Handles CRUD operations for waiting list entries including adding, reading, updating,
 * and removing entries. Supports collection group queries to find all waiting list entries
 * for a user across all events.
 *
 * Design Pattern: Repository Pattern
 * - Encapsulates data access logic for WaitingListEntry entities
 * - Provides abstraction over Firestore subcollection operations
 * - Handles Firestore-specific query patterns
 *
 * Outstanding Issues:
 * - Collection group queries require index creation in Firebase Console
 */
import com.example.arcane.model.WaitingListEntry;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Repository class that provides data access methods for WaitingListEntry entities.
 * Manages waiting list entries stored as subcollections under events in Firestore.
 *
 * @version 1.0
 */
public class WaitingListRepository {
    private static final String SUBCOLLECTION_NAME = "waitingList";
    private final FirebaseFirestore db;

    /**
     * Constructs a new WaitingListRepository with the default Firestore instance.
     *
     * @version 1.0
     */
    public WaitingListRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructs a new WaitingListRepository with the specified Firestore instance.
     * Used for dependency injection in tests.
     *
     * @param db The FirebaseFirestore instance to use
     * @version 1.0
     */
    public WaitingListRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Adds a user to an event's waiting list by creating a new waiting list entry.
     *
     * @param eventId The unique identifier of the event
     * @param entry The WaitingListEntry object to add
     * @return A Task that completes with the document reference of the created waiting list entry
     */
    public Task<DocumentReference> addToWaitingList(String eventId, WaitingListEntry entry) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .add(entry);
    }

    /**
     * Gets all waiting list entries for an event.
     *
     * @param eventId The unique identifier of the event
     * @return A Task that completes with a QuerySnapshot containing all waiting list entries for the event
     */
    public Task<QuerySnapshot> getWaitingListForEvent(String eventId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .get();
    }

    /**
     * Gets a waiting list entry document by its ID.
     *
     * @param eventId The unique identifier of the event
     * @param entryId The unique identifier of the waiting list entry
     * @return A Task that completes with the waiting list entry document snapshot
     */
    public Task<DocumentSnapshot> getWaitingListEntry(String eventId, String entryId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(entryId)
                .get();
    }

    /**
     * Checks if a user is already in the waiting list for an event.
     *
     * @param eventId The unique identifier of the event
     * @param entrantId The unique identifier of the user (entrant) to check
     * @return A Task that completes with a QuerySnapshot (empty if user is not in waiting list)
     */
    public Task<QuerySnapshot> checkUserInWaitingList(String eventId, String entrantId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }

    /**
     * Removes a user from the waiting list by deleting the waiting list entry.
     *
     * @param eventId The unique identifier of the event
     * @param entryId The unique identifier of the waiting list entry to remove
     * @return A Task that completes when the waiting list entry is deleted
     */
    public Task<Void> removeFromWaitingList(String eventId, String entryId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(entryId)
                .delete();
    }

    /**
     * Updates a waiting list entry with new data (e.g., set invitedAt timestamp).
     *
     * @param eventId The unique identifier of the event
     * @param entryId The unique identifier of the waiting list entry to update
     * @param entry The WaitingListEntry object containing updated data
     * @return A Task that completes when the waiting list entry is updated
     */
    public Task<Void> updateWaitingListEntry(String eventId, String entryId, WaitingListEntry entry) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(entryId)
                .set(entry);
    }

    /**
     * Gets all waiting list entries for a user across all events using a collection group query.
     * Note: This requires an index to be created in Firebase Console.
     *
     * @param entrantId The unique identifier of the user (entrant)
     * @return A Task that completes with a QuerySnapshot containing all waiting list entries for the user
     */
    public Task<QuerySnapshot> getWaitingListEntriesByUser(String entrantId) {
        // Note: This requires a collection group query
        // You'll need to create an index in Firebase Console
        return db.collectionGroup(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }
}

