/**
 * Event.java
 * 
 * Purpose: Domain model representing an event that users can register for and attend.
 * 
 * Design Pattern: This class follows the Domain Model pattern and uses OOP Composition
 * to manage related entities (waiting list entries and decisions) in-memory.
 * The class is designed to work with Firestore for persistence.
 * 
 * Outstanding Issues:
 * - Waiting list and decisions are loaded separately and composed in-memory;
 *   this requires explicit loading via EventService.getEventWithDetails()
 * - Description field is currently not used in the CreateEventFragment UI
 * - Poster image URL and geolocation are set to null and need to be implemented
 * 
 * @version 1.0
 */
package com.example.arcane.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;

/**
 * Domain model representing an event that users can register for and attend.
 *
 * <p>Stores organizer information, scheduling details, location, registration window,
 * capacity constraints, and current lifecycle status. Related collections (waiting list
 * and decisions) are composed in-memory for convenience and are expected to be loaded
 * separately from Firestore.</p>
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
    private String qrCodeImageBase64;
    private Integer qrStyleVersion;

    // OOP Composition - Lists for in-memory operations (NOT saved to Firestore)
    private List<WaitingListEntry> waitingList;  // Loaded separately from subcollection
    private List<Decision> decisions;  // Loaded separately from subcollection
    /**
     * Required no-arg constructor for Firestore deserialization.
     */
    // Required no-arg constructor for Firestore
    public Event() {}
    /**
     * Creates a fully-specified Event.
     *
     * @param eventId the Firestore document ID (nullable before creation)
     * @param organizerId the organizer's user/profile ID
     * @param eventName the human-readable name of the event
     * @param description a short description or details of the event
     * @param geolocation optional geographic location of the event
     * @param posterImageUrl optional URL to the event poster image
     * @param eventDate the scheduled date/time of the event
     * @param location a human-readable venue/location string
     * @param cost optional price/cost to attend (nullable for free)
     * @param registrationStartDate when registration opens
     * @param registrationEndDate when registration closes
     * @param maxEntrants optional maximum number of entrants allowed
     * @param numberOfWinners number of winners to draw/choose
     * @param geolocationRequired whether check-in/validation by geolocation is required
     * @param status lifecycle status: "DRAFT", "OPEN", "CLOSED", "DRAWN", or "COMPLETED"
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
     * Adds an entry to the in-memory waiting list (initializes the list if null).
     *
     * @param entry the waiting-list entry to add
     */
    // OOP Methods
    public void addToWaitingList(WaitingListEntry entry) {
        if (waitingList == null) {
            waitingList = new java.util.ArrayList<>();
        }
        waitingList.add(entry);
    }
    /**
     * Returns the in-memory waiting list (may be null if not initialized/loaded).
     *
     * @return the waiting list entries
     */
    public List<WaitingListEntry> getWaitingList() {
        return waitingList;
    }

    /**
     * Sets the waiting list entries.
     *
     * @param waitingList the waiting list entries to set
     */
    public void setWaitingList(List<WaitingListEntry> waitingList) {
        this.waitingList = waitingList;
    }

    /**
     * Returns the in-memory decisions list (may be null if not initialized/loaded).
     *
     * @return the decisions list
     */
    public List<Decision> getDecisions() {
        return decisions;
    }

    /**
     * Sets the decisions list.
     *
     * @param decisions the decisions list to set
     */
    public void setDecisions(List<Decision> decisions) {
        this.decisions = decisions;
    }

    // Getters and Setters
    /**
     * Gets the event document ID.
     *
     * @return the event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the event document ID.
     *
     * @param eventId the event ID to set
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the organizer's user/profile ID.
     *
     * @return the organizer ID
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets the organizer's user/profile ID.
     *
     * @param organizerId the organizer ID to set
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Gets the event name.
     *
     * @return the event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the event name.
     *
     * @param eventName the event name to set
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Gets the event description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the event description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the geographic location of the event.
     *
     * @return the geolocation, or null if not set
     */
    public GeoPoint getGeolocation() {
        return geolocation;
    }

    /**
     * Sets the geographic location of the event.
     *
     * @param geolocation the geolocation to set
     */
    public void setGeolocation(GeoPoint geolocation) {
        this.geolocation = geolocation;
    }

    /**
     * Gets the poster image URL.
     *
     * @return the poster image URL, or null if not set
     */
    public String getPosterImageUrl() {
        return posterImageUrl;
    }

    /**
     * Sets the poster image URL.
     *
     * @param posterImageUrl the poster image URL to set
     */
    public void setPosterImageUrl(String posterImageUrl) {
        this.posterImageUrl = posterImageUrl;
    }

    /**
     * Gets the event date/time.
     *
     * @return the event date
     */
    public Timestamp getEventDate() {
        return eventDate;
    }

    /**
     * Sets the event date/time.
     *
     * @param eventDate the event date to set
     */
    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    /**
     * Gets the location string.
     *
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location string.
     *
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Gets the cost to attend the event.
     *
     * @return the cost, or null if free
     */
    public Double getCost() {
        return cost;
    }

    /**
     * Sets the cost to attend the event.
     *
     * @param cost the cost to set, or null for free events
     */
    public void setCost(Double cost) {
        this.cost = cost;
    }

    /**
     * Gets the registration start date.
     *
     * @return the registration start date
     */
    public Timestamp getRegistrationStartDate() {
        return registrationStartDate;
    }

    /**
     * Sets the registration start date.
     *
     * @param registrationStartDate the registration start date to set
     */
    public void setRegistrationStartDate(Timestamp registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    /**
     * Gets the registration end date.
     *
     * @return the registration end date
     */
    public Timestamp getRegistrationEndDate() {
        return registrationEndDate;
    }

    /**
     * Sets the registration end date.
     *
     * @param registrationEndDate the registration end date to set
     */
    public void setRegistrationEndDate(Timestamp registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    /**
     * Gets the maximum number of entrants allowed.
     *
     * @return the maximum entrants, or null if unlimited
     */
    public Integer getMaxEntrants() {
        return maxEntrants;
    }

    /**
     * Sets the maximum number of entrants allowed.
     *
     * @param maxEntrants the maximum entrants to set, or null for unlimited
     */
    public void setMaxEntrants(Integer maxEntrants) {
        this.maxEntrants = maxEntrants;
    }

    /**
     * Gets the number of winners to draw/choose.
     *
     * @return the number of winners
     */
    public Integer getNumberOfWinners() {
        return numberOfWinners;
    }

    /**
     * Sets the number of winners to draw/choose.
     *
     * @param numberOfWinners the number of winners to set
     */
    public void setNumberOfWinners(Integer numberOfWinners) {
        this.numberOfWinners = numberOfWinners;
    }

    /**
     * Gets whether geolocation is required for check-in.
     *
     * @return true if geolocation is required, false otherwise
     */
    public Boolean getGeolocationRequired() {
        return geolocationRequired;
    }

    /**
     * Sets whether geolocation is required for check-in.
     *
     * @param geolocationRequired true if geolocation is required, false otherwise
     */
    public void setGeolocationRequired(Boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    public String getQrCodeImageBase64() {
        return qrCodeImageBase64;
    }

    public void setQrCodeImageBase64(String qrCodeImageBase64) {
        this.qrCodeImageBase64 = qrCodeImageBase64;
    }

    public Integer getQrStyleVersion() {
        return qrStyleVersion;
    }

    public void setQrStyleVersion(Integer qrStyleVersion) {
        this.qrStyleVersion = qrStyleVersion;
    }

    /**
     * Gets the event status.
     *
     * @return the status ("DRAFT", "OPEN", "CLOSED", "DRAWN", or "COMPLETED")
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the event status.
     *
     * @param status the status to set ("DRAFT", "OPEN", "CLOSED", "DRAWN", or "COMPLETED")
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
