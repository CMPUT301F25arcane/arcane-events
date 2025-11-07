package com.example.arcane.model;

import com.google.firebase.Timestamp;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for WaitingListEntry model class
 */
public class WaitingListEntryTest {

    private String testEntryId;
    private String testEntrantId;
    private Timestamp testJoinTimestamp;
    private Timestamp testInvitedAt;

    @Before
    public void setUp() {
        // Initialize test data
        testEntryId = "entry123";
        testEntrantId = "entrant456";
        testJoinTimestamp = new Timestamp(new Date());
        testInvitedAt = Timestamp.now();
    }

    /**
     * Test the no-arg constructor (required by Firestore)
     */
    @Test
    public void testNoArgConstructor() {
        WaitingListEntry entry = new WaitingListEntry();

        assertNotNull("WaitingListEntry object should not be null", entry);
        assertNull("EntryId should be null", entry.getEntryId());
        assertNull("EntrantId should be null", entry.getEntrantId());
        assertNull("JoinTimestamp should be null", entry.getJoinTimestamp());
        assertNull("InvitedAt should be null", entry.getInvitedAt());
    }

    /**
     * Test the full constructor with all fields
     */
    @Test
    public void testFullConstructor() {
        WaitingListEntry entry = new WaitingListEntry(testEntryId, testEntrantId,
                                                      testJoinTimestamp, testInvitedAt);

        assertNotNull("WaitingListEntry object should not be null", entry);
        assertEquals("EntryId should match", testEntryId, entry.getEntryId());
        assertEquals("EntrantId should match", testEntrantId, entry.getEntrantId());
        assertEquals("JoinTimestamp should match", testJoinTimestamp, entry.getJoinTimestamp());
        assertEquals("InvitedAt should match", testInvitedAt, entry.getInvitedAt());
    }

    /**
     * Test constructor with null invitedAt (optional field)
     */
    @Test
    public void testConstructorWithNullInvitedAt() {
        WaitingListEntry entry = new WaitingListEntry(testEntryId, testEntrantId,
                                                      testJoinTimestamp, null);

        assertNotNull("WaitingListEntry object should not be null", entry);
        assertEquals("EntryId should match", testEntryId, entry.getEntryId());
        assertEquals("EntrantId should match", testEntrantId, entry.getEntrantId());
        assertEquals("JoinTimestamp should match", testJoinTimestamp, entry.getJoinTimestamp());
        assertNull("InvitedAt should be null", entry.getInvitedAt());
    }

    /**
     * Test all getter and setter methods
     */
    @Test
    public void testGettersAndSetters() {
        WaitingListEntry entry = new WaitingListEntry();

        // Test EntryId
        entry.setEntryId(testEntryId);
        assertEquals("EntryId getter should return set value", testEntryId, entry.getEntryId());

        // Test EntrantId
        entry.setEntrantId(testEntrantId);
        assertEquals("EntrantId getter should return set value", testEntrantId, entry.getEntrantId());

        // Test JoinTimestamp
        entry.setJoinTimestamp(testJoinTimestamp);
        assertEquals("JoinTimestamp getter should return set value", testJoinTimestamp, entry.getJoinTimestamp());

        // Test InvitedAt
        entry.setInvitedAt(testInvitedAt);
        assertEquals("InvitedAt getter should return set value", testInvitedAt, entry.getInvitedAt());
    }


    /**
     * Test that all fields can be updated after construction
     */
    @Test
    public void testFieldMutability() {
        WaitingListEntry entry = new WaitingListEntry(testEntryId, testEntrantId,
                                                      testJoinTimestamp, testInvitedAt);

        // Update all fields
        String newEntryId = "entry999";
        String newEntrantId = "entrant888";
        Timestamp newJoinTimestamp = new Timestamp(new Date(System.currentTimeMillis() + 1000));
        Timestamp newInvitedAt = Timestamp.now();

        entry.setEntryId(newEntryId);
        entry.setEntrantId(newEntrantId);
        entry.setJoinTimestamp(newJoinTimestamp);
        entry.setInvitedAt(newInvitedAt);

        // Verify all updates
        assertEquals("EntryId should be updated", newEntryId, entry.getEntryId());
        assertEquals("EntrantId should be updated", newEntrantId, entry.getEntrantId());
        assertEquals("JoinTimestamp should be updated", newJoinTimestamp, entry.getJoinTimestamp());
        assertEquals("InvitedAt should be updated", newInvitedAt, entry.getInvitedAt());
    }
}