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






}