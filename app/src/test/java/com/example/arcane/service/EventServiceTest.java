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

import com.example.arcane.model.UserProfile;
import com.example.arcane.model.WaitingListEntry;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.repository.WaitingListRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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

        com.google.android.gms.tasks.Task<Map<String, String>> task = subject.joinWaitingList(EVENT_ID, ENTRANT_ID);
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

        com.google.android.gms.tasks.Task<Map<String, String>> task = subject.joinWaitingList(EVENT_ID, ENTRANT_ID);
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

