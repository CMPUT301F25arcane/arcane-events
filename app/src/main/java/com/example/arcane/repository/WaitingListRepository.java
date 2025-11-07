package com.example.arcane.repository;

import com.example.arcane.model.WaitingListEntry;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class WaitingListRepository {
    private static final String SUBCOLLECTION_NAME = "waitingList";
    private final FirebaseFirestore db;

    /**
     * Default constructor for normal use
     */
    public WaitingListRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructor for testing
     */
    public WaitingListRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Add user to event's waiting list (subcollection)
     */
    public Task<DocumentReference> addToWaitingList(String eventId, WaitingListEntry entry) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .add(entry);
    }

    /**
     * Get all waiting list entries for an event
     */
    public Task<QuerySnapshot> getWaitingListForEvent(String eventId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .get();
    }

    /**
     * Get waiting list entry by ID
     */
    public Task<DocumentSnapshot> getWaitingListEntry(String eventId, String entryId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(entryId)
                .get();
    }

    /**
     * Check if user is already in waiting list
     */
    public Task<QuerySnapshot> checkUserInWaitingList(String eventId, String entrantId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }

    /**
     * Remove user from waiting list
     */
    public Task<Void> removeFromWaitingList(String eventId, String entryId) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(entryId)
                .delete();
    }

    /**
     * Update waiting list entry (e.g., set invitedAt)
     */
    public Task<Void> updateWaitingListEntry(String eventId, String entryId, WaitingListEntry entry) {
        return db.collection("events")
                .document(eventId)
                .collection(SUBCOLLECTION_NAME)
                .document(entryId)
                .set(entry);
    }

    /**
     * Get all events where a user is in the waiting list
     */
    public Task<QuerySnapshot> getWaitingListEntriesByUser(String entrantId) {
        // Note: This requires a collection group query
        // You'll need to create an index in Firebase Console
        return db.collectionGroup(SUBCOLLECTION_NAME)
                .whereEqualTo("entrantId", entrantId)
                .get();
    }
}

