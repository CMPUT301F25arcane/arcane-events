package com.example.arcane.repository;

import com.example.arcane.model.WaitingListEntry;
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
 * Unit tests for WaitingListRepository
 */
@RunWith(MockitoJUnitRunner.class)
public class WaitingListRepositoryTest {

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private CollectionReference mockEventsCollectionRef;

    @Mock
    private DocumentReference mockEventDocumentRef;

    @Mock
    private CollectionReference mockWaitingListCollectionRef;

    @Mock
    private DocumentReference mockEntryDocumentRef;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private Query mockQuery;

    private WaitingListRepository waitingListRepository;
    private WaitingListEntry testEntry;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test waiting list entry
        testEntry = new WaitingListEntry();
        testEntry.setEntryId("entry123");
        testEntry.setEntrantId("entrant456");
        testEntry.setJoinTimestamp(new Timestamp(new Date()));

        // Setup mock chain for Firestore subcollection
        when(mockDb.collection("events")).thenReturn(mockEventsCollectionRef);
        when(mockEventsCollectionRef.document(anyString())).thenReturn(mockEventDocumentRef);
        when(mockEventDocumentRef.collection("waitingList")).thenReturn(mockWaitingListCollectionRef);
        when(mockWaitingListCollectionRef.document(anyString())).thenReturn(mockEntryDocumentRef);

        // Create WaitingListRepository with mocked FirebaseFirestore
        waitingListRepository = new WaitingListRepository(mockDb);
    }

    /**
     * Test addToWaitingList - verifies entry is added to event's waiting list subcollection
     */
    @Test
    public void testAddToWaitingList() {
        // Arrange
        String eventId = "event123";
        Task<DocumentReference> mockTask = Tasks.forResult(mockEntryDocumentRef);
        when(mockWaitingListCollectionRef.add(any(WaitingListEntry.class))).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<DocumentReference> result = waitingListRepository.addToWaitingList(eventId, testEntry);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain document reference", mockEntryDocumentRef, result.getResult());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("waitingList");
        verify(mockWaitingListCollectionRef).add(testEntry);
    }

    /**
     * Test getWaitingListForEvent - verifies all waiting list entries for an event are retrieved
     */
    @Test
    public void testGetWaitingListForEvent() {
        // Arrange
        String eventId = "event123";
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockWaitingListCollectionRef.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = waitingListRepository.getWaitingListForEvent(eventId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("waitingList");
        verify(mockWaitingListCollectionRef).get();
    }

    /**
     * Test getWaitingListEntry - verifies entry is retrieved by ID
     */
    @Test
    public void testGetWaitingListEntry() {
        // Arrange
        String eventId = "event123";
        String entryId = "entry123";
        Task<DocumentSnapshot> mockTask = Tasks.forResult(mockDocumentSnapshot);
        when(mockEntryDocumentRef.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<DocumentSnapshot> result = waitingListRepository.getWaitingListEntry(eventId, entryId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain document snapshot", mockDocumentSnapshot, result.getResult());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("waitingList");
        verify(mockWaitingListCollectionRef).document(entryId);
        verify(mockEntryDocumentRef).get();
    }

    /**
     * Test checkUserInWaitingList - verifies checking if user is already in waiting list
     */
    @Test
    public void testCheckUserInWaitingList() {
        // Arrange
        String eventId = "event123";
        String entrantId = "entrant456";
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockWaitingListCollectionRef.whereEqualTo("entrantId", entrantId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = waitingListRepository.checkUserInWaitingList(eventId, entrantId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("waitingList");
        verify(mockWaitingListCollectionRef).whereEqualTo("entrantId", entrantId);
        verify(mockQuery).get();
    }

    /**
     * Test removeFromWaitingList - verifies entry is deleted from waiting list
     */
    @Test
    public void testRemoveFromWaitingList() {
        // Arrange
        String eventId = "event123";
        String entryId = "entry123";
        Task<Void> mockTask = Tasks.forResult(null);
        when(mockEntryDocumentRef.delete()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<Void> result = waitingListRepository.removeFromWaitingList(eventId, entryId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("waitingList");
        verify(mockWaitingListCollectionRef).document(entryId);
        verify(mockEntryDocumentRef).delete();
    }

    /**
     * Test updateWaitingListEntry - verifies entry is updated in Firestore
     */
    @Test
    public void testUpdateWaitingListEntry() {
        // Arrange
        String eventId = "event123";
        String entryId = "entry123";
        testEntry.setInvitedAt(new Timestamp(new Date()));
        Task<Void> mockTask = Tasks.forResult(null);
        when(mockEntryDocumentRef.set(any(WaitingListEntry.class))).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<Void> result = waitingListRepository.updateWaitingListEntry(eventId, entryId, testEntry);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("waitingList");
        verify(mockWaitingListCollectionRef).document(entryId);
        verify(mockEntryDocumentRef).set(testEntry);
    }

    /**
     * Test getWaitingListEntriesByUser - verifies collection group query for user's entries
     */
    @Test
    public void testGetWaitingListEntriesByUser() {
        // Arrange
        String entrantId = "entrant456";
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockDb.collectionGroup("waitingList")).thenReturn(mockQuery);
        when(mockQuery.whereEqualTo("entrantId", entrantId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = waitingListRepository.getWaitingListEntriesByUser(entrantId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());
        verify(mockDb).collectionGroup("waitingList");
        verify(mockQuery).whereEqualTo("entrantId", entrantId);
        verify(mockQuery).get();
    }

    /**
     * Test checkUserInWaitingList when user is not in waiting list
     */
    @Test
    public void testCheckUserInWaitingListNotFound() {
        // Arrange
        String eventId = "event123";
        String entrantId = "nonexistent-entrant";
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockWaitingListCollectionRef.whereEqualTo("entrantId", entrantId)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = waitingListRepository.checkUserInWaitingList(eventId, entrantId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertTrue("Query result should be empty", result.getResult().isEmpty());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("waitingList");
        verify(mockWaitingListCollectionRef).whereEqualTo("entrantId", entrantId);
        verify(mockQuery).get();
    }

    /**
     * Test addToWaitingList with complete WaitingListEntry object
     */
    @Test
    public void testAddToWaitingListWithCompleteEntry() {
        // Arrange
        String eventId = "event999";
        WaitingListEntry completeEntry = new WaitingListEntry();
        completeEntry.setEntryId("entry999");
        completeEntry.setEntrantId("entrant999");
        completeEntry.setJoinTimestamp(new Timestamp(new Date()));
        completeEntry.setInvitedAt(new Timestamp(new Date()));

        Task<DocumentReference> mockTask = Tasks.forResult(mockEntryDocumentRef);
        when(mockWaitingListCollectionRef.add(any(WaitingListEntry.class))).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<DocumentReference> result = waitingListRepository.addToWaitingList(eventId, completeEntry);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("waitingList");
        verify(mockWaitingListCollectionRef).add(completeEntry);
    }

    /**
     * Test getWaitingListForEvent returns empty list when no entries exist
     */
    @Test
    public void testGetWaitingListForEventEmpty() {
        // Arrange
        String eventId = "event123";
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockWaitingListCollectionRef.get()).thenReturn(mockTask);

        // Act - Call actual repository method
        Task<QuerySnapshot> result = waitingListRepository.getWaitingListForEvent(eventId);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertTrue("Query result should be empty", result.getResult().isEmpty());
        verify(mockDb).collection("events");
        verify(mockEventsCollectionRef).document(eventId);
        verify(mockEventDocumentRef).collection("waitingList");
        verify(mockWaitingListCollectionRef).get();
    }
}