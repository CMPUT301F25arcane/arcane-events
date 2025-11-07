package com.example.arcane.model;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for UserProfile model class
 * Tests constructors, getters, setters, and field validations
 */
public class UserProfileTest {

    private String testUserId;
    private String testDeviceId;
    private String testName;
    private String testEmail;
    private String testRole;

    @Before
    public void setUp() {
        // Initialize test data
        testUserId = "user123";
        testDeviceId = "device-abc-123";
        testName = "John Doe";
        testEmail = "john.doe@email.com";
        testRole = "ENTRANT";
    }

    /**
     * Test the no-arg constructor (required by Firestore)
     */
    @Test
    public void testNoArgConstructor() {
        UserProfile profile = new UserProfile();

        assertNotNull("UserProfile object should not be null", profile);
        assertNull("UserId should be null", profile.getUserId());
        assertNull("DeviceId should be null", profile.getDeviceId());
        assertNull("Name should be null", profile.getName());
        assertNull("Email should be null", profile.getEmail());
        assertNull("Role should be null", profile.getRole());
        assertNull("RegisteredEventIds should be null", profile.getRegisteredEventIds());
    }

    /**
     * Test the constructor with core fields (null for geolocation/notification)
     */
    @Test
    public void testConstructorWithCoreFields() {
        UserProfile profile = new UserProfile(testUserId, testDeviceId, testName,
                                             testEmail, testRole, null, null);

        assertNotNull("UserProfile object should not be null", profile);
        assertEquals("UserId should match", testUserId, profile.getUserId());
        assertEquals("DeviceId should match", testDeviceId, profile.getDeviceId());
        assertEquals("Name should match", testName, profile.getName());
        assertEquals("Email should match", testEmail, profile.getEmail());
        assertEquals("Role should match", testRole, profile.getRole());
    }

    /**
     * Test constructor with null optional fields
     */
    @Test
    public void testConstructorWithNullOptionalFields() {
        UserProfile profile = new UserProfile(testUserId, null, testName,
                                             testEmail, testRole, null, null);

        assertNotNull("UserProfile object should not be null", profile);
        assertEquals("UserId should match", testUserId, profile.getUserId());
        assertNull("DeviceId should be null", profile.getDeviceId());
        assertEquals("Name should match", testName, profile.getName());
        assertEquals("Email should match", testEmail, profile.getEmail());
        assertEquals("Role should match", testRole, profile.getRole());
    }

    /**
     * Test all getter and setter methods for implemented fields
     */
    @Test
    public void testGettersAndSetters() {
        UserProfile profile = new UserProfile();

        // Test UserId
        profile.setUserId(testUserId);
        assertEquals("UserId getter should return set value", testUserId, profile.getUserId());

        // Test DeviceId
        profile.setDeviceId(testDeviceId);
        assertEquals("DeviceId getter should return set value", testDeviceId, profile.getDeviceId());

        // Test Name
        profile.setName(testName);
        assertEquals("Name getter should return set value", testName, profile.getName());

        // Test Email
        profile.setEmail(testEmail);
        assertEquals("Email getter should return set value", testEmail, profile.getEmail());

        // Test Role
        profile.setRole(testRole);
        assertEquals("Role getter should return set value", testRole, profile.getRole());

        // Test RegisteredEventIds
        List<String> eventIds = Arrays.asList("event1", "event2");
        profile.setRegisteredEventIds(eventIds);
        assertEquals("RegisteredEventIds getter should return set value", eventIds, profile.getRegisteredEventIds());
    }

    /**
     * Test registeredEventIds list operations
     */
    @Test
    public void testRegisteredEventIdsList() {
        UserProfile profile = new UserProfile();

        // Initially null
        assertNull("RegisteredEventIds should be null initially", profile.getRegisteredEventIds());

        // Set a list
        List<String> eventIds = new ArrayList<>();
        eventIds.add("event1");
        eventIds.add("event2");
        eventIds.add("event3");

        profile.setRegisteredEventIds(eventIds);

        assertNotNull("RegisteredEventIds should not be null", profile.getRegisteredEventIds());
        assertEquals("RegisteredEventIds should have 3 items", 3, profile.getRegisteredEventIds().size());
        assertTrue("RegisteredEventIds should contain event1", profile.getRegisteredEventIds().contains("event1"));
        assertTrue("RegisteredEventIds should contain event2", profile.getRegisteredEventIds().contains("event2"));
        assertTrue("RegisteredEventIds should contain event3", profile.getRegisteredEventIds().contains("event3"));

        // Modify the list
        profile.getRegisteredEventIds().remove("event2");
        assertEquals("RegisteredEventIds should have 2 items after removal", 2, profile.getRegisteredEventIds().size());
        assertFalse("RegisteredEventIds should not contain event2", profile.getRegisteredEventIds().contains("event2"));

        // Add to the list
        profile.getRegisteredEventIds().add("event4");
        assertEquals("RegisteredEventIds should have 3 items after addition", 3, profile.getRegisteredEventIds().size());
        assertTrue("RegisteredEventIds should contain event4", profile.getRegisteredEventIds().contains("event4"));
    }



    /**
     * Test that all core fields can be updated after construction
     */
    @Test
    public void testFieldMutability() {
        UserProfile profile = new UserProfile(testUserId, testDeviceId, testName,
                                             testEmail, testRole, null, null);

        // Update all core fields
        String newUserId = "user456";
        String newDeviceId = "device-xyz-789";
        String newName = "Jane Smith";
        String newEmail = "jane.smith@email.com";
        String newRole = "ORGANIZER";
        List<String> newEventIds = Arrays.asList("event10", "event20");

        profile.setUserId(newUserId);
        profile.setDeviceId(newDeviceId);
        profile.setName(newName);
        profile.setEmail(newEmail);
        profile.setRole(newRole);
        profile.setRegisteredEventIds(newEventIds);

        // Verify all updates
        assertEquals("UserId should be updated", newUserId, profile.getUserId());
        assertEquals("DeviceId should be updated", newDeviceId, profile.getDeviceId());
        assertEquals("Name should be updated", newName, profile.getName());
        assertEquals("Email should be updated", newEmail, profile.getEmail());
        assertEquals("Role should be updated", newRole, profile.getRole());
        assertEquals("RegisteredEventIds should be updated", newEventIds, profile.getRegisteredEventIds());
    }





}