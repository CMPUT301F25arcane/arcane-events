package com.example.arcane.repository;

import com.example.arcane.model.WaitingListEntry;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Repository class for managing waiting list entries in Firestore.
 *
 * <p>Handles CRUD operations for waiting list entries stored as subcollections
 * under events in Firestore.</p>
 *
 * @version 1.0
 */
public class WaitingListRepository {
    private static final String SUBCOLLECTION_NAME = "waitingList";
    private final FirebaseFirestore db;

    /**
     * Constructs a new WaitingListRepository instance.
     */
    public WaitingListRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Adds a user to an event's waiting list (subcollection).
     *
     * @param eventId the event ID
     * @param entry the waiting list entry to add
     * @return a Task that completes with the document reference of the created entry
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
     * @param eventId the event ID
     * @return a Task that completes with the query snapshot of waiting list entries
     */
    public Task<QuerySnapshot> getWaitingListForEvent(String eventId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .get();
    }

    /**
     * Gets a waiting list entry by ID.
     *
     * @param eventId the event ID
     * @param entryId the entry ID
     * @return a Task that completes with the document snapshot of the entry
     */
    public Task<DocumentSnapshot> getWaitingListEntry(String eventId, String entryId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(entryId)
                .get();
    }

    /**
     * Checks if a user is already in the waiting list.
     *
     * @param eventId the event ID
     * @param entrantId the entrant's user ID
     * @return a Task that completes with a query snapshot (empty if not found)
     */
    public Task<QuerySnapshot> checkUserInWaitingList(String eventId, String entrantId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }

    /**
     * Removes a user from the waiting list.
     *
     * @param eventId the event ID
     * @param entryId the entry ID to remove
     * @return a Task that completes when the entry is removed
     */
    public Task<Void> removeFromWaitingList(String eventId, String entryId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(entryId)
                .delete();
    }

    /**
     * Updates a waiting list entry (e.g., set invitedAt).
     *
     * @param eventId the event ID
     * @param entryId the entry ID to update
     * @param entry the updated waiting list entry
     * @return a Task that completes when the entry is updated
     */
    public Task<Void> updateWaitingListEntry(String eventId, String entryId, WaitingListEntry entry) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(entryId)
                .set(entry);
    }

    /**
     * Gets all events where a user is in the waiting list.
     *
     * <p>Note: This requires a collection group query.
     * You'll need to create an index in Firebase Console.</p>
     *
     * @param entrantId the entrant's user ID
     * @return a Task that completes with the query snapshot of waiting list entries
     */
    public Task<QuerySnapshot> getWaitingListEntriesByUser(String entrantId) {
        // Note: This requires a collection group query
        // You'll need to create an index in Firebase Console
        return db.collectionGroup(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }
}

