/**
 * EventRepository.java
 * 
 * Purpose: Data access layer for event operations in Firestore.
 * 
 * Design Pattern: Repository pattern. Encapsulates Firestore queries and operations
 * for events, providing a clean abstraction for the service layer.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.repository;

import com.example.arcane.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.Map;
import java.util.List;

/**
 * Repository class for managing events in Firestore.
 *
 * <p>Handles CRUD operations for events stored in the "events" collection.</p>
 *
 * @version 1.0
 */
public class EventRepository {
    private static final String COLLECTION_NAME = "events";
    private final FirebaseFirestore db;

    /**
     * Default constructor for normal use
     */
    public EventRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructor for testing
     */
    public EventRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Creates a new event.
     *
     * <p>If eventId is null, auto-generates a new document ID using .add().</p>
     *
     * @param event the event to create
     * @return a Task that completes with the document reference of the created event
     */
    public Task<DocumentReference> createEvent(Event event) {
        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            // New event - auto-generate ID using .add()
            return db.collection(COLLECTION_NAME)
                    .add(event)
                    .continueWith(task -> {
                        if (task.isSuccessful()) {
                            DocumentReference docRef = task.getResult();
                            // Update event with the generated ID
                            event.setEventId(docRef.getId());
                            return docRef;
                        } else {
                            throw task.getException();
                        }
                    });
        } else {
            // Existing event with ID - use .set()
            DocumentReference docRef = db.collection(COLLECTION_NAME).document(event.getEventId());
            return docRef.set(event).continueWith(task -> {
                if (task.isSuccessful()) {
                    return docRef;
                } else {
                    throw task.getException();
                }
            });
        }
    }

    /**
     * Gets an event by ID.
     *
     * @param eventId the event ID to retrieve
     * @return a Task that completes with the event document snapshot
     */
    public Task<DocumentSnapshot> getEventById(String eventId) {
        return db.collection(COLLECTION_NAME).document(eventId).get();
    }

    /**
     * Updates an event.
     *
     * @param event the event to update
     * @return a Task that completes when the event is updated
     */
    public Task<Void> updateEvent(Event event) {
        return db.collection(COLLECTION_NAME).document(event.getEventId()).set(event);
    }

    /**
     * Partially updates an event with the provided fields.
     *
     * @param eventId the event ID to update
     * @param updates the map of fields to update
     * @return a Task that completes when the event is updated
     */
    public Task<Void> updateEventFields(String eventId, Map<String, Object> updates) {
        return db.collection(COLLECTION_NAME).document(eventId).update(updates);
    }

    /**
     * Deletes an event and all its subcollections (waitingList, decisions).
     * 
     * <p>In Firestore, deleting a document does not automatically delete its subcollections.
     * This method deletes all subcollection documents before deleting the event document
     * to ensure complete removal from the database.</p>
     *
     * @param eventId the event ID to delete
     * @return a Task that completes when the event and all subcollections are deleted
     */
    public Task<Void> deleteEvent(String eventId) {
        Task<Void> deleteWaitingList = deleteSubcollection(eventId, "waitingList");
        Task<Void> deleteDecisions = deleteSubcollection(eventId, "decisions");
        
        // Wait for both subcollections to be deleted, then delete the event document
        return Tasks.whenAll(deleteWaitingList, deleteDecisions)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        // Now delete the event document itself
                        return db.collection(COLLECTION_NAME).document(eventId).delete();
                    } else {
                        throw task.getException();
                    }
                })
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return null; // Return Void
                    } else {
                        throw task.getException();
                    }
                });
    }
    
    /**
     * Deletes all documents in a subcollection.
     * 
     * <p>Uses batch writes to efficiently delete multiple documents.
     * Firestore batch limit is 500 operations, so this handles large subcollections.</p>
     *
     * @param eventId the event ID
     * @param subcollectionName the name of the subcollection to delete
     * @return a Task that completes when all documents in the subcollection are deleted
     */
    private Task<Void> deleteSubcollection(String eventId, String subcollectionName) {
        return db.collection(COLLECTION_NAME)
                .document(eventId)
                .collection(subcollectionName)
                .get()
                .continueWithTask(querySnapshotTask -> {
                    if (!querySnapshotTask.isSuccessful()) {
                        // If subcollection doesn't exist or is empty, that's fine
                        return Tasks.forResult(null);
                    }
                    
                    QuerySnapshot querySnapshot = querySnapshotTask.getResult();
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        return Tasks.forResult(null);
                    }
                    
                    // Delete documents in batches (Firestore batch limit is 500)
                    List<Task<Void>> batchTasks = new java.util.ArrayList<>();
                    WriteBatch batch = db.batch();
                    int batchCount = 0;
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        batch.delete(doc.getReference());
                        batchCount++;
                        
                        // Commit batch when reaching limit
                        if (batchCount >= 500) {
                            batchTasks.add(batch.commit());
                            batch = db.batch();
                            batchCount = 0;
                        }
                    }
                    
                    // Commit remaining batch if any
                    if (batchCount > 0) {
                        batchTasks.add(batch.commit());
                    }
                    
                    // Wait for all batches to complete
                    return batchTasks.isEmpty() ? Tasks.forResult(null) : Tasks.whenAll(batchTasks);
                })
                .continueWith(task -> null); // Return Void
    }

    /**
     * Gets all events.
     *
     * @return a Task that completes with the query snapshot of all events
     */
    public Task<QuerySnapshot> getAllEvents() {
        return db.collection(COLLECTION_NAME).get();
    }

    /**
     * Gets events by organizer.
     *
     * @param organizerId the organizer's user ID
     * @return a Task that completes with the query snapshot of events
     */
    public Task<QuerySnapshot> getEventsByOrganizer(String organizerId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("organizerId", organizerId)
                .get();
    }

    /**
     * Gets open events (for registration).
     *
     * @return a Task that completes with the query snapshot of open events
     */
    public Task<QuerySnapshot> getOpenEvents() {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "OPEN")
                .get();
    }
}

