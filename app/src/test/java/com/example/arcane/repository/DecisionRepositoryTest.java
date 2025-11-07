package com.example.arcane.repository;

import com.example.arcane.model.Decision;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DecisionRepository
 */
@RunWith(MockitoJUnitRunner.class)
public class DecisionRepositoryTest {

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private CollectionReference mockEventsCollectionRef;

    @Mock
    private DocumentReference mockEventDocumentRef;

    @Mock
    private CollectionReference mockDecisionsCollectionRef;

    @Mock
    private DocumentReference mockDecisionDocumentRef;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private Query mockQuery;

    private DecisionRepository decisionRepository;
    private Decision testDecision;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test decision
        testDecision = new Decision();
        testDecision.setDecisionId("decision123");
        testDecision.setEntrantId("entrant456");
        testDecision.setEntryId("entry789");
        testDecision.setStatus("INVITED");
        testDecision.setUpdatedAt(new Timestamp(new Date()));

        // Setup mock chain for Firestore subcollection
        when(mockDb.collection("events")).thenReturn(mockEventsCollectionRef);
        when(mockEventsCollectionRef.document(anyString())).thenReturn(mockEventDocumentRef);
        when(mockEventDocumentRef.collection("decisions")).thenReturn(mockDecisionsCollectionRef);
        when(mockDecisionsCollectionRef.document(anyString())).thenReturn(mockDecisionDocumentRef);

        // Create DecisionRepository with mocked FirebaseFirestore (IoC)
        decisionRepository = new DecisionRepository(mockDb);
    }

    /**
     * Test createDecision - verifies decision is added to event's decisions subcollection
     */
    @Test
    public void testCreateDecision() {
        // Arrange
        String eventId = "event123";
        Task<DocumentReference> mockTask = Tasks.forResult(mockDecisionDocumentRef);
        when(mockDecisionsCollectionRef.add(any(Decision.class))).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<DocumentReference> result = decisionRepository.createDecision(eventId, testDecision);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain document reference", mockDecisionDocumentRef, result.getResult());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("decisions");
        verify(mockDecisionsCollectionRef).add(testDecision);
    }

    /**
     * Test getDecisionsForEvent - verifies all decisions for an event are retrieved
     */
    @Test
    public void testGetDecisionsForEvent() {
        // Arrange
        String eventId = "event123";
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockDecisionsCollectionRef.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = decisionRepository.getDecisionsForEvent(eventId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("decisions");
        verify(mockDecisionsCollectionRef).get();
    }

    /**
     * Test getDecisionForUser - verifies decision for specific user in event is retrieved
     */
    @Test
    public void testGetDecisionForUser() {
        // Arrange
        String eventId = "event123";
        String entrantId = "entrant456";
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockDecisionsCollectionRef.whereEqualTo("entrantId", entrantId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = decisionRepository.getDecisionForUser(eventId, entrantId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("decisions");
        verify(mockDecisionsCollectionRef).whereEqualTo("entrantId", entrantId);
        verify(mockQuery).get();
    }

    /**
     * Test getDecisionById - verifies decision is retrieved by ID
     */
    @Test
    public void testGetDecisionById() {
        // Arrange
        String eventId = "event123";
        String decisionId = "decision123";
        Task<DocumentSnapshot> mockTask = Tasks.forResult(mockDocumentSnapshot);
        when(mockDecisionDocumentRef.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<DocumentSnapshot> result = decisionRepository.getDecisionById(eventId, decisionId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain document snapshot", mockDocumentSnapshot, result.getResult());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("decisions");
        verify(mockDecisionsCollectionRef).document(decisionId);
        verify(mockDecisionDocumentRef).get();
    }

    /**
     * Test updateDecision - verifies decision is updated in Firestore
     */
    @Test
    public void testUpdateDecision() {
        // Arrange
        String eventId = "event123";
        String decisionId = "decision123";
        testDecision.setStatus("ACCEPTED");
        Task<Void> mockTask = Tasks.forResult(null);
        when(mockDecisionDocumentRef.set(any(Decision.class))).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<Void> result = decisionRepository.updateDecision(eventId, decisionId, testDecision);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("decisions");
        verify(mockDecisionsCollectionRef).document(decisionId);
        verify(mockDecisionDocumentRef).set(testDecision);
    }

    /**
     * Test deleteDecision - verifies decision is deleted from Firestore
     */
    @Test
    public void testDeleteDecision() {
        // Arrange
        String eventId = "event123";
        String decisionId = "decision123";
        Task<Void> mockTask = Tasks.forResult(null);
        when(mockDecisionDocumentRef.delete()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<Void> result = decisionRepository.deleteDecision(eventId, decisionId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("decisions");
        verify(mockDecisionsCollectionRef).document(decisionId);
        verify(mockDecisionDocumentRef).delete();
    }

    /**
     * Test getDecisionsByUser - verifies collection group query for user's decisions
     */
    @Test
    public void testGetDecisionsByUser() {
        // Arrange
        String entrantId = "entrant456";
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockDb.collectionGroup("decisions")).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo("entrantId", entrantId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = decisionRepository.getDecisionsByUser(entrantId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());
        verify(mockDb).collectionGroup("decisions");
        verify(mockQuery).whereEqualTo("entrantId", entrantId);
        verify(mockQuery).get();
    }

    /**
     * Test getDecisionsByStatus - verifies decisions filtered by status
     */
    @Test
    public void testGetDecisionsByStatus() {
        // Arrange
        String eventId = "event123";
        String status = "INVITED";
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockDecisionsCollectionRef.whereEqualTo("status", status)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = decisionRepository.getDecisionsByStatus(eventId, status);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("decisions");
        verify(mockDecisionsCollectionRef).whereEqualTo("status", status);
        verify(mockQuery).get();
    }

    /**
     * Test getDecisionForUser with no matching decision
     */
    @Test
    public void testGetDecisionForUserNoMatch() {
        // Arrange
        String eventId = "event123";
        String entrantId = "nonexistent-entrant";
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockDecisionsCollectionRef.whereEqualTo("entrantId", entrantId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = decisionRepository.getDecisionForUser(eventId, entrantId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertTrue("Query result should be empty", result.getResult().isEmpty());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("decisions");
        verify(mockDecisionsCollectionRef).whereEqualTo("entrantId", entrantId);
        verify(mockQuery).get();
    }

    /**
     * Test createDecision with complete Decision object
     */
    @Test
    public void testCreateDecisionWithCompleteObject() {
        // Arrange
        String eventId = "event999";
        Decision completeDecision = new Decision();
        completeDecision.setDecisionId("decision999");
        completeDecision.setEntrantId("entrant999");
        completeDecision.setEntryId("entry999");
        completeDecision.setStatus("PENDING");
        completeDecision.setRespondedAt(new Timestamp(new Date()));
        completeDecision.setUpdatedAt(new Timestamp(new Date()));

        Task<DocumentReference> mockTask = Tasks.forResult(mockDecisionDocumentRef);
        when(mockDecisionsCollectionRef.add(any(Decision.class))).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<DocumentReference> result = decisionRepository.createDecision(eventId, completeDecision);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("decisions");
        verify(mockDecisionsCollectionRef).add(completeDecision);
    }
}