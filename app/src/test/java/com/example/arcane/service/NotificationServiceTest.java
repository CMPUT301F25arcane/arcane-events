package com.example.arcane.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Looper;

import com.example.arcane.model.Decision;
import com.example.arcane.model.Notification;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.NotificationRepository;
import com.example.arcane.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link NotificationService}.
 *
 * Tests notification sending logic including user opt-out preferences,
 * batch notification sending, and basic CRUD operations.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class NotificationServiceTest {

    private static final String USER_ID = "user-123";
    private static final String EVENT_ID = "event-456";
    private static final String NOTIFICATION_ID = "notif-789";
    private static final String NOTIFICATION_TYPE = "INVITED";
    private static final String NOTIFICATION_TITLE = "You're Invited!";
    private static final String NOTIFICATION_MESSAGE = "You've been selected for the event";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DecisionRepository decisionRepository;

    @Mock
    private DocumentReference mockDocumentReference;

    @Mock
    private DocumentSnapshot mockUserSnapshot;

    @Mock
    private QuerySnapshot mockDecisionsSnapshot;

    private NotificationService subject;

    @Before
    public void setUp() {
        subject = new NotificationService(notificationRepository, userRepository, decisionRepository);
    }

    /**
     * Test that sendNotification creates a notification when user has NOT opted out.
     */
    @Test
    public void sendNotification_createsNotificationWhenUserOptedIn() throws Exception {
        // Mock user who has NOT opted out (notificationOptOut = false or null)
        when(mockUserSnapshot.exists()).thenReturn(true);
        when(mockUserSnapshot.getBoolean("notificationOptOut")).thenReturn(false);
        when(userRepository.getUserById(USER_ID)).thenReturn(Tasks.forResult(mockUserSnapshot));

        // Mock notification creation
        when(mockDocumentReference.getId()).thenReturn(NOTIFICATION_ID);
        when(notificationRepository.createNotification(eq(USER_ID), any(Notification.class)))
                .thenReturn(Tasks.forResult(mockDocumentReference));

        Task<DocumentReference> task = subject.sendNotification(
                USER_ID, EVENT_ID, NOTIFICATION_TYPE, NOTIFICATION_TITLE, NOTIFICATION_MESSAGE);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());
        assertNotNull(task.getResult());
        assertEquals(NOTIFICATION_ID, task.getResult().getId());

        // Verify notification was created with correct data
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).createNotification(eq(USER_ID), notificationCaptor.capture());

        Notification capturedNotification = notificationCaptor.getValue();
        assertEquals(USER_ID, capturedNotification.getUserId());
        assertEquals(EVENT_ID, capturedNotification.getEventId());
        assertEquals(NOTIFICATION_TYPE, capturedNotification.getType());
        assertEquals(NOTIFICATION_TITLE, capturedNotification.getTitle());
        assertEquals(NOTIFICATION_MESSAGE, capturedNotification.getMessage());
        assertEquals(false, capturedNotification.getRead());
        assertNotNull(capturedNotification.getTimestamp());
    }

    /**
     * Test that sendNotification returns null when user has opted out.
     */
    @Test
    public void sendNotification_returnsNullWhenUserOptedOut() throws Exception {
        // Mock user who HAS opted out (notificationOptOut = true)
        when(mockUserSnapshot.exists()).thenReturn(true);
        when(mockUserSnapshot.getBoolean("notificationOptOut")).thenReturn(true);
        when(userRepository.getUserById(USER_ID)).thenReturn(Tasks.forResult(mockUserSnapshot));

        Task<DocumentReference> task = subject.sendNotification(
                USER_ID, EVENT_ID, NOTIFICATION_TYPE, NOTIFICATION_TITLE, NOTIFICATION_MESSAGE);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());
        assertNull("Should return null when user opted out", task.getResult());

        // Verify notification was NOT created
        verify(notificationRepository, never()).createNotification(anyString(), any(Notification.class));
    }

    /**
     * Test that sendNotification returns null when user does not exist.
     */
    @Test
    public void sendNotification_returnsNullWhenUserNotFound() throws Exception {
        // Mock user not found
        when(mockUserSnapshot.exists()).thenReturn(false);
        when(userRepository.getUserById(USER_ID)).thenReturn(Tasks.forResult(mockUserSnapshot));

        Task<DocumentReference> task = subject.sendNotification(
                USER_ID, EVENT_ID, NOTIFICATION_TYPE, NOTIFICATION_TITLE, NOTIFICATION_MESSAGE);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());
        assertNull("Should return null when user not found", task.getResult());

        // Verify notification was NOT created
        verify(notificationRepository, never()).createNotification(anyString(), any(Notification.class));
    }

    /**
     * Test that sendNotification creates notification when user has null opt-out preference.
     */
    @Test
    public void sendNotification_createsNotificationWhenOptOutIsNull() throws Exception {
        // Mock user with null notificationOptOut (should treat as opted in)
        when(mockUserSnapshot.exists()).thenReturn(true);
        when(mockUserSnapshot.getBoolean("notificationOptOut")).thenReturn(null);
        when(userRepository.getUserById(USER_ID)).thenReturn(Tasks.forResult(mockUserSnapshot));

        // Mock notification creation
        when(mockDocumentReference.getId()).thenReturn(NOTIFICATION_ID);
        when(notificationRepository.createNotification(eq(USER_ID), any(Notification.class)))
                .thenReturn(Tasks.forResult(mockDocumentReference));

        Task<DocumentReference> task = subject.sendNotification(
                USER_ID, EVENT_ID, NOTIFICATION_TYPE, NOTIFICATION_TITLE, NOTIFICATION_MESSAGE);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());
        assertNotNull("Should create notification when optOut is null", task.getResult());

        // Verify notification was created
        verify(notificationRepository).createNotification(eq(USER_ID), any(Notification.class));
    }

    /**
     * Test sendNotificationsToEntrantsByStatus with multiple entrants (all opted in).
     */
    @Test
    public void sendNotificationsToEntrantsByStatus_sendsToAllEntrants() throws Exception {
        String status = "INVITED";

        // Mock 3 decisions with different entrants
        List<QueryDocumentSnapshot> decisionDocs = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            QueryDocumentSnapshot doc = org.mockito.Mockito.mock(QueryDocumentSnapshot.class);
            when(doc.getString("entrantId")).thenReturn("user-" + i);
            decisionDocs.add(doc);
        }

        when(mockDecisionsSnapshot.isEmpty()).thenReturn(false);
        when(mockDecisionsSnapshot.iterator()).thenReturn(decisionDocs.iterator());
        when(decisionRepository.getDecisionsByStatus(EVENT_ID, status))
                .thenReturn(Tasks.forResult(mockDecisionsSnapshot));

        // Mock all users opted in
        for (int i = 1; i <= 3; i++) {
            DocumentSnapshot userSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
            when(userSnapshot.exists()).thenReturn(true);
            when(userSnapshot.getBoolean("notificationOptOut")).thenReturn(false);
            when(userRepository.getUserById("user-" + i)).thenReturn(Tasks.forResult(userSnapshot));

            DocumentReference docRef = org.mockito.Mockito.mock(DocumentReference.class);
            when(docRef.getId()).thenReturn("notif-" + i);
            when(notificationRepository.createNotification(eq("user-" + i), any(Notification.class)))
                    .thenReturn(Tasks.forResult(docRef));
        }

        Task<Map<String, Object>> task = subject.sendNotificationsToEntrantsByStatus(
                EVENT_ID, status, NOTIFICATION_TITLE, NOTIFICATION_MESSAGE);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());

        Map<String, Object> result = task.getResult();
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals(3, result.get("count"));
        assertTrue(((String) result.get("message")).contains("3"));

        // Verify notifications were created for all 3 users
        verify(notificationRepository, times(3)).createNotification(anyString(), any(Notification.class));
    }

    /**
     * Test sendNotificationsToEntrantsByStatus with mixed opt-out preferences.
     */
    @Test
    public void sendNotificationsToEntrantsByStatus_respectsOptOutPreferences() throws Exception {
        String status = "LOST";

        // Mock 3 decisions
        List<QueryDocumentSnapshot> decisionDocs = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            QueryDocumentSnapshot doc = org.mockito.Mockito.mock(QueryDocumentSnapshot.class);
            when(doc.getString("entrantId")).thenReturn("user-" + i);
            decisionDocs.add(doc);
        }

        when(mockDecisionsSnapshot.isEmpty()).thenReturn(false);
        when(mockDecisionsSnapshot.iterator()).thenReturn(decisionDocs.iterator());
        when(decisionRepository.getDecisionsByStatus(EVENT_ID, status))
                .thenReturn(Tasks.forResult(mockDecisionsSnapshot));

        // Mock user-1 opted in, user-2 opted out, user-3 opted in
        for (int i = 1; i <= 3; i++) {
            DocumentSnapshot userSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
            when(userSnapshot.exists()).thenReturn(true);
            boolean optedOut = (i == 2); // user-2 opted out
            when(userSnapshot.getBoolean("notificationOptOut")).thenReturn(optedOut);
            when(userRepository.getUserById("user-" + i)).thenReturn(Tasks.forResult(userSnapshot));

            if (!optedOut) {
                DocumentReference docRef = org.mockito.Mockito.mock(DocumentReference.class);
                when(docRef.getId()).thenReturn("notif-" + i);
                when(notificationRepository.createNotification(eq("user-" + i), any(Notification.class)))
                        .thenReturn(Tasks.forResult(docRef));
            }
        }

        Task<Map<String, Object>> task = subject.sendNotificationsToEntrantsByStatus(
                EVENT_ID, status, NOTIFICATION_TITLE, NOTIFICATION_MESSAGE);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());

        Map<String, Object> result = task.getResult();
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals(2, result.get("count")); // Only 2 notifications sent (user-2 opted out)

        // Verify notifications were created only for opted-in users
        verify(notificationRepository, times(2)).createNotification(anyString(), any(Notification.class));
    }

    /**
     * Test sendNotificationsToEntrantsByStatus with empty decision list.
     */
    @Test
    public void sendNotificationsToEntrantsByStatus_handlesEmptyDecisionList() throws Exception {
        String status = "INVITED";

        // Mock empty decisions snapshot
        when(mockDecisionsSnapshot.isEmpty()).thenReturn(true);
        when(decisionRepository.getDecisionsByStatus(EVENT_ID, status))
                .thenReturn(Tasks.forResult(mockDecisionsSnapshot));

        Task<Map<String, Object>> task = subject.sendNotificationsToEntrantsByStatus(
                EVENT_ID, status, NOTIFICATION_TITLE, NOTIFICATION_MESSAGE);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());

        Map<String, Object> result = task.getResult();
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals(0, result.get("count"));
        assertTrue(((String) result.get("message")).contains("No entrants found"));

        // Verify no notifications were created
        verify(notificationRepository, never()).createNotification(anyString(), any(Notification.class));
    }

    /**
     * Test sendNotificationsToEntrantsByStatus handles decision query failure.
     */
    @Test
    public void sendNotificationsToEntrantsByStatus_handlesDecisionQueryFailure() throws Exception {
        String status = "INVITED";

        // Mock failed decision query
        Exception testException = new Exception("Firestore query failed");
        when(decisionRepository.getDecisionsByStatus(EVENT_ID, status))
                .thenReturn(Tasks.forException(testException));

        Task<Map<String, Object>> task = subject.sendNotificationsToEntrantsByStatus(
                EVENT_ID, status, NOTIFICATION_TITLE, NOTIFICATION_MESSAGE);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());

        Map<String, Object> result = task.getResult();
        assertNotNull(result);
        assertEquals("error", result.get("status"));
        assertEquals(0, result.get("count"));
        assertEquals("Failed to get decisions", result.get("message"));

        // Verify no notifications were created
        verify(notificationRepository, never()).createNotification(anyString(), any(Notification.class));
    }

    /**
     * Test sendNotificationsToEntrantsByStatus with null entrantId in decision.
     */
    @Test
    public void sendNotificationsToEntrantsByStatus_skipsNullEntrantIds() throws Exception {
        String status = "INVITED";

        // Mock 2 decisions: one with null entrantId, one with valid entrantId
        QueryDocumentSnapshot doc1 = org.mockito.Mockito.mock(QueryDocumentSnapshot.class);
        when(doc1.getString("entrantId")).thenReturn(null);

        QueryDocumentSnapshot doc2 = org.mockito.Mockito.mock(QueryDocumentSnapshot.class);
        when(doc2.getString("entrantId")).thenReturn("user-valid");

        List<QueryDocumentSnapshot> decisionDocs = Arrays.asList(doc1, doc2);

        when(mockDecisionsSnapshot.isEmpty()).thenReturn(false);
        when(mockDecisionsSnapshot.iterator()).thenReturn(decisionDocs.iterator());
        when(decisionRepository.getDecisionsByStatus(EVENT_ID, status))
                .thenReturn(Tasks.forResult(mockDecisionsSnapshot));

        // Mock valid user opted in
        DocumentSnapshot userSnapshot = org.mockito.Mockito.mock(DocumentSnapshot.class);
        when(userSnapshot.exists()).thenReturn(true);
        when(userSnapshot.getBoolean("notificationOptOut")).thenReturn(false);
        when(userRepository.getUserById("user-valid")).thenReturn(Tasks.forResult(userSnapshot));

        DocumentReference docRef = org.mockito.Mockito.mock(DocumentReference.class);
        when(docRef.getId()).thenReturn("notif-1");
        when(notificationRepository.createNotification(eq("user-valid"), any(Notification.class)))
                .thenReturn(Tasks.forResult(docRef));

        Task<Map<String, Object>> task = subject.sendNotificationsToEntrantsByStatus(
                EVENT_ID, status, NOTIFICATION_TITLE, NOTIFICATION_MESSAGE);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());

        Map<String, Object> result = task.getResult();
        assertNotNull(result);
        assertEquals("success", result.get("status"));
        assertEquals(1, result.get("count")); // Only 1 notification sent (null entrantId skipped)

        // Verify only 1 notification was created
        verify(notificationRepository, times(1)).createNotification(eq("user-valid"), any(Notification.class));
    }

    /**
     * Test getUserNotifications delegates to repository.
     */
    @Test
    public void getUserNotifications_delegatesToRepository() throws Exception {
        when(notificationRepository.getNotificationsForUser(USER_ID))
                .thenReturn(Tasks.forResult(mockDecisionsSnapshot));

        Task<QuerySnapshot> task = subject.getUserNotifications(USER_ID);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());
        assertEquals(mockDecisionsSnapshot, task.getResult());

        verify(notificationRepository).getNotificationsForUser(USER_ID);
    }

    /**
     * Test getUnreadNotifications delegates to repository.
     */
    @Test
    public void getUnreadNotifications_delegatesToRepository() throws Exception {
        when(notificationRepository.getUnreadNotificationsForUser(USER_ID))
                .thenReturn(Tasks.forResult(mockDecisionsSnapshot));

        Task<QuerySnapshot> task = subject.getUnreadNotifications(USER_ID);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());
        assertEquals(mockDecisionsSnapshot, task.getResult());

        verify(notificationRepository).getUnreadNotificationsForUser(USER_ID);
    }

    /**
     * Test markNotificationRead delegates to repository.
     */
    @Test
    public void markNotificationRead_delegatesToRepository() throws Exception {
        when(notificationRepository.markAsRead(USER_ID, NOTIFICATION_ID))
                .thenReturn(Tasks.forResult(null));

        Task<Void> task = subject.markNotificationRead(USER_ID, NOTIFICATION_ID);
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        assertTrue(task.isComplete());
        assertTrue(task.isSuccessful());

        verify(notificationRepository).markAsRead(USER_ID, NOTIFICATION_ID);
    }
}