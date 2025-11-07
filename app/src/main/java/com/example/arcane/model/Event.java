package com.example.arcane.model;

/**
 * This file defines the Event class, which represents an event in the system.
 * Contains all event information including organizer, dates, location, cost, and lottery settings.
 * Uses OOP Composition to manage waiting list entries and decisions in memory (not persisted to Firestore).
 *
 * Design Pattern: Domain Model Pattern, OOP Composition
 * - Represents the core domain entity for events
 * - Uses composition to manage related entities (WaitingListEntry, Decision)
 * - Follows Firestore data model with DocumentId annotation
 *
 * Outstanding Issues:
 * - Waiting list and decisions are loaded separately from subcollections (not auto-loaded)
 * - Description field may need better validation
 * - Image and geolocation fields are optional but not fully implemented in UI
 */
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;

/**
 * Domain model representing an event in the system.
 * Contains event details, registration information, and lottery settings.
 *
 * @version 1.0
 */
public class Event {
    @DocumentId
    private String eventId;

    private String organizerId;  // Reference to UserProfile
    private String eventName;
    private String description;
    private GeoPoint geolocation;  // Optional
    private String posterImageUrl;  // Optional
    private Timestamp eventDate;
    private String location;
    private Double cost;
    private Timestamp registrationStartDate;
    private Timestamp registrationEndDate;
    private Integer maxEntrants;  // Optional
    private Integer numberOfWinners;
    private Boolean geolocationRequired;
    private String status;  // "DRAFT", "OPEN", "CLOSED", "DRAWN", "COMPLETED"

    // OOP Composition - Lists for in-memory operations (NOT saved to Firestore)
    private List<WaitingListEntry> waitingList;  // Loaded separately from subcollection
    private List<Decision> decisions;  // Loaded separately from subcollection

    /**
     * Required no-arg constructor for Firestore serialization.
     *
     * @version 1.0
     */
    public Event() {}

    /**
     * Constructs a new Event with all fields.
     *
     * @param eventId The unique identifier for the event
     * @param organizerId The ID of the user who created the event
     * @param eventName The name of the event
     * @param description The description of the event
     * @param geolocation Optional geolocation coordinates
     * @param posterImageUrl Optional URL to the event poster image
     * @param eventDate The date and time of the event
     * @param location The location name of the event
     * @param cost The cost to attend the event
     * @param registrationStartDate When registration opens
     * @param registrationEndDate When registration closes
     * @param maxEntrants Maximum number of entrants allowed
     * @param numberOfWinners Number of winners in the lottery
     * @param geolocationRequired Whether geolocation is required for registration
     * @param status The current status of the event (DRAFT, OPEN, CLOSED, DRAWN, COMPLETED)
     * @version 1.0
     */
    public Event(String eventId, String organizerId, String eventName, String description,
                 GeoPoint geolocation, String posterImageUrl, Timestamp eventDate, String location,
                 Double cost, Timestamp registrationStartDate, Timestamp registrationEndDate,
                 Integer maxEntrants, Integer numberOfWinners, Boolean geolocationRequired,
                 String status) {
        this.eventId = eventId;
        this.organizerId = organizerId;
        this.eventName = eventName;
        this.description = description;
        this.geolocation = geolocation;
        this.posterImageUrl = posterImageUrl;
        this.eventDate = eventDate;
        this.location = location;
        this.cost = cost;
        this.registrationStartDate = registrationStartDate;
        this.registrationEndDate = registrationEndDate;
        this.maxEntrants = maxEntrants;
        this.numberOfWinners = numberOfWinners;
        this.geolocationRequired = geolocationRequired != null ? geolocationRequired : false;
        this.status = status;
    }

    /**
     * Adds a waiting list entry to the in-memory waiting list.
     * Note: This does not persist to Firestore; entries are stored in subcollection.
     *
     * @param entry The WaitingListEntry to add
     */
    public void addToWaitingList(WaitingListEntry entry) {
        if (waitingList == null) {
            waitingList = new java.util.ArrayList<>();
        }
        waitingList.add(entry);
    }

    /**
     * Gets the in-memory waiting list.
     * Note: This is not persisted to Firestore; entries are stored in subcollection.
     *
     * @return The list of waiting list entries
     */
    public List<WaitingListEntry> getWaitingList() {
        return waitingList;
    }

    /**
     * Sets the in-memory waiting list.
     * Note: This does not persist to Firestore; entries are stored in subcollection.
     *
     * @param waitingList The list of waiting list entries to set
     */
    public void setWaitingList(List<WaitingListEntry> waitingList) {
        this.waitingList = waitingList;
    }

    /**
     * Gets the in-memory decisions list.
     * Note: This is not persisted to Firestore; decisions are stored in subcollection.
     *
     * @return The list of decisions
     */
    public List<Decision> getDecisions() {
        return decisions;
    }

    /**
     * Sets the in-memory decisions list.
     * Note: This does not persist to Firestore; decisions are stored in subcollection.
     *
     * @param decisions The list of decisions to set
     */
    public void setDecisions(List<Decision> decisions) {
        this.decisions = decisions;
    }

    /**
     * Gets the event ID.
     *
     * @return The event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the event ID.
     *
     * @param eventId The event ID to set
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the organizer ID.
     *
     * @return The organizer ID
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets the organizer ID.
     *
     * @param organizerId The organizer ID to set
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Gets the event name.
     *
     * @return The event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the event name.
     *
     * @param eventName The event name to set
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Gets the event description.
     *
     * @return The event description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the event description.
     *
     * @param description The event description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the geolocation coordinates.
     *
     * @return The geolocation coordinates
     */
    public GeoPoint getGeolocation() {
        return geolocation;
    }

    /**
     * Sets the geolocation coordinates.
     *
     * @param geolocation The geolocation coordinates to set
     */
    public void setGeolocation(GeoPoint geolocation) {
        this.geolocation = geolocation;
    }

    /**
     * Gets the poster image URL.
     *
     * @return The poster image URL
     */
    public String getPosterImageUrl() {
        return posterImageUrl;
    }

    /**
     * Sets the poster image URL.
     *
     * @param posterImageUrl The poster image URL to set
     */
    public void setPosterImageUrl(String posterImageUrl) {
        this.posterImageUrl = posterImageUrl;
    }

    /**
     * Gets the event date.
     *
     * @return The event date
     */
    public Timestamp getEventDate() {
        return eventDate;
    }

    /**
     * Sets the event date.
     *
     * @param eventDate The event date to set
     */
    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    /**
     * Gets the event location.
     *
     * @return The event location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the event location.
     *
     * @param location The event location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the event cost.
     *
     * @return The event cost
     */
    public Double getCost() {
        return cost;
    }

    /**
     * Sets the event cost.
     *
     * @param cost The event cost to set
     */
    public void setCost(Double cost) {
        this.cost = cost;
    }

    /**
     * Gets the registration start date.
     *
     * @return The registration start date
     */
    public Timestamp getRegistrationStartDate() {
        return registrationStartDate;
    }

    /**
     * Sets the registration start date.
     *
     * @param registrationStartDate The registration start date to set
     */
    public void setRegistrationStartDate(Timestamp registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    /**
     * Gets the registration end date.
     *
     * @return The registration end date
     */
    public Timestamp getRegistrationEndDate() {
        return registrationEndDate;
    }

    /**
     * Sets the registration end date.
     *
     * @param registrationEndDate The registration end date to set
     */
    public void setRegistrationEndDate(Timestamp registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    /**
     * Gets the maximum number of entrants.
     *
     * @return The maximum number of entrants
     */
    public Integer getMaxEntrants() {
        return maxEntrants;
    }

    /**
     * Sets the maximum number of entrants.
     *
     * @param maxEntrants The maximum number of entrants to set
     */
    public void setMaxEntrants(Integer maxEntrants) {
        this.maxEntrants = maxEntrants;
    }

    /**
     * Gets the number of winners.
     *
     * @return The number of winners
     */
    public Integer getNumberOfWinners() {
        return numberOfWinners;
    }

    /**
     * Sets the number of winners.
     *
     * @param numberOfWinners The number of winners to set
     */
    public void setNumberOfWinners(Integer numberOfWinners) {
        this.numberOfWinners = numberOfWinners;
    }

    /**
     * Gets whether geolocation is required.
     *
     * @return true if geolocation is required, false otherwise
     */
    public Boolean getGeolocationRequired() {
        return geolocationRequired;
    }

    /**
     * Sets whether geolocation is required.
     *
     * @param geolocationRequired Whether geolocation is required
     */
    public void setGeolocationRequired(Boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    /**
     * Gets the event status.
     *
     * @return The event status (DRAFT, OPEN, CLOSED, DRAWN, COMPLETED)
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the event status.
     *
     * @param status The event status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
