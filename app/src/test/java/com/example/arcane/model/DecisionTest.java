package com.example.arcane.model;

import com.google.firebase.Timestamp;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for Decision model class
 * Tests constructors, getters, setters, status values, and timestamp handling
 */
public class DecisionTest {

    private String testDecisionId;
    private String testEntrantId;
    private String testEntryId;
    private String testStatus;
    private Timestamp testRespondedAt;
    private Timestamp testUpdatedAt;

    @Before
    public void setUp() {
        // Initialize test data
        testDecisionId = "decision123";
        testEntrantId = "entrant456";
        testEntryId = "entry789";
        testStatus = "PENDING";
        testRespondedAt = new Timestamp(new Date());
        testUpdatedAt = Timestamp.now();
    }

    /**
     * Test the no-arg constructor (required by Firestore)
     */
    @Test
    public void testNoArgConstructor() {
        Decision decision = new Decision();

        assertNotNull("Decision object should not be null", decision);
        assertNull("DecisionId should be null", decision.getDecisionId());
        assertNull("EntrantId should be null", decision.getEntrantId());
        assertNull("EntryId should be null", decision.getEntryId());
        assertNull("Status should be null", decision.getStatus());
        assertNull("RespondedAt should be null", decision.getRespondedAt());
        assertNull("UpdatedAt should be null", decision.getUpdatedAt());
    }

    /**
     * Test the full constructor with all fields
     */
    @Test
    public void testFullConstructor() {
        Decision decision = new Decision(testDecisionId, testEntrantId, testEntryId,
                                        testStatus, testRespondedAt, testUpdatedAt);

        assertNotNull("Decision object should not be null", decision);
        assertEquals("DecisionId should match", testDecisionId, decision.getDecisionId());
        assertEquals("EntrantId should match", testEntrantId, decision.getEntrantId());
        assertEquals("EntryId should match", testEntryId, decision.getEntryId());
        assertEquals("Status should match", testStatus, decision.getStatus());
        assertEquals("RespondedAt should match", testRespondedAt, decision.getRespondedAt());
        assertEquals("UpdatedAt should match", testUpdatedAt, decision.getUpdatedAt());
    }

    /**
     * Test constructor with null respondedAt (optional field)
     */
    @Test
    public void testConstructorWithNullRespondedAt() {
        Decision decision = new Decision(testDecisionId, testEntrantId, testEntryId,
                                        testStatus, null, testUpdatedAt);

        assertNotNull("Decision object should not be null", decision);
        assertEquals("DecisionId should match", testDecisionId, decision.getDecisionId());
        assertNull("RespondedAt should be null", decision.getRespondedAt());
        assertEquals("UpdatedAt should match", testUpdatedAt, decision.getUpdatedAt());
    }

    /**
     * Test all getter and setter methods
     */
    @Test
    public void testGettersAndSetters() {
        Decision decision = new Decision();

        // Test DecisionId
        decision.setDecisionId(testDecisionId);
        assertEquals("DecisionId getter should return set value", testDecisionId, decision.getDecisionId());

        // Test EntrantId
        decision.setEntrantId(testEntrantId);
        assertEquals("EntrantId getter should return set value", testEntrantId, decision.getEntrantId());

        // Test EntryId
        decision.setEntryId(testEntryId);
        assertEquals("EntryId getter should return set value", testEntryId, decision.getEntryId());

        // Test Status
        decision.setStatus(testStatus);
        assertEquals("Status getter should return set value", testStatus, decision.getStatus());

        // Test RespondedAt
        decision.setRespondedAt(testRespondedAt);
        assertEquals("RespondedAt getter should return set value", testRespondedAt, decision.getRespondedAt());

        // Test UpdatedAt
        decision.setUpdatedAt(testUpdatedAt);
        assertEquals("UpdatedAt getter should return set value", testUpdatedAt, decision.getUpdatedAt());
    }

    /**
     * Test status values for lottery system
     */
    @Test
    public void testStatusValues() {
        Decision decision = new Decision();

        // Test PENDING status
        decision.setStatus("PENDING");
        assertEquals("Status should be PENDING", "PENDING", decision.getStatus());

        // Test INVITED status
        decision.setStatus("INVITED");
        assertEquals("Status should be INVITED", "INVITED", decision.getStatus());

        // Test ACCEPTED status
        decision.setStatus("ACCEPTED");
        assertEquals("Status should be ACCEPTED", "ACCEPTED", decision.getStatus());

        // Test DECLINED status
        decision.setStatus("DECLINED");
        assertEquals("Status should be DECLINED", "DECLINED", decision.getStatus());

        // Test CANCELLED status
        decision.setStatus("CANCELLED");
        assertEquals("Status should be CANCELLED", "CANCELLED", decision.getStatus());

        // Test LOST status
        decision.setStatus("LOST");
        assertEquals("Status should be LOST", "LOST", decision.getStatus());

        // Test null status
        decision.setStatus(null);
        assertNull("Status should be null", decision.getStatus());
    }

    /**
     * Test respondedAt timestamp (optional field)
     */
    @Test
    public void testRespondedAtOptional() {
        Decision decision = new Decision();

        // Initially null
        assertNull("RespondedAt should be null initially", decision.getRespondedAt());

        // Set timestamp
        Timestamp timestamp = new Timestamp(new Date());
        decision.setRespondedAt(timestamp);
        assertEquals("RespondedAt should be set", timestamp, decision.getRespondedAt());

        // Set back to null
        decision.setRespondedAt(null);
        assertNull("RespondedAt should be null after setting to null", decision.getRespondedAt());
    }

    /**
     * Test updatedAt timestamp
     */
    @Test
    public void testUpdatedAtTimestamp() {
        Decision decision = new Decision();

        assertNull("UpdatedAt should be null initially", decision.getUpdatedAt());

        Timestamp now = Timestamp.now();
        decision.setUpdatedAt(now);

        assertNotNull("UpdatedAt should not be null", decision.getUpdatedAt());
        assertEquals("UpdatedAt should match", now, decision.getUpdatedAt());

        // Update to new timestamp
        Timestamp later = new Timestamp(new Date(System.currentTimeMillis() + 1000));
        decision.setUpdatedAt(later);
        assertEquals("UpdatedAt should be updated", later, decision.getUpdatedAt());
    }

    /**
     * Test timestamps creation and handling
     */
    @Test
    public void testTimestampCreation() {
        Date now = new Date();
        Timestamp respondedAt = new Timestamp(now);
        Timestamp updatedAt = Timestamp.now();

        Decision decision = new Decision(testDecisionId, testEntrantId, testEntryId,
                                        "ACCEPTED", respondedAt, updatedAt);

        assertNotNull("RespondedAt should not be null", decision.getRespondedAt());
        assertNotNull("UpdatedAt should not be null", decision.getUpdatedAt());
        assertEquals("RespondedAt date should match", now, decision.getRespondedAt().toDate());
    }

    /**
     * Test status transition flow (PENDING -> INVITED -> ACCEPTED)
     */
    @Test
    public void testStatusTransitionAccepted() {
        Decision decision = new Decision();

        // Start as PENDING
        decision.setStatus("PENDING");
        decision.setUpdatedAt(Timestamp.now());
        assertNull("RespondedAt should be null for PENDING", decision.getRespondedAt());

        // Transition to INVITED
        decision.setStatus("INVITED");
        decision.setUpdatedAt(Timestamp.now());
        assertNull("RespondedAt should still be null for INVITED", decision.getRespondedAt());

        // Transition to ACCEPTED
        decision.setStatus("ACCEPTED");
        Timestamp acceptedTime = Timestamp.now();
        decision.setRespondedAt(acceptedTime);
        decision.setUpdatedAt(acceptedTime);

        assertEquals("Status should be ACCEPTED", "ACCEPTED", decision.getStatus());
        assertNotNull("RespondedAt should be set when ACCEPTED", decision.getRespondedAt());
        assertEquals("RespondedAt should match acceptance time", acceptedTime, decision.getRespondedAt());
    }

    /**
     * Test status transition flow (PENDING -> INVITED -> DECLINED)
     */
    @Test
    public void testStatusTransitionDeclined() {
        Decision decision = new Decision();

        // Start as PENDING
        decision.setStatus("PENDING");
        decision.setUpdatedAt(Timestamp.now());

        // Transition to INVITED
        decision.setStatus("INVITED");
        decision.setUpdatedAt(Timestamp.now());

        // Transition to DECLINED
        decision.setStatus("DECLINED");
        Timestamp declinedTime = Timestamp.now();
        decision.setRespondedAt(declinedTime);
        decision.setUpdatedAt(declinedTime);

        assertEquals("Status should be DECLINED", "DECLINED", decision.getStatus());
        assertNotNull("RespondedAt should be set when DECLINED", decision.getRespondedAt());
        assertEquals("RespondedAt should match decline time", declinedTime, decision.getRespondedAt());
    }

    /**
     * Test status transition (PENDING -> LOST)
     */
    @Test
    public void testStatusTransitionLost() {
        Decision decision = new Decision();

        // Start as PENDING
        decision.setStatus("PENDING");
        decision.setUpdatedAt(Timestamp.now());

        // Transition to LOST (lottery not won)
        decision.setStatus("LOST");
        decision.setUpdatedAt(Timestamp.now());

        assertEquals("Status should be LOST", "LOST", decision.getStatus());
        // RespondedAt should remain null for LOST (user didn't respond)
        assertNull("RespondedAt should be null for LOST", decision.getRespondedAt());
    }

    /**
     * Test that all fields can be updated after construction
     */
    @Test
    public void testFieldMutability() {
        Decision decision = new Decision(testDecisionId, testEntrantId, testEntryId,
                                        testStatus, testRespondedAt, testUpdatedAt);

        // Update all fields
        String newDecisionId = "decision999";
        String newEntrantId = "entrant888";
        String newEntryId = "entry777";
        String newStatus = "ACCEPTED";
        Timestamp newRespondedAt = new Timestamp(new Date(System.currentTimeMillis() + 1000));
        Timestamp newUpdatedAt = Timestamp.now();

        decision.setDecisionId(newDecisionId);
        decision.setEntrantId(newEntrantId);
        decision.setEntryId(newEntryId);
        decision.setStatus(newStatus);
        decision.setRespondedAt(newRespondedAt);
        decision.setUpdatedAt(newUpdatedAt);

        // Verify all updates
        assertEquals("DecisionId should be updated", newDecisionId, decision.getDecisionId());
        assertEquals("EntrantId should be updated", newEntrantId, decision.getEntrantId());
        assertEquals("EntryId should be updated", newEntryId, decision.getEntryId());
        assertEquals("Status should be updated", newStatus, decision.getStatus());
        assertEquals("RespondedAt should be updated", newRespondedAt, decision.getRespondedAt());
        assertEquals("UpdatedAt should be updated", newUpdatedAt, decision.getUpdatedAt());
    }

    /**
     * Test creating multiple Decision instances with different data
     */
    @Test
    public void testMultipleDecisionInstances() {
        Decision decision1 = new Decision("dec1", "entrant1", "entry1", "PENDING", null, Timestamp.now());
        Decision decision2 = new Decision("dec2", "entrant2", "entry2", "ACCEPTED", Timestamp.now(), Timestamp.now());

        // Verify they are different
        assertNotEquals("DecisionIds should be different", decision1.getDecisionId(), decision2.getDecisionId());
        assertNotEquals("EntrantIds should be different", decision1.getEntrantId(), decision2.getEntrantId());
        assertNotEquals("Statuses should be different", decision1.getStatus(), decision2.getStatus());

        // Verify they are independent objects
        decision1.setStatus("INVITED");
        assertEquals("Decision1 status should be updated", "INVITED", decision1.getStatus());
        assertEquals("Decision2 status should remain unchanged", "ACCEPTED", decision2.getStatus());
    }

    /**
     * Test entrantId field (reference to user)
     */
    @Test
    public void testEntrantIdField() {
        Decision decision = new Decision();

        assertNull("EntrantId should be null initially", decision.getEntrantId());

        decision.setEntrantId(testEntrantId);
        assertEquals("EntrantId should be set", testEntrantId, decision.getEntrantId());

        String newEntrantId = "newEntrant999";
        decision.setEntrantId(newEntrantId);
        assertEquals("EntrantId should be updated", newEntrantId, decision.getEntrantId());
    }

    /**
     * Test entryId field (reference to waiting list entry)
     */
    @Test
    public void testEntryIdField() {
        Decision decision = new Decision();

        assertNull("EntryId should be null initially", decision.getEntryId());

        decision.setEntryId(testEntryId);
        assertEquals("EntryId should be set", testEntryId, decision.getEntryId());

        String newEntryId = "newEntry999";
        decision.setEntryId(newEntryId);
        assertEquals("EntryId should be updated", newEntryId, decision.getEntryId());
    }

    /**
     * Test decision for lottery winner who accepts
     */
    @Test
    public void testLotteryWinnerAccepts() {
        Decision decision = new Decision();
        decision.setDecisionId("dec123");
        decision.setEntrantId("user456");
        decision.setEntryId("entry789");
        decision.setStatus("INVITED");
        decision.setUpdatedAt(Timestamp.now());

        // User accepts
        Timestamp acceptTime = Timestamp.now();
        decision.setStatus("ACCEPTED");
        decision.setRespondedAt(acceptTime);
        decision.setUpdatedAt(acceptTime);

        assertEquals("Status should be ACCEPTED", "ACCEPTED", decision.getStatus());
        assertNotNull("RespondedAt should be set", decision.getRespondedAt());
    }

    /**
     * Test decision for lottery winner who declines
     */
    @Test
    public void testLotteryWinnerDeclines() {
        Decision decision = new Decision();
        decision.setDecisionId("dec123");
        decision.setEntrantId("user456");
        decision.setEntryId("entry789");
        decision.setStatus("INVITED");
        decision.setUpdatedAt(Timestamp.now());

        // User declines
        Timestamp declineTime = Timestamp.now();
        decision.setStatus("DECLINED");
        decision.setRespondedAt(declineTime);
        decision.setUpdatedAt(declineTime);

        assertEquals("Status should be DECLINED", "DECLINED", decision.getStatus());
        assertNotNull("RespondedAt should be set", decision.getRespondedAt());
    }

    /**
     * Test decision for lottery loser
     */
    @Test
    public void testLotteryLoser() {
        Decision decision = new Decision();
        decision.setDecisionId("dec123");
        decision.setEntrantId("user456");
        decision.setEntryId("entry789");
        decision.setStatus("PENDING");
        decision.setUpdatedAt(Timestamp.now());

        // Lottery drawn, user loses
        decision.setStatus("LOST");
        decision.setUpdatedAt(Timestamp.now());

        assertEquals("Status should be LOST", "LOST", decision.getStatus());
        assertNull("RespondedAt should be null for LOST", decision.getRespondedAt());
    }
}