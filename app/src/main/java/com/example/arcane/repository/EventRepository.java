package com.example.arcane.repository;

/**
 * This file defines the EventRepository class, which provides data access methods
 * for Event documents stored in Firestore. Handles CRUD operations for events including
 * creating, reading, updating, and deleting. Supports querying events by organizer and status.
 *
 * Design Pattern: Repository Pattern
 * - Encapsulates data access logic for Event entities
 * - Provides abstraction over Firestore collection operations
 * - Handles Firestore-specific query patterns
 *
 * Outstanding Issues:
 * - None identified at this time
 */
import com.example.arcane.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.List;

/**
 * Repository class that provides data access methods for Event entities.
 * Manages events stored in the events collection in Firestore.
 *
 * @version 1.0
 */
public class EventRepository {
    private static final String COLLECTION_NAME = "events";
    private final FirebaseFirestore db;

    /**
     * Constructs a new EventRepository with the default Firestore instance.
     *
     * @version 1.0
     */
    public EventRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructs a new EventRepository with the specified Firestore instance.
     * Used for dependency injection in tests.
     *
     * @param db The FirebaseFirestore instance to use
     * @version 1.0
     */
    public EventRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Creates a new event document in Firestore.
     * If eventId is null or empty, auto-generates a new document ID using .add().
     * If eventId is provided, uses .set() to create the document with that ID.
     *
     * @param event The Event object to create
     * @return A Task that completes with the document reference of the created event
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
     * Gets an event document by its ID.
     *
     * @param eventId The unique identifier of the event
     * @return A Task that completes with the event document snapshot
     */
    public Task<DocumentSnapshot> getEventById(String eventId) {
        return db.collection(COLLECTION_NAME).document(eventId).get();
    }

    /**
     * Updates an event document with new data.
     *
     * @param event The Event object containing updated data (must have a valid eventId)
     * @return A Task that completes when the event is updated
     */
    public Task<Void> updateEvent(Event event) {
        return db.collection(COLLECTION_NAME).document(event.getEventId()).set(event);
    }

    /**
     * Deletes an event document.
     *
     * @param eventId The unique identifier of the event to delete
     * @return A Task that completes when the event is deleted
     */
    public Task<Void> deleteEvent(String eventId) {
        return db.collection(COLLECTION_NAME).document(eventId).delete();
    }

    /**
     * Gets all events from the Firestore collection.
     *
     * @return A Task that completes with a QuerySnapshot containing all events
     */
    public Task<QuerySnapshot> getAllEvents() {
        return db.collection(COLLECTION_NAME).get();
    }

    /**
     * Gets all events created by a specific organizer.
     *
     * @param organizerId The unique identifier of the organizer
     * @return A Task that completes with a QuerySnapshot containing events created by the organizer
     */
    public Task<QuerySnapshot> getEventsByOrganizer(String organizerId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("organizerId", organizerId)
                .get();
    }

    /**
     * Gets all events with status "OPEN" (available for registration).
     *
     * @return A Task that completes with a QuerySnapshot containing all open events
     */
    public Task<QuerySnapshot> getOpenEvents() {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "OPEN")
                .get();
    }
}

