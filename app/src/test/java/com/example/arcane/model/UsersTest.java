package com.example.arcane.model;

import com.google.firebase.Timestamp;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for Users model class
 * Tests constructors, getters, setters, and field validations
 */
public class UsersTest {

    private String testId;
    private String testName;
    private String testEmail;
    private String testPhone;
    private String testDeviceId;
    private Timestamp testTimestamp;

    @Before
    public void setUp() {
        // Initialize test data
        testId = "user123";
        testName = "John Doe";
        testEmail = "john.doe@email.com";
        testPhone = "780-555-0001";
        testDeviceId = "device-abc-123";
        testTimestamp = new Timestamp(new Date());
    }

    /**
     * Test the no-arg constructor (required by Firestore)
     */
    @Test
    public void testNoArgConstructor() {
        Users user = new Users();

        assertNotNull("User object should not be null", user);
        assertNull("ID should be null", user.getId());
        assertNull("Name should be null", user.getName());
        assertNull("Email should be null", user.getEmail());
        assertNull("Phone should be null", user.getPhone());
        assertNull("DeviceId should be null", user.getDeviceId());
        assertNull("CreatedAt should be null", user.getCreatedAt());
        assertNull("Role should be null", user.getRole());
        assertNull("RegisteredEventIds should be null", user.getRegisteredEventIds());
    }

    /**
     * Test the full constructor with all required fields
     */
    @Test
    public void testFullConstructor() {
        Users user = new Users(testId, testName, testEmail, testPhone, testDeviceId, testTimestamp);

        assertNotNull("User object should not be null", user);
        assertEquals("ID should match", testId, user.getId());
        assertEquals("Name should match", testName, user.getName());
        assertEquals("Email should match", testEmail, user.getEmail());
        assertEquals("Phone should match", testPhone, user.getPhone());
        assertEquals("DeviceId should match", testDeviceId, user.getDeviceId());
        assertEquals("Timestamp should match", testTimestamp, user.getCreatedAt());
        assertNull("Role should default to null", user.getRole());
        assertNotNull("RegisteredEventIds should be initialized", user.getRegisteredEventIds());
        assertTrue("RegisteredEventIds should be empty ArrayList", user.getRegisteredEventIds().isEmpty());
    }

    /**
     * Test constructor with null optional fields (phone and deviceId)
     */
    @Test
    public void testConstructorWithNullOptionalFields() {
        Users user = new Users(testId, testName, testEmail, null, null, testTimestamp);

        assertNotNull("User object should not be null", user);
        assertEquals("ID should match", testId, user.getId());
        assertEquals("Name should match", testName, user.getName());
        assertEquals("Email should match", testEmail, user.getEmail());
        assertNull("Phone should be null", user.getPhone());
        assertNull("DeviceId should be null", user.getDeviceId());
        assertEquals("Timestamp should match", testTimestamp, user.getCreatedAt());
    }

    /**
     * Test all getter and setter methods
     */
    @Test
    public void testGettersAndSetters() {
        Users user = new Users();

        // Test ID
        user.setId(testId);
        assertEquals("ID getter should return set value", testId, user.getId());

        // Test Name
        user.setName(testName);
        assertEquals("Name getter should return set value", testName, user.getName());

        // Test Email
        user.setEmail(testEmail);
        assertEquals("Email getter should return set value", testEmail, user.getEmail());

        // Test Phone
        user.setPhone(testPhone);
        assertEquals("Phone getter should return set value", testPhone, user.getPhone());

        // Test DeviceId
        user.setDeviceId(testDeviceId);
        assertEquals("DeviceId getter should return set value", testDeviceId, user.getDeviceId());

        // Test CreatedAt
        user.setCreatedAt(testTimestamp);
        assertEquals("CreatedAt getter should return set value", testTimestamp, user.getCreatedAt());

        // Test Role
        String role = "USER";
        user.setRole(role);
        assertEquals("Role getter should return set value", role, user.getRole());

        // Test RegisteredEventIds
        List<String> eventIds = Arrays.asList("event1", "event2", "event3");
        user.setRegisteredEventIds(eventIds);
        assertEquals("RegisteredEventIds getter should return set value", eventIds, user.getRegisteredEventIds());
    }

    /**
     * Test that deviceId can be null (optional field)
     */
    @Test
    public void testDeviceIdOptional() {
        Users user = new Users(testId, testName, testEmail, testPhone, null, testTimestamp);

        assertNull("DeviceId should be null when not provided", user.getDeviceId());

        // Test setting deviceId to null
        user.setDeviceId(testDeviceId);
        assertEquals("DeviceId should be set", testDeviceId, user.getDeviceId());

        user.setDeviceId(null);
        assertNull("DeviceId should be null after setting to null", user.getDeviceId());
    }

    /**
     * Test that phone can be null (optional field)
     */
    @Test
    public void testPhoneOptional() {
        Users user = new Users(testId, testName, testEmail, null, testDeviceId, testTimestamp);

        assertNull("Phone should be null when not provided", user.getPhone());

        // Test setting phone to null
        user.setPhone(testPhone);
        assertEquals("Phone should be set", testPhone, user.getPhone());

        user.setPhone(null);
        assertNull("Phone should be null after setting to null", user.getPhone());
    }

    /**
     * Test Timestamp creation and setting
     */
    @Test
    public void testTimestampCreation() {
        Date now = new Date();
        Timestamp timestamp = new Timestamp(now);

        Users user = new Users();
        user.setCreatedAt(timestamp);

        assertNotNull("Timestamp should not be null", user.getCreatedAt());
        assertEquals("Timestamp should match the set value", timestamp, user.getCreatedAt());
        assertEquals("Timestamp date should match", now, user.getCreatedAt().toDate());
    }

    /**
     * Test registeredEventIds list operations
     */
    @Test
    public void testRegisteredEventIdsListOperations() {
        Users user = new Users(testId, testName, testEmail, testPhone, testDeviceId, testTimestamp);

        // Verify initialized as empty ArrayList
        assertNotNull("RegisteredEventIds should be initialized", user.getRegisteredEventIds());
        assertTrue("RegisteredEventIds should be empty", user.getRegisteredEventIds().isEmpty());
        assertEquals("RegisteredEventIds size should be 0", 0, user.getRegisteredEventIds().size());

        // Add events
        user.getRegisteredEventIds().add("event1");
        user.getRegisteredEventIds().add("event2");
        user.getRegisteredEventIds().add("event3");

        assertEquals("RegisteredEventIds should have 3 items", 3, user.getRegisteredEventIds().size());
        assertTrue("RegisteredEventIds should contain event1", user.getRegisteredEventIds().contains("event1"));
        assertTrue("RegisteredEventIds should contain event2", user.getRegisteredEventIds().contains("event2"));
        assertTrue("RegisteredEventIds should contain event3", user.getRegisteredEventIds().contains("event3"));

        // Remove event
        user.getRegisteredEventIds().remove("event2");
        assertEquals("RegisteredEventIds should have 2 items after removal", 2, user.getRegisteredEventIds().size());
        assertFalse("RegisteredEventIds should not contain event2", user.getRegisteredEventIds().contains("event2"));
    }

    /**
     * Test setting registeredEventIds to a new list
     */
    @Test
    public void testSetRegisteredEventIds() {
        Users user = new Users();

        List<String> eventIds = new ArrayList<>();
        eventIds.add("event123");
        eventIds.add("event456");

        user.setRegisteredEventIds(eventIds);

        assertNotNull("RegisteredEventIds should not be null", user.getRegisteredEventIds());
        assertEquals("RegisteredEventIds size should be 2", 2, user.getRegisteredEventIds().size());
        assertEquals("RegisteredEventIds should match the set list", eventIds, user.getRegisteredEventIds());
    }

    /**
     * Test role field
     */
    @Test
    public void testRoleField() {
        Users user = new Users(testId, testName, testEmail, testPhone, testDeviceId, testTimestamp);

        // Role should default to null
        assertNull("Role should default to null", user.getRole());

        // Test setting role to USER
        user.setRole("USER");
        assertEquals("Role should be USER", "USER", user.getRole());

        // Test setting role to ORGANISER
        user.setRole("ORGANISER");
        assertEquals("Role should be ORGANISER", "ORGANISER", user.getRole());

        // Test setting role to null
        user.setRole(null);
        assertNull("Role should be null", user.getRole());
    }

    /**
     * Test that all fields can be updated after construction
     */
    @Test
    public void testFieldMutability() {
        Users user = new Users(testId, testName, testEmail, testPhone, testDeviceId, testTimestamp);

        // Update all fields
        String newId = "user456";
        String newName = "Jane Smith";
        String newEmail = "jane.smith@email.com";
        String newPhone = "780-555-0002";
        String newDeviceId = "device-xyz-789";
        Timestamp newTimestamp = new Timestamp(new Date(System.currentTimeMillis() + 1000));
        String newRole = "ORGANISER";
        List<String> newEventIds = Arrays.asList("event10", "event20");

        user.setId(newId);
        user.setName(newName);
        user.setEmail(newEmail);
        user.setPhone(newPhone);
        user.setDeviceId(newDeviceId);
        user.setCreatedAt(newTimestamp);
        user.setRole(newRole);
        user.setRegisteredEventIds(newEventIds);

        // Verify all updates
        assertEquals("ID should be updated", newId, user.getId());
        assertEquals("Name should be updated", newName, user.getName());
        assertEquals("Email should be updated", newEmail, user.getEmail());
        assertEquals("Phone should be updated", newPhone, user.getPhone());
        assertEquals("DeviceId should be updated", newDeviceId, user.getDeviceId());
        assertEquals("CreatedAt should be updated", newTimestamp, user.getCreatedAt());
        assertEquals("Role should be updated", newRole, user.getRole());
        assertEquals("RegisteredEventIds should be updated", newEventIds, user.getRegisteredEventIds());
    }

    /**
     * Test creating multiple users with different data
     */
    @Test
    public void testMultipleUserInstances() {
        Users user1 = new Users("user1", "Alice", "alice@email.com", "111-1111", "device1", testTimestamp);
        Users user2 = new Users("user2", "Bob", "bob@email.com", "222-2222", "device2", testTimestamp);

        assertNotEquals("User IDs should be different", user1.getId(), user2.getId());
        assertNotEquals("User names should be different", user1.getName(), user2.getName());
        assertNotEquals("User emails should be different", user1.getEmail(), user2.getEmail());

        // Verify they are independent objects
        user1.setRole("USER");
        user2.setRole("ORGANISER");

        assertEquals("User1 role should be USER", "USER", user1.getRole());
        assertEquals("User2 role should be ORGANISER", "ORGANISER", user2.getRole());
    }
}