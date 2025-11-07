package com.example.arcane.model;

import com.google.firebase.Timestamp;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for Event model class
 * Tests constructors, getters, setters, and field validations
 */
public class EventTest {

    private String testEventId;
    private String testOrganizerId;
    private String testEventName;
    private String testDescription;
    private String testLocation;
    private Double testCost;
    private Integer testMaxEntrants;
    private Integer testNumberOfWinners;
    private Timestamp testEventDate;
    private Timestamp testRegistrationStartDate;
    private Timestamp testRegistrationEndDate;
    private Boolean testGeolocationRequired;
    private String testStatus;

    @Before
    public void setUp() {
        // Initialize test data
        testEventId = "event123";
        testOrganizerId = "organizer456";
        testEventName = "Swimming Lessons for Beginners";
        testDescription = "Learn to swim with certified instructors";
        testLocation = "Community Recreation Centre";
        testCost = 60.0;
        testMaxEntrants = 50;
        testNumberOfWinners = 20;
        testEventDate = new Timestamp(new Date(System.currentTimeMillis() + 86400000)); // Tomorrow
        testRegistrationStartDate = Timestamp.now();
        testRegistrationEndDate = new Timestamp(new Date(System.currentTimeMillis() + 43200000)); // 12 hours from now
        testGeolocationRequired = false;
        testStatus = "OPEN";
    }

    /**
     * Test the no-arg constructor (required by Firestore)
     */
    @Test
    public void testNoArgConstructor() {
        Event event = new Event();

        assertNotNull("Event object should not be null", event);
        assertNull("EventId should be null", event.getEventId());
        assertNull("OrganizerId should be null", event.getOrganizerId());
        assertNull("EventName should be null", event.getEventName());
        assertNull("Description should be null", event.getDescription());
        assertNull("Location should be null", event.getLocation());
        assertNull("Cost should be null", event.getCost());
        assertNull("MaxEntrants should be null", event.getMaxEntrants());
        assertNull("NumberOfWinners should be null", event.getNumberOfWinners());
        assertNull("EventDate should be null", event.getEventDate());
        assertNull("RegistrationStartDate should be null", event.getRegistrationStartDate());
        assertNull("RegistrationEndDate should be null", event.getRegistrationEndDate());
        assertNull("GeolocationRequired should be null", event.getGeolocationRequired());
        assertNull("Status should be null", event.getStatus());
        assertNull("WaitingList should be null", event.getWaitingList());
        assertNull("Decisions should be null", event.getDecisions());
    }

    /**
     * Test the full constructor with all implemented fields
     */
    @Test
    public void testFullConstructor() {
        Event event = new Event(testEventId, testOrganizerId, testEventName, testDescription,
                               null, null, testEventDate, testLocation, testCost,
                               testRegistrationStartDate, testRegistrationEndDate,
                               testMaxEntrants, testNumberOfWinners, testGeolocationRequired, testStatus);

        assertNotNull("Event object should not be null", event);
        assertEquals("EventId should match", testEventId, event.getEventId());
        assertEquals("OrganizerId should match", testOrganizerId, event.getOrganizerId());
        assertEquals("EventName should match", testEventName, event.getEventName());
        assertEquals("Description should match", testDescription, event.getDescription());
        assertEquals("Location should match", testLocation, event.getLocation());
        assertEquals("Cost should match", testCost, event.getCost());
        assertEquals("MaxEntrants should match", testMaxEntrants, event.getMaxEntrants());
        assertEquals("NumberOfWinners should match", testNumberOfWinners, event.getNumberOfWinners());
        assertEquals("EventDate should match", testEventDate, event.getEventDate());
        assertEquals("RegistrationStartDate should match", testRegistrationStartDate, event.getRegistrationStartDate());
        assertEquals("RegistrationEndDate should match", testRegistrationEndDate, event.getRegistrationEndDate());
        assertEquals("GeolocationRequired should match", testGeolocationRequired, event.getGeolocationRequired());
        assertEquals("Status should match", testStatus, event.getStatus());
    }

    /**
     * Test constructor with optional fields set to null
     */
    @Test
    public void testConstructorWithNullOptionalFields() {
        Event event = new Event(testEventId, testOrganizerId, testEventName, testDescription,
                               null, null, testEventDate, testLocation, null,
                               testRegistrationStartDate, testRegistrationEndDate,
                               null, testNumberOfWinners, false, testStatus);

        assertNotNull("Event object should not be null", event);
        assertEquals("EventId should match", testEventId, event.getEventId());
        assertNull("Cost should be null", event.getCost());
        assertNull("MaxEntrants should be null", event.getMaxEntrants());
        assertEquals("NumberOfWinners should match", testNumberOfWinners, event.getNumberOfWinners());
    }

    /**
     * Test all getter and setter methods for implemented fields
     */
    @Test
    public void testGettersAndSetters() {
        Event event = new Event();

        // Test EventId
        event.setEventId(testEventId);
        assertEquals("EventId getter should return set value", testEventId, event.getEventId());

        // Test OrganizerId
        event.setOrganizerId(testOrganizerId);
        assertEquals("OrganizerId getter should return set value", testOrganizerId, event.getOrganizerId());

        // Test EventName
        event.setEventName(testEventName);
        assertEquals("EventName getter should return set value", testEventName, event.getEventName());

        // Test Description
        event.setDescription(testDescription);
        assertEquals("Description getter should return set value", testDescription, event.getDescription());

        // Test Location
        event.setLocation(testLocation);
        assertEquals("Location getter should return set value", testLocation, event.getLocation());

        // Test Cost
        event.setCost(testCost);
        assertEquals("Cost getter should return set value", testCost, event.getCost());

        // Test MaxEntrants
        event.setMaxEntrants(testMaxEntrants);
        assertEquals("MaxEntrants getter should return set value", testMaxEntrants, event.getMaxEntrants());

        // Test NumberOfWinners
        event.setNumberOfWinners(testNumberOfWinners);
        assertEquals("NumberOfWinners getter should return set value", testNumberOfWinners, event.getNumberOfWinners());

        // Test EventDate
        event.setEventDate(testEventDate);
        assertEquals("EventDate getter should return set value", testEventDate, event.getEventDate());

        // Test RegistrationStartDate
        event.setRegistrationStartDate(testRegistrationStartDate);
        assertEquals("RegistrationStartDate getter should return set value", testRegistrationStartDate, event.getRegistrationStartDate());

        // Test RegistrationEndDate
        event.setRegistrationEndDate(testRegistrationEndDate);
        assertEquals("RegistrationEndDate getter should return set value", testRegistrationEndDate, event.getRegistrationEndDate());

        // Test GeolocationRequired
        event.setGeolocationRequired(true);
        assertEquals("GeolocationRequired getter should return set value", Boolean.TRUE, event.getGeolocationRequired());

        // Test Status
        event.setStatus(testStatus);
        assertEquals("Status getter should return set value", testStatus, event.getStatus());
    }

    /**
     * Test numberOfWinners field (lottery capacity)
     */
    @Test
    public void testNumberOfWinnersField() {
        Event event = new Event();

        assertNull("NumberOfWinners should be null initially", event.getNumberOfWinners());

        event.setNumberOfWinners(20);
        assertEquals("NumberOfWinners should be 20", Integer.valueOf(20), event.getNumberOfWinners());

        event.setNumberOfWinners(5);
        assertEquals("NumberOfWinners should be updated to 5", Integer.valueOf(5), event.getNumberOfWinners());

        event.setNumberOfWinners(null);
        assertNull("NumberOfWinners should be null", event.getNumberOfWinners());
    }

    /**
     * Test maxEntrants optional field (waiting list capacity)
     */
    @Test
    public void testMaxEntrantsOptional() {
        Event event = new Event();

        assertNull("MaxEntrants should be null initially", event.getMaxEntrants());

        event.setMaxEntrants(50);
        assertEquals("MaxEntrants should be 50", Integer.valueOf(50), event.getMaxEntrants());

        event.setMaxEntrants(null);
        assertNull("MaxEntrants should be null after setting to null", event.getMaxEntrants());
    }

    /**
     * Test cost optional field
     */
    @Test
    public void testCostOptional() {
        Event event = new Event();

        assertNull("Cost should be null initially", event.getCost());

        event.setCost(25.50);
        assertEquals("Cost should be 25.50", Double.valueOf(25.50), event.getCost(), 0.001);

        event.setCost(0.0);
        assertEquals("Cost should be 0.0 for free events", Double.valueOf(0.0), event.getCost(), 0.001);

        event.setCost(null);
        assertNull("Cost should be null after setting to null", event.getCost());
    }

    /**
     * Test event status field
     */
    @Test
    public void testStatusField() {
        Event event = new Event();

        // Test various status values
        event.setStatus("DRAFT");
        assertEquals("Status should be DRAFT", "DRAFT", event.getStatus());

        event.setStatus("OPEN");
        assertEquals("Status should be OPEN", "OPEN", event.getStatus());

        event.setStatus("CLOSED");
        assertEquals("Status should be CLOSED", "CLOSED", event.getStatus());

        event.setStatus("DRAWN");
        assertEquals("Status should be DRAWN", "DRAWN", event.getStatus());

        event.setStatus("COMPLETED");
        assertEquals("Status should be COMPLETED", "COMPLETED", event.getStatus());

        event.setStatus(null);
        assertNull("Status should be null", event.getStatus());
    }

    /**
     * Test event date fields
     */
    @Test
    public void testEventDates() {
        Event event = new Event();

        Timestamp eventDate = new Timestamp(new Date());
        Timestamp startDate = new Timestamp(new Date(System.currentTimeMillis() - 86400000)); // Yesterday
        Timestamp endDate = new Timestamp(new Date(System.currentTimeMillis() + 86400000)); // Tomorrow

        event.setEventDate(eventDate);
        event.setRegistrationStartDate(startDate);
        event.setRegistrationEndDate(endDate);

        assertEquals("EventDate should match", eventDate, event.getEventDate());
        assertEquals("RegistrationStartDate should match", startDate, event.getRegistrationStartDate());
        assertEquals("RegistrationEndDate should match", endDate, event.getRegistrationEndDate());
    }

    /**
     * Test waitingList operations (OOP composition)
     */
    @Test
    public void testWaitingListOperations() {
        Event event = new Event();

        // Initially null
        assertNull("WaitingList should be null initially", event.getWaitingList());

        // Add entry using addToWaitingList method
        WaitingListEntry entry1 = new WaitingListEntry();
        event.addToWaitingList(entry1);

        assertNotNull("WaitingList should not be null after adding entry", event.getWaitingList());
        assertEquals("WaitingList should have 1 entry", 1, event.getWaitingList().size());
        assertTrue("WaitingList should contain entry1", event.getWaitingList().contains(entry1));

        // Add more entries
        WaitingListEntry entry2 = new WaitingListEntry();
        WaitingListEntry entry3 = new WaitingListEntry();
        event.addToWaitingList(entry2);
        event.addToWaitingList(entry3);

        assertEquals("WaitingList should have 3 entries", 3, event.getWaitingList().size());
    }

    /**
     * Test setWaitingList with a new list
     */
    @Test
    public void testSetWaitingList() {
        Event event = new Event();

        List<WaitingListEntry> waitingList = new ArrayList<>();
        waitingList.add(new WaitingListEntry());
        waitingList.add(new WaitingListEntry());

        event.setWaitingList(waitingList);

        assertNotNull("WaitingList should not be null", event.getWaitingList());
        assertEquals("WaitingList should have 2 entries", 2, event.getWaitingList().size());
        assertEquals("WaitingList should match the set list", waitingList, event.getWaitingList());
    }

    /**
     * Test decisions list operations (OOP composition)
     */
    @Test
    public void testDecisionsOperations() {
        Event event = new Event();

        // Initially null
        assertNull("Decisions should be null initially", event.getDecisions());

        // Set decisions list
        List<Decision> decisions = new ArrayList<>();
        decisions.add(new Decision());
        decisions.add(new Decision());
        decisions.add(new Decision());

        event.setDecisions(decisions);

        assertNotNull("Decisions should not be null", event.getDecisions());
        assertEquals("Decisions should have 3 entries", 3, event.getDecisions().size());
        assertEquals("Decisions should match the set list", decisions, event.getDecisions());
    }

    /**
     * Test that all fields can be updated after construction
     */
    @Test
    public void testFieldMutability() {
        Event event = new Event(testEventId, testOrganizerId, testEventName, testDescription,
                               null, null, testEventDate, testLocation, testCost,
                               testRegistrationStartDate, testRegistrationEndDate,
                               testMaxEntrants, testNumberOfWinners, testGeolocationRequired, testStatus);

        // Update all fields
        String newEventId = "event999";
        String newOrganizerId = "organizer999";
        String newEventName = "Piano Lessons";
        String newDescription = "Learn to play piano";
        String newLocation = "Music Hall";
        Double newCost = 75.0;
        Integer newMaxEntrants = 30;
        Integer newNumberOfWinners = 15;
        Timestamp newEventDate = new Timestamp(new Date(System.currentTimeMillis() + 172800000));
        Timestamp newRegStart = Timestamp.now();
        Timestamp newRegEnd = new Timestamp(new Date(System.currentTimeMillis() + 86400000));
        Boolean newGeoRequired = true;
        String newStatus = "CLOSED";

        event.setEventId(newEventId);
        event.setOrganizerId(newOrganizerId);
        event.setEventName(newEventName);
        event.setDescription(newDescription);
        event.setLocation(newLocation);
        event.setCost(newCost);
        event.setMaxEntrants(newMaxEntrants);
        event.setNumberOfWinners(newNumberOfWinners);
        event.setEventDate(newEventDate);
        event.setRegistrationStartDate(newRegStart);
        event.setRegistrationEndDate(newRegEnd);
        event.setGeolocationRequired(newGeoRequired);
        event.setStatus(newStatus);

        // Verify all updates
        assertEquals("EventId should be updated", newEventId, event.getEventId());
        assertEquals("OrganizerId should be updated", newOrganizerId, event.getOrganizerId());
        assertEquals("EventName should be updated", newEventName, event.getEventName());
        assertEquals("Description should be updated", newDescription, event.getDescription());
        assertEquals("Location should be updated", newLocation, event.getLocation());
        assertEquals("Cost should be updated", newCost, event.getCost());
        assertEquals("MaxEntrants should be updated", newMaxEntrants, event.getMaxEntrants());
        assertEquals("NumberOfWinners should be updated", newNumberOfWinners, event.getNumberOfWinners());
        assertEquals("EventDate should be updated", newEventDate, event.getEventDate());
        assertEquals("RegistrationStartDate should be updated", newRegStart, event.getRegistrationStartDate());
        assertEquals("RegistrationEndDate should be updated", newRegEnd, event.getRegistrationEndDate());
        assertEquals("GeolocationRequired should be updated", newGeoRequired, event.getGeolocationRequired());
        assertEquals("Status should be updated", newStatus, event.getStatus());
    }

    /**
     * Test creating multiple Event instances with different data
     */
    @Test
    public void testMultipleEventInstances() {
        Event event1 = new Event("event1", "org1", "Swimming", "Swimming lessons",
                                null, null, testEventDate, "Pool", 60.0,
                                testRegistrationStartDate, testRegistrationEndDate,
                                50, 20, false, "OPEN");

        Event event2 = new Event("event2", "org2", "Yoga", "Yoga workshop",
                                null, null, testEventDate, "Gym", 30.0,
                                testRegistrationStartDate, testRegistrationEndDate,
                                30, 10, true, "CLOSED");

        // Verify they are different
        assertNotEquals("EventIds should be different", event1.getEventId(), event2.getEventId());
        assertNotEquals("EventNames should be different", event1.getEventName(), event2.getEventName());
        assertNotEquals("Costs should be different", event1.getCost(), event2.getCost());
        assertNotEquals("Statuses should be different", event1.getStatus(), event2.getStatus());

        // Verify they are independent objects
        event1.setEventName("Swimming Updated");
        assertEquals("Event1 name should be updated", "Swimming Updated", event1.getEventName());
        assertEquals("Event2 name should remain unchanged", "Yoga", event2.getEventName());
    }

    /**
     * Test empty waiting list initialization
     */
    @Test
    public void testEmptyWaitingListInitialization() {
        Event event = new Event();

        event.setWaitingList(new ArrayList<>());

        assertNotNull("WaitingList should not be null", event.getWaitingList());
        assertTrue("WaitingList should be empty", event.getWaitingList().isEmpty());
        assertEquals("WaitingList size should be 0", 0, event.getWaitingList().size());
    }

    /**
     * Test empty decisions list initialization
     */
    @Test
    public void testEmptyDecisionsInitialization() {
        Event event = new Event();

        event.setDecisions(new ArrayList<>());

        assertNotNull("Decisions should not be null", event.getDecisions());
        assertTrue("Decisions should be empty", event.getDecisions().isEmpty());
        assertEquals("Decisions size should be 0", 0, event.getDecisions().size());
    }
}