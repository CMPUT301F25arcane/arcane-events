package com.example.arcane.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.arcane.model.Event;
import com.example.arcane.model.UserProfile;
import com.example.arcane.model.WaitingListEntry;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.repository.WaitingListRepository;
import com.example.arcane.service.NotificationService;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import android.os.Looper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * Unit tests exercising the core business logic paths in {@link EventService}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class EventServiceTest {

    private static final String EVENT_ID = "event-123";
    private static final String ENTRANT_ID = "user-456";
    private static final String ENTRY_ID = "entry-42";
    private static final String DECISION_ID = "decision-314";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private EventRepository eventRepository;

    @Mock
    private WaitingListRepository waitingListRepository;

    @Mock
    private DecisionRepository decisionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private EventService subject;

    @Before
    public void setUp() {
        subject = new EventService(eventRepository, waitingListRepository, decisionRepository, userRepository, notificationService);
    }

    @Test
    public void joinWaitingList_returnsAlreadyExistsWhenUserPresent() throws Exception {
        QuerySnapshot existingSnapshot = mockQuerySnapshot(false);
        when(waitingListRepository.checkUserInWaitingList(EVENT_ID, ENTRANT_ID))
                .thenReturn(Tasks.forResult(existingSnapshot));

        // Call with null sessionLocation (tests don't require location)
        com.google.android.gms.tasks.Task<Map<String, String>> task = subject.joinWaitingList(EVENT_ID, ENTRANT_ID, null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());
        Map<String, String> result = task.getResult();

        assertNotNull(result);
        assertEquals("already_exists", result.get("status"));
        verify(waitingListRepository, never()).addToWaitingList(anyString(), any(WaitingListEntry.class));
        verify(decisionRepository, never()).createDecision(anyString(), any());
    }

    @Test
    public void joinWaitingList_successCreatesEntryDecisionAndUpdatesUser() throws Exception {
        QuerySnapshot emptySnapshot = mockQuerySnapshot(true);
        when(waitingListRepository.checkUserInWaitingList(EVENT_ID, ENTRANT_ID))
                .thenReturn(Tasks.forResult(emptySnapshot));

        // Mock event fetch (required for geolocationRequired check)
        Event testEvent = new Event();
        testEvent.setGeolocationRequired(false); // Test with geolocation not required
        DocumentSnapshot eventSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(eventSnapshot.exists()).thenReturn(true);
        when(eventSnapshot.toObject(Event.class)).thenReturn(testEvent);
        when(eventRepository.getEventById(EVENT_ID))
                .thenReturn(Tasks.forResult(eventSnapshot));

        DocumentReference entryRef = mockDocumentReference(ENTRY_ID);
        when(waitingListRepository.addToWaitingList(anyString(), any(WaitingListEntry.class)))
                .thenReturn(Tasks.forResult(entryRef));

        DocumentReference decisionRef = mockDocumentReference(DECISION_ID);
        when(decisionRepository.createDecision(anyString(), any()))
                .thenReturn(Tasks.forResult(decisionRef));

        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(ENTRANT_ID);
        userProfile.setRegisteredEventIds(new ArrayList<>());

        DocumentSnapshot userSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(userSnapshot.toObject(UserProfile.class)).thenReturn(userProfile);
        when(userRepository.getUserById(ENTRANT_ID)).thenReturn(Tasks.forResult(userSnapshot));
        when(userRepository.updateUser(any(UserProfile.class))).thenReturn(Tasks.forResult(null));

        // Call with null sessionLocation (tests don't require location)
        com.google.android.gms.tasks.Task<Map<String, String>> task = subject.joinWaitingList(EVENT_ID, ENTRANT_ID, null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());
        Map<String, String> result = task.getResult();

        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals(ENTRY_ID, result.get("entryId"));
        assertEquals(DECISION_ID, result.get("decisionId"));

        ArgumentCaptor<WaitingListEntry> entryCaptor = ArgumentCaptor.forClass(WaitingListEntry.class);
        verify(waitingListRepository).addToWaitingList(org.mockito.Mockito.eq(EVENT_ID), entryCaptor.capture());
        assertEquals(ENTRANT_ID, entryCaptor.getValue().getEntrantId());
        assertNotNull(entryCaptor.getValue().getJoinTimestamp());

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userRepository).updateUser(profileCaptor.capture());
        List<String> registeredEvents = profileCaptor.getValue().getRegisteredEventIds();
        assertNotNull(registeredEvents);
        assertEquals(1, registeredEvents.size());
        assertEquals(EVENT_ID, registeredEvents.get(0));
    }

    @Test
    public void leaveWaitingList_removesEntryAndUpdatesUserProfile() throws Exception {
        // Mock decision lookup - when decisionId is null, it queries for decision
        QueryDocumentSnapshot decisionDoc = org.mockito.Mockito.mock(QueryDocumentSnapshot.class);
        when(decisionDoc.getId()).thenReturn(DECISION_ID);
        
        QuerySnapshot decisionSnapshot = org.mockito.Mockito.mock(QuerySnapshot.class);
        when(decisionSnapshot.isEmpty()).thenReturn(false);
        when(decisionSnapshot.getDocuments()).thenReturn(Arrays.asList(decisionDoc));
        
        when(decisionRepository.getDecisionForUser(EVENT_ID, ENTRANT_ID))
                .thenReturn(Tasks.forResult(decisionSnapshot));
        
        // Mock decision deletion
        when(decisionRepository.deleteDecision(EVENT_ID, DECISION_ID))
                .thenReturn(Tasks.forResult(null));
        
        // Mock waiting list removal
        when(waitingListRepository.removeFromWaitingList(EVENT_ID, ENTRY_ID))
                .thenReturn(Tasks.forResult(null));

        // Mock user profile
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(ENTRANT_ID);
        userProfile.setRegisteredEventIds(new ArrayList<>(Arrays.asList(EVENT_ID, "other")));

        DocumentSnapshot userSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(userSnapshot.toObject(UserProfile.class)).thenReturn(userProfile);

        when(userRepository.getUserById(ENTRANT_ID)).thenReturn(Tasks.forResult(userSnapshot));
        when(userRepository.updateUser(any(UserProfile.class))).thenReturn(Tasks.forResult(null));

        // Call with null decisionId - it will be looked up
        com.google.android.gms.tasks.Task<Void> task = subject.leaveWaitingList(EVENT_ID, ENTRANT_ID, ENTRY_ID, null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());
        Void completion = task.getResult();
        assertNull(completion);

        // Verify all operations were called
        verify(decisionRepository).getDecisionForUser(EVENT_ID, ENTRANT_ID);
        verify(decisionRepository).deleteDecision(EVENT_ID, DECISION_ID);
        verify(waitingListRepository).removeFromWaitingList(EVENT_ID, ENTRY_ID);

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userRepository).updateUser(profileCaptor.capture());
        List<String> updatedEvents = profileCaptor.getValue().getRegisteredEventIds();
        assertNotNull(updatedEvents);
        assertEquals(1, updatedEvents.size());
        assertEquals("other", updatedEvents.get(0));
    }

    // Geolocation Tests

    @Test
    public void joinWaitingList_geolocationRequired_sessionLocationProvided_savesLocation() throws Exception {
        // Arrange - Event requires geolocation
        GeoPoint sessionLocation = new GeoPoint(53.5461, -113.4938);
        Event testEvent = new Event();
        testEvent.setGeolocationRequired(true);

        DocumentSnapshot eventSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(eventSnapshot.exists()).thenReturn(true);
        when(eventSnapshot.toObject(Event.class)).thenReturn(testEvent);
        when(eventRepository.getEventById(EVENT_ID))
                .thenReturn(Tasks.forResult(eventSnapshot));

        QuerySnapshot emptySnapshot = mockQuerySnapshot(true);
        when(waitingListRepository.checkUserInWaitingList(EVENT_ID, ENTRANT_ID))
                .thenReturn(Tasks.forResult(emptySnapshot));

        DocumentReference entryRef = mockDocumentReference(ENTRY_ID);
        when(waitingListRepository.addToWaitingList(anyString(), any(WaitingListEntry.class)))
                .thenReturn(Tasks.forResult(entryRef));

        DocumentReference decisionRef = mockDocumentReference(DECISION_ID);
        when(decisionRepository.createDecision(anyString(), any()))
                .thenReturn(Tasks.forResult(decisionRef));

        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(ENTRANT_ID);
        userProfile.setRegisteredEventIds(new ArrayList<>());

        DocumentSnapshot userSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(userSnapshot.toObject(UserProfile.class)).thenReturn(userProfile);
        when(userRepository.getUserById(ENTRANT_ID)).thenReturn(Tasks.forResult(userSnapshot));
        when(userRepository.updateUser(any(UserProfile.class))).thenReturn(Tasks.forResult(null));

        // Act - Join with session location
        com.google.android.gms.tasks.Task<Map<String, String>> task = subject.joinWaitingList(EVENT_ID, ENTRANT_ID, sessionLocation);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        assertTrue(task.isSuccessful());
        ArgumentCaptor<WaitingListEntry> entryCaptor = ArgumentCaptor.forClass(WaitingListEntry.class);
        verify(waitingListRepository).addToWaitingList(org.mockito.Mockito.eq(EVENT_ID), entryCaptor.capture());

        WaitingListEntry entry = entryCaptor.getValue();
        assertNotNull("Location should be saved when geolocation required and session location provided", entry.getJoinLocation());
        assertEquals("Latitude should match", 53.5461, entry.getJoinLocation().getLatitude(), 0.0001);
        assertEquals("Longitude should match", -113.4938, entry.getJoinLocation().getLongitude(), 0.0001);
    }

    @Test
    public void joinWaitingList_geolocationRequired_noSessionLocation_savesNullLocation() throws Exception {
        // Arrange - Event requires geolocation but no session location provided
        Event testEvent = new Event();
        testEvent.setGeolocationRequired(true);

        DocumentSnapshot eventSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(eventSnapshot.exists()).thenReturn(true);
        when(eventSnapshot.toObject(Event.class)).thenReturn(testEvent);
        when(eventRepository.getEventById(EVENT_ID))
                .thenReturn(Tasks.forResult(eventSnapshot));

        QuerySnapshot emptySnapshot = mockQuerySnapshot(true);
        when(waitingListRepository.checkUserInWaitingList(EVENT_ID, ENTRANT_ID))
                .thenReturn(Tasks.forResult(emptySnapshot));

        DocumentReference entryRef = mockDocumentReference(ENTRY_ID);
        when(waitingListRepository.addToWaitingList(anyString(), any(WaitingListEntry.class)))
                .thenReturn(Tasks.forResult(entryRef));

        DocumentReference decisionRef = mockDocumentReference(DECISION_ID);
        when(decisionRepository.createDecision(anyString(), any()))
                .thenReturn(Tasks.forResult(decisionRef));

        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(ENTRANT_ID);
        userProfile.setRegisteredEventIds(new ArrayList<>());

        DocumentSnapshot userSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(userSnapshot.toObject(UserProfile.class)).thenReturn(userProfile);
        when(userRepository.getUserById(ENTRANT_ID)).thenReturn(Tasks.forResult(userSnapshot));
        when(userRepository.updateUser(any(UserProfile.class))).thenReturn(Tasks.forResult(null));

        // Act - Join without session location (null)
        com.google.android.gms.tasks.Task<Map<String, String>> task = subject.joinWaitingList(EVENT_ID, ENTRANT_ID, null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert - Should still succeed but location is null (graceful degradation)
        assertTrue(task.isSuccessful());
        ArgumentCaptor<WaitingListEntry> entryCaptor = ArgumentCaptor.forClass(WaitingListEntry.class);
        verify(waitingListRepository).addToWaitingList(org.mockito.Mockito.eq(EVENT_ID), entryCaptor.capture());

        WaitingListEntry entry = entryCaptor.getValue();
        assertNull("Location should be null when not provided (graceful degradation)", entry.getJoinLocation());
    }

    @Test
    public void joinWaitingList_geolocationNotRequired_sessionLocationProvided_ignoresLocation() throws Exception {
        // Arrange - Event does NOT require geolocation
        GeoPoint sessionLocation = new GeoPoint(53.5461, -113.4938);
        Event testEvent = new Event();
        testEvent.setGeolocationRequired(false);

        DocumentSnapshot eventSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(eventSnapshot.exists()).thenReturn(true);
        when(eventSnapshot.toObject(Event.class)).thenReturn(testEvent);
        when(eventRepository.getEventById(EVENT_ID))
                .thenReturn(Tasks.forResult(eventSnapshot));

        QuerySnapshot emptySnapshot = mockQuerySnapshot(true);
        when(waitingListRepository.checkUserInWaitingList(EVENT_ID, ENTRANT_ID))
                .thenReturn(Tasks.forResult(emptySnapshot));

        DocumentReference entryRef = mockDocumentReference(ENTRY_ID);
        when(waitingListRepository.addToWaitingList(anyString(), any(WaitingListEntry.class)))
                .thenReturn(Tasks.forResult(entryRef));

        DocumentReference decisionRef = mockDocumentReference(DECISION_ID);
        when(decisionRepository.createDecision(anyString(), any()))
                .thenReturn(Tasks.forResult(decisionRef));

        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(ENTRANT_ID);
        userProfile.setRegisteredEventIds(new ArrayList<>());

        DocumentSnapshot userSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(userSnapshot.toObject(UserProfile.class)).thenReturn(userProfile);
        when(userRepository.getUserById(ENTRANT_ID)).thenReturn(Tasks.forResult(userSnapshot));
        when(userRepository.updateUser(any(UserProfile.class))).thenReturn(Tasks.forResult(null));

        // Act - Join with session location but event doesn't require it
        com.google.android.gms.tasks.Task<Map<String, String>> task = subject.joinWaitingList(EVENT_ID, ENTRANT_ID, sessionLocation);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert - Location should be ignored (null)
        assertTrue(task.isSuccessful());
        ArgumentCaptor<WaitingListEntry> entryCaptor = ArgumentCaptor.forClass(WaitingListEntry.class);
        verify(waitingListRepository).addToWaitingList(org.mockito.Mockito.eq(EVENT_ID), entryCaptor.capture());

        WaitingListEntry entry = entryCaptor.getValue();
        assertNull("Location should be ignored when geolocation not required", entry.getJoinLocation());
    }

    @Test
    public void joinWaitingList_geolocationNotRequired_noSessionLocation_savesNullLocation() throws Exception {
        // Arrange - Event does NOT require geolocation and no location provided
        Event testEvent = new Event();
        testEvent.setGeolocationRequired(false);

        DocumentSnapshot eventSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(eventSnapshot.exists()).thenReturn(true);
        when(eventSnapshot.toObject(Event.class)).thenReturn(testEvent);
        when(eventRepository.getEventById(EVENT_ID))
                .thenReturn(Tasks.forResult(eventSnapshot));

        QuerySnapshot emptySnapshot = mockQuerySnapshot(true);
        when(waitingListRepository.checkUserInWaitingList(EVENT_ID, ENTRANT_ID))
                .thenReturn(Tasks.forResult(emptySnapshot));

        DocumentReference entryRef = mockDocumentReference(ENTRY_ID);
        when(waitingListRepository.addToWaitingList(anyString(), any(WaitingListEntry.class)))
                .thenReturn(Tasks.forResult(entryRef));

        DocumentReference decisionRef = mockDocumentReference(DECISION_ID);
        when(decisionRepository.createDecision(anyString(), any()))
                .thenReturn(Tasks.forResult(decisionRef));

        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(ENTRANT_ID);
        userProfile.setRegisteredEventIds(new ArrayList<>());

        DocumentSnapshot userSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(userSnapshot.toObject(UserProfile.class)).thenReturn(userProfile);
        when(userRepository.getUserById(ENTRANT_ID)).thenReturn(Tasks.forResult(userSnapshot));
        when(userRepository.updateUser(any(UserProfile.class))).thenReturn(Tasks.forResult(null));

        // Act
        com.google.android.gms.tasks.Task<Map<String, String>> task = subject.joinWaitingList(EVENT_ID, ENTRANT_ID, null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        assertTrue(task.isSuccessful());
        ArgumentCaptor<WaitingListEntry> entryCaptor = ArgumentCaptor.forClass(WaitingListEntry.class);
        verify(waitingListRepository).addToWaitingList(org.mockito.Mockito.eq(EVENT_ID), entryCaptor.capture());

        WaitingListEntry entry = entryCaptor.getValue();
        assertNull("Location should be null when not required and not provided", entry.getJoinLocation());
    }

    @Test
    public void joinWaitingList_geolocationRequiredNull_defaultsToFalse() throws Exception {
        // Arrange - Event has geolocationRequired = null (legacy event)
        GeoPoint sessionLocation = new GeoPoint(53.5461, -113.4938);
        Event testEvent = new Event();
        testEvent.setGeolocationRequired(null); // Null should be treated as false

        DocumentSnapshot eventSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(eventSnapshot.exists()).thenReturn(true);
        when(eventSnapshot.toObject(Event.class)).thenReturn(testEvent);
        when(eventRepository.getEventById(EVENT_ID))
                .thenReturn(Tasks.forResult(eventSnapshot));

        QuerySnapshot emptySnapshot = mockQuerySnapshot(true);
        when(waitingListRepository.checkUserInWaitingList(EVENT_ID, ENTRANT_ID))
                .thenReturn(Tasks.forResult(emptySnapshot));

        DocumentReference entryRef = mockDocumentReference(ENTRY_ID);
        when(waitingListRepository.addToWaitingList(anyString(), any(WaitingListEntry.class)))
                .thenReturn(Tasks.forResult(entryRef));

        DocumentReference decisionRef = mockDocumentReference(DECISION_ID);
        when(decisionRepository.createDecision(anyString(), any()))
                .thenReturn(Tasks.forResult(decisionRef));

        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(ENTRANT_ID);
        userProfile.setRegisteredEventIds(new ArrayList<>());

        DocumentSnapshot userSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(userSnapshot.toObject(UserProfile.class)).thenReturn(userProfile);
        when(userRepository.getUserById(ENTRANT_ID)).thenReturn(Tasks.forResult(userSnapshot));
        when(userRepository.updateUser(any(UserProfile.class))).thenReturn(Tasks.forResult(null));

        // Act - Join with session location but geolocationRequired is null
        com.google.android.gms.tasks.Task<Map<String, String>> task = subject.joinWaitingList(EVENT_ID, ENTRANT_ID, sessionLocation);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert - Null should be treated as false, location should not be saved
        assertTrue(task.isSuccessful());
        ArgumentCaptor<WaitingListEntry> entryCaptor = ArgumentCaptor.forClass(WaitingListEntry.class);
        verify(waitingListRepository).addToWaitingList(org.mockito.Mockito.eq(EVENT_ID), entryCaptor.capture());

        WaitingListEntry entry = entryCaptor.getValue();
        assertNull("Location should be null when geolocationRequired is null (treated as false)", entry.getJoinLocation());
    }

    @Test
    public void joinWaitingList_geolocationRequired_multipleUsers_savesDifferentLocations() throws Exception {
        // Arrange - Event requires geolocation
        Event testEvent = new Event();
        testEvent.setGeolocationRequired(true);

        DocumentSnapshot eventSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(eventSnapshot.exists()).thenReturn(true);
        when(eventSnapshot.toObject(Event.class)).thenReturn(testEvent);
        when(eventRepository.getEventById(EVENT_ID))
                .thenReturn(Tasks.forResult(eventSnapshot));

        QuerySnapshot emptySnapshot = mockQuerySnapshot(true);
        when(waitingListRepository.checkUserInWaitingList(anyString(), anyString()))
                .thenReturn(Tasks.forResult(emptySnapshot));

        DocumentReference entryRef1 = mockDocumentReference("entry-1");
        DocumentReference entryRef2 = mockDocumentReference("entry-2");
        when(waitingListRepository.addToWaitingList(anyString(), any(WaitingListEntry.class)))
                .thenReturn(Tasks.forResult(entryRef1))
                .thenReturn(Tasks.forResult(entryRef2));

        DocumentReference decisionRef = mockDocumentReference(DECISION_ID);
        when(decisionRepository.createDecision(anyString(), any()))
                .thenReturn(Tasks.forResult(decisionRef));

        // User A profile
        UserProfile userProfileA = new UserProfile();
        userProfileA.setUserId("user-A");
        userProfileA.setRegisteredEventIds(new ArrayList<>());

        DocumentSnapshot userSnapshotA = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(userSnapshotA.toObject(UserProfile.class)).thenReturn(userProfileA);

        // User B profile
        UserProfile userProfileB = new UserProfile();
        userProfileB.setUserId("user-B");
        userProfileB.setRegisteredEventIds(new ArrayList<>());

        DocumentSnapshot userSnapshotB = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(userSnapshotB.toObject(UserProfile.class)).thenReturn(userProfileB);

        when(userRepository.getUserById("user-A")).thenReturn(Tasks.forResult(userSnapshotA));
        when(userRepository.getUserById("user-B")).thenReturn(Tasks.forResult(userSnapshotB));
        when(userRepository.updateUser(any(UserProfile.class))).thenReturn(Tasks.forResult(null));

        // Act - User A joins from location A
        GeoPoint locationA = new GeoPoint(10.0, 20.0);
        com.google.android.gms.tasks.Task<Map<String, String>> taskA = subject.joinWaitingList(EVENT_ID, "user-A", locationA);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // User B joins from location B
        GeoPoint locationB = new GeoPoint(30.0, 40.0);
        com.google.android.gms.tasks.Task<Map<String, String>> taskB = subject.joinWaitingList(EVENT_ID, "user-B", locationB);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert - Verify both tasks succeeded
        assertTrue(taskA.isSuccessful());
        assertTrue(taskB.isSuccessful());

        // Capture both waiting list entries
        ArgumentCaptor<WaitingListEntry> entryCaptor = ArgumentCaptor.forClass(WaitingListEntry.class);
        verify(waitingListRepository, org.mockito.Mockito.times(2))
                .addToWaitingList(org.mockito.Mockito.eq(EVENT_ID), entryCaptor.capture());

        List<WaitingListEntry> entries = entryCaptor.getAllValues();
        assertEquals("Should have captured 2 entries", 2, entries.size());

        // Verify first user's location
        WaitingListEntry entryA = entries.get(0);
        assertNotNull("User A should have location", entryA.getJoinLocation());
        assertEquals("User A latitude", 10.0, entryA.getJoinLocation().getLatitude(), 0.0001);
        assertEquals("User A longitude", 20.0, entryA.getJoinLocation().getLongitude(), 0.0001);

        // Verify second user's location
        WaitingListEntry entryB = entries.get(1);
        assertNotNull("User B should have location", entryB.getJoinLocation());
        assertEquals("User B latitude", 30.0, entryB.getJoinLocation().getLatitude(), 0.0001);
        assertEquals("User B longitude", 40.0, entryB.getJoinLocation().getLongitude(), 0.0001);
    }

    private static QuerySnapshot mockQuerySnapshot(boolean empty) {
        QuerySnapshot snapshot = org.mockito.Mockito.mock(QuerySnapshot.class);
        when(snapshot.isEmpty()).thenReturn(empty);
        return snapshot;
    }

    private static DocumentReference mockDocumentReference(String id) {
        DocumentReference reference = org.mockito.Mockito.mock(DocumentReference.class);
        when(reference.getId()).thenReturn(id);
        return reference;
    }
}

