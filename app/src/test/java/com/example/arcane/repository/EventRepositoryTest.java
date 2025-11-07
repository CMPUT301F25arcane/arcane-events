package com.example.arcane.repository;

import com.example.arcane.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventRepository
 */
@RunWith(MockitoJUnitRunner.class)
public class EventRepositoryTest {

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private CollectionReference mockCollectionRef;

    @Mock
    private DocumentReference mockDocumentRef;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private Query mockQuery;

    private EventRepository eventRepository;
    private Event testEvent;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test event
        testEvent = new Event();
        testEvent.setEventId("event123");
        testEvent.setOrganizerId("organizer456");
        testEvent.setEventName("Test Event");
        testEvent.setDescription("Test Description");
        testEvent.setLocation("Test Location");
        testEvent.setCost(50.0);
        testEvent.setMaxEntrants(100);
        testEvent.setNumberOfWinners(20);
        testEvent.setEventDate(new Timestamp(new Date()));
        testEvent.setStatus("OPEN");

        // Setup mock chain for Firestore
        when(mockDb.collection("events")).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(anyString())).thenReturn(mockDocumentRef);

        // Create EventRepository with mocked FirebaseFirestore (IoC)
        eventRepository = new EventRepository(mockDb);
    }

    /**
     * Test getEventById - verifies event is retrieved by ID
     */
    @Test
    public void testGetEventById() {
        // Arrange
        String eventId = "event123";
        Task<DocumentSnapshot> mockTask = Tasks.forResult(mockDocumentSnapshot);
        when(mockDocumentRef.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<DocumentSnapshot> result = eventRepository.getEventById(eventId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain document snapshot", mockDocumentSnapshot, result.getResult());
        verify(mockDb).collection("events");
        verify(mockCollectionRef).document(eventId);
        verify(mockDocumentRef).get();
    }

    /**
     * Test getEventById with empty ID
     */
    @Test
    public void testGetEventByIdWithEmptyId() {
        // Arrange
        String emptyId = "";
        Task<DocumentSnapshot> mockTask = Tasks.forResult(mockDocumentSnapshot);
        when(mockDocumentRef.get()).thenReturn(mockTask);

        // Act - Call actual repository method with empty ID
        Task<DocumentSnapshot> result = eventRepository.getEventById(emptyId);

        // Assert - Repository should handle empty ID (Firestore allows it)
        assertNotNull("Result should not be null", result);
        verify(mockDb).collection("events");
        verify(mockCollectionRef).document(emptyId);
    }

    /**
     * Test getAllEvents - verifies all events are retrieved
     */
    @Test
    public void testGetAllEvents() {
        // Arrange
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockCollectionRef.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = eventRepository.getAllEvents();

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());
        verify(mockDb).collection("events");
        verify(mockCollectionRef).get();
    }

    /**
     * Test getEventsByOrganizer - verifies query by organizer ID
     */
    @Test
    public void testGetEventsByOrganizer() {
        // Arrange
        String organizerId = "organizer456";
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockCollectionRef.whereEqualTo("organizerId", organizerId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = eventRepository.getEventsByOrganizer(organizerId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());
        verify(mockDb).collection("events");
        verify(mockCollectionRef).whereEqualTo("organizerId", organizerId);
        verify(mockQuery).get();
    }

    /**
     * Test getEventsByOrganizer returns empty result when no match
     */
    @Test
    public void testGetEventsByOrganizerNoMatch() {
        // Arrange
        String organizerId = "nonexistent-organizer";
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockCollectionRef.whereEqualTo("organizerId", organizerId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = eventRepository.getEventsByOrganizer(organizerId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertTrue("Query result should be empty", result.getResult().isEmpty());
        verify(mockDb).collection("events");
        verify(mockCollectionRef).whereEqualTo("organizerId", organizerId);
        verify(mockQuery).get();
    }

}