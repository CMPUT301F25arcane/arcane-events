package com.example.arcane.repository;

import com.example.arcane.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.List;

public class EventRepository {
    private static final String COLLECTION_NAME = "events";
    private final FirebaseFirestore db;

    public EventRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a new event
     */
    public Task<Void> createEvent(Event event) {
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(event.getEventId());
        return docRef.set(event);
    }

    /**
     * Get event by ID
     */
    public Task<DocumentSnapshot> getEventById(String eventId) {
        return db.collection(COLLECTION_NAME).document(eventId).get();
    }

    /**
     * Update event
     */
    public Task<Void> updateEvent(Event event) {
        return db.collection(COLLECTION_NAME).document(event.getEventId()).set(event);
    }

    /**
     * Delete event
     */
    public Task<Void> deleteEvent(String eventId) {
        return db.collection(COLLECTION_NAME).document(eventId).delete();
    }

    /**
     * Get all events
     */
    public Task<QuerySnapshot> getAllEvents() {
        return db.collection(COLLECTION_NAME).get();
    }

    /**
     * Get events by organizer
     */
    public Task<QuerySnapshot> getEventsByOrganizer(String organizerId) {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("organizerId", organizerId)
                .get();
    }

    /**
     * Get open events (for registration)
     */
    public Task<QuerySnapshot> getOpenEvents() {
        return db.collection(COLLECTION_NAME)
                .whereEqualTo("status", "OPEN")
                .get();
    }
}

