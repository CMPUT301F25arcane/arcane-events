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
 * Note: Geolocation and notification features are not implemented in the prototype,
 *       so tests focus on actually used fields: userId, deviceId, name, email, role, registeredEventIds
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
     * Test role field with different valid values
     */
    @Test
    public void testRoleField() {
        UserProfile profile = new UserProfile();

        // Test ENTRANT role
        profile.setRole("ENTRANT");
        assertEquals("Role should be ENTRANT", "ENTRANT", profile.getRole());

        // Test ORGANIZER role
        profile.setRole("ORGANIZER");
        assertEquals("Role should be ORGANIZER", "ORGANIZER", profile.getRole());

        // Test ADMIN role
        profile.setRole("ADMIN");
        assertEquals("Role should be ADMIN", "ADMIN", profile.getRole());

        // Test null role
        profile.setRole(null);
        assertNull("Role should be null", profile.getRole());
    }

    /**
     * Test deviceId optional field
     */
    @Test
    public void testDeviceIdOptional() {
        UserProfile profile = new UserProfile(testUserId, null, testName,
                                             testEmail, testRole, null, null);

        assertNull("DeviceId should be null when not provided", profile.getDeviceId());

        // Set deviceId
        profile.setDeviceId(testDeviceId);
        assertEquals("DeviceId should be set", testDeviceId, profile.getDeviceId());

        // Set back to null
        profile.setDeviceId(null);
        assertNull("DeviceId should be null after setting to null", profile.getDeviceId());
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

    /**
     * Test creating multiple UserProfile instances with different data
     */
    @Test
    public void testMultipleUserProfileInstances() {
        UserProfile profile1 = new UserProfile("user1", "device1", "Alice",
                                              "alice@email.com", "ENTRANT", null, null);
        UserProfile profile2 = new UserProfile("user2", "device2", "Bob",
                                              "bob@email.com", "ORGANIZER", null, null);

        // Verify they are different
        assertNotEquals("UserIds should be different", profile1.getUserId(), profile2.getUserId());
        assertNotEquals("Names should be different", profile1.getName(), profile2.getName());
        assertNotEquals("Roles should be different", profile1.getRole(), profile2.getRole());

        // Verify they are independent objects
        profile1.setName("Alice Updated");
        assertEquals("Profile1 name should be updated", "Alice Updated", profile1.getName());
        assertEquals("Profile2 name should remain unchanged", "Bob", profile2.getName());
    }

    /**
     * Test empty registeredEventIds list
     */
    @Test
    public void testEmptyRegisteredEventIds() {
        UserProfile profile = new UserProfile();

        List<String> emptyList = new ArrayList<>();
        profile.setRegisteredEventIds(emptyList);

        assertNotNull("RegisteredEventIds should not be null", profile.getRegisteredEventIds());
        assertTrue("RegisteredEventIds should be empty", profile.getRegisteredEventIds().isEmpty());
        assertEquals("RegisteredEventIds size should be 0", 0, profile.getRegisteredEventIds().size());
    }

    /**
     * Test userId field (primary identifier)
     */
    @Test
    public void testUserIdField() {
        UserProfile profile = new UserProfile();

        assertNull("UserId should be null initially", profile.getUserId());

        profile.setUserId(testUserId);
        assertEquals("UserId should be set", testUserId, profile.getUserId());

        String newUserId = "new-user-456";
        profile.setUserId(newUserId);
        assertEquals("UserId should be updated", newUserId, profile.getUserId());
    }

    /**
     * Test email field
     */
    @Test
    public void testEmailField() {
        UserProfile profile = new UserProfile();

        assertNull("Email should be null initially", profile.getEmail());

        profile.setEmail(testEmail);
        assertEquals("Email should be set", testEmail, profile.getEmail());

        String newEmail = "newemail@example.com";
        profile.setEmail(newEmail);
        assertEquals("Email should be updated", newEmail, profile.getEmail());
    }

    /**
     * Test name field
     */
    @Test
    public void testNameField() {
        UserProfile profile = new UserProfile();

        assertNull("Name should be null initially", profile.getName());

        profile.setName(testName);
        assertEquals("Name should be set", testName, profile.getName());

        String newName = "Updated Name";
        profile.setName(newName);
        assertEquals("Name should be updated", newName, profile.getName());
    }
}