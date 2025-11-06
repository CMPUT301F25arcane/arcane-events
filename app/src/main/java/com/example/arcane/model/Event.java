package com.example.arcane.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class Event {
    @DocumentId
    private String id;

    // --- Metadata ---
    private String name;
    private String description;
    private Long capacity;                 // change to String if your Firestore field is string
    private Long attendeeCount;
    private Timestamp registrationStart;
    private Timestamp registrationEnd;
    private String locationName;
    private GeoPoint geo;
    private String posterUrl;
    private Boolean geolocationRequired;
    private Timestamp createdAt;

    // Organizer is a User object kept in metadata
    private Users organizer;

    // --- Waiting queue (entrant user IDs) ---
    // Stored as an array in Firestore; treated as a FIFO queue with helpers below.
    private List<String> waitingQueue;

    public Event() {
        this.waitingQueue = new ArrayList<>();
    }

    public Event(String id,
                 String name,
                 String description,
                 Long capacity,
                 Long attendeeCount,
                 Timestamp registrationStart,
                 Timestamp registrationEnd,
                 String locationName,
                 GeoPoint geo,
                 String posterUrl,
                 Boolean geolocationRequired,
                 Timestamp createdAt,
                 Users organizer,
                 List<String> waitingQueue) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.attendeeCount = attendeeCount;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.locationName = locationName;
        this.geo = geo;
        this.posterUrl = posterUrl;
        this.geolocationRequired = geolocationRequired;
        this.createdAt = createdAt;
        this.organizer = organizer;
        this.waitingQueue = (waitingQueue != null) ? waitingQueue : new ArrayList<>();
    }

    // ---------- Queue helpers ----------
    public void enqueueEntrant(String userId) {
        if (waitingQueue == null) waitingQueue = new ArrayList<>();
        waitingQueue.add(userId); // push to back
    }

    public String dequeueEntrant() {
        if (waitingQueue == null || waitingQueue.isEmpty()) return null;
        return waitingQueue.remove(0); // pop from front (FIFO)
    }

    public String peekNextEntrant() {
        if (waitingQueue == null || waitingQueue.isEmpty()) return null;
        return waitingQueue.get(0);
    }

    public boolean removeEntrant(String userId) {
        if (waitingQueue == null) return false;
        return waitingQueue.remove(userId);
    }

    public int getQueueSize() {
        return (waitingQueue == null) ? 0 : waitingQueue.size();
    }

    // ---------- Getters / Setters ----------
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCapacity() { return capacity; }
    public void setCapacity(Long capacity) { this.capacity = capacity; }

    public Long getAttendeeCount() { return attendeeCount; }
    public void setAttendeeCount(Long attendeeCount) { this.attendeeCount = attendeeCount; }

    public Timestamp getRegistrationStart() { return registrationStart; }
    public void setRegistrationStart(Timestamp registrationStart) { this.registrationStart = registrationStart; }

    public Timestamp getRegistrationEnd() { return registrationEnd; }
    public void setRegistrationEnd(Timestamp registrationEnd) { this.registrationEnd = registrationEnd; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public GeoPoint getGeo() { return geo; }
    public void setGeo(GeoPoint geo) { this.geo = geo; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public Boolean getGeolocationRequired() { return geolocationRequired; }
    public void setGeolocationRequired(Boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Users getOrganizer() { return organizer; }
    public void setOrganizer(Users organizer) { this.organizer = organizer; }

    public List<String> getWaitingQueue() { return waitingQueue; }
    public void setWaitingQueue(List<String> waitingQueue) {
        this.waitingQueue = (waitingQueue != null) ? waitingQueue : new ArrayList<>();
    }
}
