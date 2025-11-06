package com.example.arcane.model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Users {

    private String id;         // use auth.uid as doc id
    private String name;
    private String email;
    private String phone;      // optional
    private String deviceId;   // optional
    private Timestamp createdAt;
    private String role;       // e.g., "USER" or "ORGANISER"
    private List<String> registeredEventIds; // workaround array for My Events

    // Required public no-arg constructor (Firestore uses this)
    public Users() {}

    public Users(String id, String name, String email, String phone, String deviceId, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.deviceId = deviceId;
        this.createdAt = createdAt;
        this.role = null;
        this.registeredEventIds = new ArrayList<>();
    }

    // Getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<String> getRegisteredEventIds() { return registeredEventIds; }
    public void setRegisteredEventIds(List<String> registeredEventIds) { this.registeredEventIds = registeredEventIds; }
}

