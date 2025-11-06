package com.example.arcane.repository;

import com.example.arcane.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
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
     * Constructs a new EventRepository instance.
     */
    public EventRepository() {
        this.db = FirebaseFirestore.getInstance();
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
     * Deletes an event.
     *
     * @param eventId the event ID to delete
     * @return a Task that completes when the event is deleted
     */
    public Task<Void> deleteEvent(String eventId) {
        return db.collection(COLLECTION_NAME).document(eventId).delete();
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

