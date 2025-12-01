package com.example.arcane.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

/**
 * Unit tests for the WaitingListEntry class.
 *
 * Tests all constructors, getters, and setters for the WaitingListEntry model.
 *
 * @version 1.0
 */
public class WaitingListEntryTest {

    private Timestamp testJoinTimestamp;
    private Timestamp testInvitedAt;
    private String testEntryId;
    private String testEntrantId;

    @Before
    public void setUp() {
        // Create test timestamps
        testJoinTimestamp = Timestamp.now();
        testInvitedAt = new Timestamp(testJoinTimestamp.getSeconds() + 3600, 0); // 1 hour later
        testEntryId = "entry123";
        testEntrantId = "user456";
    }

    // ========== Constructor Tests ==========

    /**
     * Test the no-arg constructor creates an empty WaitingListEntry.
     */
    @Test
    public void testNoArgConstructor() {
        WaitingListEntry entry = new WaitingListEntry();

        assertNotNull("Entry should not be null", entry);
        assertNull("Entry ID should be null initially", entry.getEntryId());
        assertNull("Entrant ID should be null initially", entry.getEntrantId());
        assertNull("Join timestamp should be null initially", entry.getJoinTimestamp());
        assertNull("Invited at should be null initially", entry.getInvitedAt());
    }

    /**
     * Test the parameterized constructor sets all fields correctly.
     */
    @Test
    public void testParameterizedConstructor() {
        GeoPoint testLocation = new GeoPoint(37.7749, -122.4194);
        WaitingListEntry entry = new WaitingListEntry(
                testEntryId,
                testEntrantId,
                testJoinTimestamp,
                testInvitedAt,
                testLocation
        );

        assertEquals("Entry ID should match", testEntryId, entry.getEntryId());
        assertEquals("Entrant ID should match", testEntrantId, entry.getEntrantId());
        assertEquals("Join timestamp should match", testJoinTimestamp, entry.getJoinTimestamp());
        assertEquals("Invited at should match", testInvitedAt, entry.getInvitedAt());
        assertEquals("Join location should match", testLocation, entry.getJoinLocation());
    }

    /**
     * Test the parameterized constructor with null invitedAt (optional field).
     */
    @Test
    public void testParameterizedConstructorWithNullInvitedAt() {
        GeoPoint testLocation = new GeoPoint(37.7749, -122.4194);
        WaitingListEntry entry = new WaitingListEntry(
                testEntryId,
                testEntrantId,
                testJoinTimestamp,
                null,
                testLocation
        );

        assertEquals("Entry ID should match", testEntryId, entry.getEntryId());
        assertEquals("Entrant ID should match", testEntrantId, entry.getEntrantId());
        assertEquals("Join timestamp should match", testJoinTimestamp, entry.getJoinTimestamp());
        assertNull("Invited at should be null", entry.getInvitedAt());
        assertEquals("Join location should match", testLocation, entry.getJoinLocation());
    }

    /**
     * Test the parameterized constructor with null entryId (may be null before creation).
     */
    @Test
    public void testParameterizedConstructorWithNullEntryId() {
        GeoPoint testLocation = new GeoPoint(37.7749, -122.4194);
        WaitingListEntry entry = new WaitingListEntry(
                null,
                testEntrantId,
                testJoinTimestamp,
                testInvitedAt,
                testLocation
        );

        assertNull("Entry ID should be null", entry.getEntryId());
        assertEquals("Entrant ID should match", testEntrantId, entry.getEntrantId());
        assertEquals("Join timestamp should match", testJoinTimestamp, entry.getJoinTimestamp());
        assertEquals("Invited at should match", testInvitedAt, entry.getInvitedAt());
        assertEquals("Join location should match", testLocation, entry.getJoinLocation());
    }

    // ========== Getter and Setter Tests for entryId ==========

    /**
     * Test getEntryId returns the correct value.
     */
    @Test
    public void testGetEntryId() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEntryId(testEntryId);

        assertEquals("Entry ID should match", testEntryId, entry.getEntryId());
    }

    /**
     * Test setEntryId sets the value correctly.
     */
    @Test
    public void testSetEntryId() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEntryId(testEntryId);

        assertEquals("Entry ID should be set", testEntryId, entry.getEntryId());
    }

    /**
     * Test setEntryId can set null value.
     */
    @Test
    public void testSetEntryIdNull() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEntryId(testEntryId);
        entry.setEntryId(null);

        assertNull("Entry ID should be null", entry.getEntryId());
    }

    /**
     * Test setEntryId can update an existing value.
     */
    @Test
    public void testSetEntryIdUpdate() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEntryId("oldId");
        entry.setEntryId(testEntryId);

        assertEquals("Entry ID should be updated", testEntryId, entry.getEntryId());
    }

    // ========== Getter and Setter Tests for entrantId ==========

    /**
     * Test getEntrantId returns the correct value.
     */
    @Test
    public void testGetEntrantId() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEntrantId(testEntrantId);

        assertEquals("Entrant ID should match", testEntrantId, entry.getEntrantId());
    }

    /**
     * Test setEntrantId sets the value correctly.
     */
    @Test
    public void testSetEntrantId() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEntrantId(testEntrantId);

        assertEquals("Entrant ID should be set", testEntrantId, entry.getEntrantId());
    }

    /**
     * Test setEntrantId can set null value.
     */
    @Test
    public void testSetEntrantIdNull() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEntrantId(testEntrantId);
        entry.setEntrantId(null);

        assertNull("Entrant ID should be null", entry.getEntrantId());
    }

    /**
     * Test setEntrantId can update an existing value.
     */
    @Test
    public void testSetEntrantIdUpdate() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEntrantId("oldEntrantId");
        entry.setEntrantId(testEntrantId);

        assertEquals("Entrant ID should be updated", testEntrantId, entry.getEntrantId());
    }

    // ========== Getter and Setter Tests for joinTimestamp ==========

    /**
     * Test getJoinTimestamp returns the correct value.
     */
    @Test
    public void testGetJoinTimestamp() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setJoinTimestamp(testJoinTimestamp);

        assertEquals("Join timestamp should match", testJoinTimestamp, entry.getJoinTimestamp());
    }

    /**
     * Test setJoinTimestamp sets the value correctly.
     */
    @Test
    public void testSetJoinTimestamp() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setJoinTimestamp(testJoinTimestamp);

        assertEquals("Join timestamp should be set", testJoinTimestamp, entry.getJoinTimestamp());
    }

    /**
     * Test setJoinTimestamp can set null value.
     */
    @Test
    public void testSetJoinTimestampNull() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setJoinTimestamp(testJoinTimestamp);
        entry.setJoinTimestamp(null);

        assertNull("Join timestamp should be null", entry.getJoinTimestamp());
    }

    /**
     * Test setJoinTimestamp can update an existing value.
     */
    @Test
    public void testSetJoinTimestampUpdate() {
        WaitingListEntry entry = new WaitingListEntry();
        Timestamp oldTimestamp = new Timestamp(1000, 0);
        entry.setJoinTimestamp(oldTimestamp);
        entry.setJoinTimestamp(testJoinTimestamp);

        assertEquals("Join timestamp should be updated", testJoinTimestamp, entry.getJoinTimestamp());
    }

    // ========== Getter and Setter Tests for invitedAt ==========

    /**
     * Test getInvitedAt returns the correct value.
     */
    @Test
    public void testGetInvitedAt() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setInvitedAt(testInvitedAt);

        assertEquals("Invited at should match", testInvitedAt, entry.getInvitedAt());
    }

    /**
     * Test setInvitedAt sets the value correctly.
     */
    @Test
    public void testSetInvitedAt() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setInvitedAt(testInvitedAt);

        assertEquals("Invited at should be set", testInvitedAt, entry.getInvitedAt());
    }

    /**
     * Test setInvitedAt can set null value (optional field).
     */
    @Test
    public void testSetInvitedAtNull() {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setInvitedAt(testInvitedAt);
        entry.setInvitedAt(null);

        assertNull("Invited at should be null", entry.getInvitedAt());
    }

    /**
     * Test setInvitedAt can update an existing value.
     */
    @Test
    public void testSetInvitedAtUpdate() {
        WaitingListEntry entry = new WaitingListEntry();
        Timestamp oldInvitedAt = new Timestamp(2000, 0);
        entry.setInvitedAt(oldInvitedAt);
        entry.setInvitedAt(testInvitedAt);

        assertEquals("Invited at should be updated", testInvitedAt, entry.getInvitedAt());
    }

    // ========== Integration Tests ==========

    /**
     * Test that all fields can be set and retrieved independently.
     */
    @Test
    public void testAllFieldsIndependent() {
        WaitingListEntry entry = new WaitingListEntry();

        // Set all fields
        entry.setEntryId(testEntryId);
        entry.setEntrantId(testEntrantId);
        entry.setJoinTimestamp(testJoinTimestamp);
        entry.setInvitedAt(testInvitedAt);

        // Verify all fields
        assertEquals("Entry ID should be set", testEntryId, entry.getEntryId());
        assertEquals("Entrant ID should be set", testEntrantId, entry.getEntrantId());
        assertEquals("Join timestamp should be set", testJoinTimestamp, entry.getJoinTimestamp());
        assertEquals("Invited at should be set", testInvitedAt, entry.getInvitedAt());
    }

    /**
     * Test that a fully constructed entry matches a manually set entry.
     */
    @Test
    public void testConstructorVsSetters() {
        GeoPoint testLocation = new GeoPoint(37.7749, -122.4194);
        // Create entry using constructor
        WaitingListEntry constructorEntry = new WaitingListEntry(
                testEntryId,
                testEntrantId,
                testJoinTimestamp,
                testInvitedAt,
                testLocation
        );

        // Create entry using setters
        WaitingListEntry setterEntry = new WaitingListEntry();
        setterEntry.setEntryId(testEntryId);
        setterEntry.setEntrantId(testEntrantId);
        setterEntry.setJoinTimestamp(testJoinTimestamp);
        setterEntry.setInvitedAt(testInvitedAt);
        setterEntry.setJoinLocation(testLocation);

        // Both should have the same values
        assertEquals("Entry IDs should match",
                constructorEntry.getEntryId(), setterEntry.getEntryId());
        assertEquals("Entrant IDs should match",
                constructorEntry.getEntrantId(), setterEntry.getEntrantId());
        assertEquals("Join timestamps should match",
                constructorEntry.getJoinTimestamp(), setterEntry.getJoinTimestamp());
        assertEquals("Invited at timestamps should match",
                constructorEntry.getInvitedAt(), setterEntry.getInvitedAt());
        assertEquals("Join locations should match",
                constructorEntry.getJoinLocation(), setterEntry.getJoinLocation());
    }
}