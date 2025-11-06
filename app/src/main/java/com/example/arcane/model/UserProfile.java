package com.example.arcane.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import java.util.List;

public class UserProfile {
    @DocumentId
    private String userId;
    
    private String deviceId;
    private String name;
    private String email;
    private String role;  // "ENTRANT", "ORGANIZER", "ADMIN"
    private GeoPoint geolocation;  // Optional
    private Boolean notificationOptOut;

    // List of event IDs user has registered for (saved to Firestore for workaround)
    private List<String> registeredEventIds;

    // Required no-arg constructor for Firestore
    public UserProfile() {}

    public UserProfile(String userId, String deviceId, String name, String email, 
                      String role, GeoPoint geolocation, Boolean notificationOptOut) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.geolocation = geolocation;
        this.notificationOptOut = notificationOptOut != null ? notificationOptOut : false;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public GeoPoint getGeolocation() {
        return geolocation;
    }

    public void setGeolocation(GeoPoint geolocation) {
        this.geolocation = geolocation;
    }

    public Boolean getNotificationOptOut() {
        return notificationOptOut;
    }

    public void setNotificationOptOut(Boolean notificationOptOut) {
        this.notificationOptOut = notificationOptOut;
    }

    public List<String> getRegisteredEventIds() {
        return registeredEventIds;
    }

    public void setRegisteredEventIds(List<String> registeredEventIds) {
        this.registeredEventIds = registeredEventIds;
    }
}

