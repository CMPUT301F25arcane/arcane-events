package com.example.arcane.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;

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

    // Required no-arg constructor for Firestore
    public Event() {}

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

    // OOP Methods
    public void addToWaitingList(WaitingListEntry entry) {
        if (waitingList == null) {
            waitingList = new java.util.ArrayList<>();
        }
        waitingList.add(entry);
    }

    public List<WaitingListEntry> getWaitingList() {
        return waitingList;
    }

    public void setWaitingList(List<WaitingListEntry> waitingList) {
        this.waitingList = waitingList;
    }

    public List<Decision> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<Decision> decisions) {
        this.decisions = decisions;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GeoPoint getGeolocation() {
        return geolocation;
    }

    public void setGeolocation(GeoPoint geolocation) {
        this.geolocation = geolocation;
    }

    public String getPosterImageUrl() {
        return posterImageUrl;
    }

    public void setPosterImageUrl(String posterImageUrl) {
        this.posterImageUrl = posterImageUrl;
    }

    public Timestamp getEventDate() {
        return eventDate;
    }

    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public Timestamp getRegistrationStartDate() {
        return registrationStartDate;
    }

    public void setRegistrationStartDate(Timestamp registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    public Timestamp getRegistrationEndDate() {
        return registrationEndDate;
    }

    public void setRegistrationEndDate(Timestamp registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    public Integer getMaxEntrants() {
        return maxEntrants;
    }

    public void setMaxEntrants(Integer maxEntrants) {
        this.maxEntrants = maxEntrants;
    }

    public Integer getNumberOfWinners() {
        return numberOfWinners;
    }

    public void setNumberOfWinners(Integer numberOfWinners) {
        this.numberOfWinners = numberOfWinners;
    }

    public Boolean getGeolocationRequired() {
        return geolocationRequired;
    }

    public void setGeolocationRequired(Boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
