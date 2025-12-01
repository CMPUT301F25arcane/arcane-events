package com.example.arcane.repository;

import com.example.arcane.model.Notification;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import android.os.Looper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NotificationRepository}.
 *
 * Tests CRUD operations and complex queries for notifications
 * stored in Firestore subcollections under user documents.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class NotificationRepositoryTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final String USER_ID = "user-123";
    private static final String NOTIFICATION_ID = "notif-456";
    private static final String EVENT_ID = "event-789";

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private CollectionReference mockUsersCollectionRef;

    @Mock
    private CollectionReference mockNotificationsCollectionRef;

    @Mock
    private DocumentReference mockUserDocRef;

    @Mock
    private DocumentReference mockNotificationDocRef;

    @Mock
    private DocumentSnapshot mockDocumentSnapshot;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private Query mockQuery;

    private NotificationRepository repository;
    private Notification testNotification;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create test notification
        testNotification = new Notification(
                NOTIFICATION_ID,
                USER_ID,
                EVENT_ID,
                "INVITED",
                "You're Invited!",
                "You've been selected for the event",
                new Timestamp(new Date()),
                false
        );

        // Setup mock chain for Firestore
        when(mockDb.collection("users")).thenReturn(mockUsersCollectionRef);
        when(mockUsersCollectionRef.document(anyString())).thenReturn(mockUserDocRef);
        when(mockUserDocRef.collection("notifications")).thenReturn(mockNotificationsCollectionRef);
        when(mockNotificationsCollectionRef.document(anyString())).thenReturn(mockNotificationDocRef);

        // Create NotificationRepository with mocked FirebaseFirestore (IoC)
        repository = new NotificationRepository(mockDb);
    }

    /**
     * Test createNotification adds notification to user's subcollection.
     */
    @Test
    public void createNotification_addsNotificationSuccessfully() {
        // Arrange
        Task<DocumentReference> mockTask = Tasks.forResult(mockNotificationDocRef);
        when(mockNotificationsCollectionRef.add(any(Notification.class))).thenReturn(mockTask);

        // Act
        Task<DocumentReference> result = repository.createNotification(USER_ID, testNotification);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain document reference", mockNotificationDocRef, result.getResult());

        // Verify correct Firestore path was used
        verify(mockDb).collection("users");
        verify(mockUsersCollectionRef).document(USER_ID);
        verify(mockUserDocRef).collection("notifications");
        verify(mockNotificationsCollectionRef).add(testNotification);
    }

    /**
     * Test getNotificationsForUser retrieves notifications ordered by timestamp.
     */
    @Test
    public void getNotificationsForUser_retrievesNotificationsOrderedByTimestamp() {
        // Arrange
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockNotificationsCollectionRef.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act
        Task<QuerySnapshot> result = repository.getNotificationsForUser(USER_ID);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());

        // Verify correct query was executed
        verify(mockDb).collection("users");
        verify(mockUsersCollectionRef).document(USER_ID);
        verify(mockUserDocRef).collection("notifications");
        verify(mockNotificationsCollectionRef).orderBy("timestamp", Query.Direction.DESCENDING);
        verify(mockQuery).get();
    }

    /**
     * Test getUnreadNotificationsForUser retrieves all notifications (filtering done by caller).
     */
    @Test
    public void getUnreadNotificationsForUser_retrievesAllNotifications() {
        // Arrange
        Task<QuerySnapshot> mockTask = Tasks.forResult(mockQuerySnapshot);
        when(mockNotificationsCollectionRef.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(mockQuery);
        when(mockQuery.get()).thenReturn(mockTask);

        // Act
        Task<QuerySnapshot> result = repository.getUnreadNotificationsForUser(USER_ID);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain query snapshot", mockQuerySnapshot, result.getResult());

        // Verify same query as getNotificationsForUser (no whereEqualTo filter)
        verify(mockNotificationsCollectionRef).orderBy("timestamp", Query.Direction.DESCENDING);
        verify(mockQuery).get();
    }

    /**
     * Test markAsRead updates the read field to true.
     */
    @Test
    public void markAsRead_updatesReadFieldToTrue() {
        // Arrange
        Task<Void> mockTask = Tasks.forResult(null);
        when(mockNotificationDocRef.update("read", true)).thenReturn(mockTask);

        // Act
        Task<Void> result = repository.markAsRead(USER_ID, NOTIFICATION_ID);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());

        // Verify correct update operation
        verify(mockDb).collection("users");
        verify(mockUsersCollectionRef).document(USER_ID);
        verify(mockUserDocRef).collection("notifications");
        verify(mockNotificationsCollectionRef).document(NOTIFICATION_ID);
        verify(mockNotificationDocRef).update("read", true);
    }

    /**
     * Test getNotificationById retrieves specific notification.
     */
    @Test
    public void getNotificationById_retrievesSpecificNotification() {
        // Arrange
        Task<DocumentSnapshot> mockTask = Tasks.forResult(mockDocumentSnapshot);
        when(mockNotificationDocRef.get()).thenReturn(mockTask);

        // Act
        Task<DocumentSnapshot> result = repository.getNotificationById(USER_ID, NOTIFICATION_ID);

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());
        assertEquals("Result should contain document snapshot", mockDocumentSnapshot, result.getResult());

        // Verify correct path
        verify(mockDb).collection("users");
        verify(mockUsersCollectionRef).document(USER_ID);
        verify(mockUserDocRef).collection("notifications");
        verify(mockNotificationsCollectionRef).document(NOTIFICATION_ID);
        verify(mockNotificationDocRef).get();
    }

    /**
     * Test getAllNotificationsWithEmails with multiple users and notifications.
     */
    @Test
    public void getAllNotificationsWithEmails_aggregatesNotificationsFromAllUsers() {
        // Arrange - Mock 2 users
        QueryDocumentSnapshot userDoc1 = mock(QueryDocumentSnapshot.class);
        when(userDoc1.getId()).thenReturn("user-1");
        when(userDoc1.getString("email")).thenReturn("user1@example.com");

        QueryDocumentSnapshot userDoc2 = mock(QueryDocumentSnapshot.class);
        when(userDoc2.getId()).thenReturn("user-2");
        when(userDoc2.getString("email")).thenReturn("user2@example.com");

        List<QueryDocumentSnapshot> userDocs = new ArrayList<>();
        userDocs.add(userDoc1);
        userDocs.add(userDoc2);

        QuerySnapshot usersSnapshot = mock(QuerySnapshot.class);
        when(usersSnapshot.iterator()).thenReturn(userDocs.iterator());

        Task<QuerySnapshot> usersTask = Tasks.forResult(usersSnapshot);
        when(mockUsersCollectionRef.get()).thenReturn(usersTask);

        // Mock notifications for user-1
        QueryDocumentSnapshot notif1 = mock(QueryDocumentSnapshot.class);
        when(notif1.getId()).thenReturn("notif-1");
        when(notif1.toObject(Notification.class)).thenReturn(new Notification(
                "notif-1", "user-1", "event-1", "INVITED", "Title1", "Message1",
                new Timestamp(new Date(System.currentTimeMillis() - 1000)), false
        ));

        List<QueryDocumentSnapshot> notifDocs1 = new ArrayList<>();
        notifDocs1.add(notif1);

        QuerySnapshot notifSnapshot1 = mock(QuerySnapshot.class);
        when(notifSnapshot1.iterator()).thenReturn(notifDocs1.iterator());

        // Mock notifications for user-2
        QueryDocumentSnapshot notif2 = mock(QueryDocumentSnapshot.class);
        when(notif2.getId()).thenReturn("notif-2");
        when(notif2.toObject(Notification.class)).thenReturn(new Notification(
                "notif-2", "user-2", "event-2", "LOST", "Title2", "Message2",
                new Timestamp(new Date()), false
        ));

        List<QueryDocumentSnapshot> notifDocs2 = new ArrayList<>();
        notifDocs2.add(notif2);

        QuerySnapshot notifSnapshot2 = mock(QuerySnapshot.class);
        when(notifSnapshot2.iterator()).thenReturn(notifDocs2.iterator());

        // Setup individual notification query tasks
        Task<QuerySnapshot> notifTask1 = Tasks.forResult(notifSnapshot1);
        Task<QuerySnapshot> notifTask2 = Tasks.forResult(notifSnapshot2);

        // Mock the notification subcollection queries
        CollectionReference notifCollRef1 = mock(CollectionReference.class);
        CollectionReference notifCollRef2 = mock(CollectionReference.class);

        DocumentReference userDocRef1 = mock(DocumentReference.class);
        DocumentReference userDocRef2 = mock(DocumentReference.class);

        when(mockUsersCollectionRef.document("user-1")).thenReturn(userDocRef1);
        when(mockUsersCollectionRef.document("user-2")).thenReturn(userDocRef2);

        when(userDocRef1.collection("notifications")).thenReturn(notifCollRef1);
        when(userDocRef2.collection("notifications")).thenReturn(notifCollRef2);

        Query query1 = mock(Query.class);
        Query query2 = mock(Query.class);

        when(notifCollRef1.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(query1);
        when(notifCollRef2.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(query2);

        when(query1.get()).thenReturn(notifTask1);
        when(query2.get()).thenReturn(notifTask2);

        // Act
        Task<NotificationRepository.NotificationResult> result = repository.getAllNotificationsWithEmails();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());

        NotificationRepository.NotificationResult notifResult = result.getResult();
        assertNotNull("Notification result should not be null", notifResult);

        // Verify notifications list
        List<Notification> notifications = notifResult.getNotifications();
        assertNotNull("Notifications list should not be null", notifications);
        assertEquals("Should have 2 notifications", 2, notifications.size());

        // Verify email map
        Map<String, String> emailMap = notifResult.getUserIdToEmailMap();
        assertNotNull("Email map should not be null", emailMap);
        assertEquals("Should have 2 user emails", 2, emailMap.size());
        assertEquals("user1@example.com", emailMap.get("user-1"));
        assertEquals("user2@example.com", emailMap.get("user-2"));

        // Verify Firestore operations
        verify(mockUsersCollectionRef).get();
    }

    /**
     * Test getAllNotificationsWithEmails with no users returns empty result.
     */
    @Test
    public void getAllNotificationsWithEmails_handlesNoUsers() {
        // Arrange - Empty users collection
        QuerySnapshot emptyUsersSnapshot = mock(QuerySnapshot.class);
        when(emptyUsersSnapshot.iterator()).thenReturn(new ArrayList<QueryDocumentSnapshot>().iterator());

        Task<QuerySnapshot> usersTask = Tasks.forResult(emptyUsersSnapshot);
        when(mockUsersCollectionRef.get()).thenReturn(usersTask);

        // Act
        Task<NotificationRepository.NotificationResult> result = repository.getAllNotificationsWithEmails();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());

        NotificationRepository.NotificationResult notifResult = result.getResult();
        assertNotNull("Notification result should not be null", notifResult);

        assertTrue("Notifications list should be empty", notifResult.getNotifications().isEmpty());
        assertTrue("Email map should be empty", notifResult.getUserIdToEmailMap().isEmpty());
    }

    /**
     * Test getAllNotificationsWithEmails handles user with missing email field.
     */
    @Test
    public void getAllNotificationsWithEmails_handlesMissingEmail() {
        // Arrange - User with null email
        QueryDocumentSnapshot userDoc = mock(QueryDocumentSnapshot.class);
        when(userDoc.getId()).thenReturn("user-no-email");
        when(userDoc.getString("email")).thenReturn(null);
        when(userDoc.get("email")).thenReturn(null);

        List<QueryDocumentSnapshot> userDocs = new ArrayList<>();
        userDocs.add(userDoc);

        QuerySnapshot usersSnapshot = mock(QuerySnapshot.class);
        when(usersSnapshot.iterator()).thenReturn(userDocs.iterator());

        Task<QuerySnapshot> usersTask = Tasks.forResult(usersSnapshot);
        when(mockUsersCollectionRef.get()).thenReturn(usersTask);

        // Mock empty notifications for this user
        QuerySnapshot emptyNotifSnapshot = mock(QuerySnapshot.class);
        when(emptyNotifSnapshot.iterator()).thenReturn(new ArrayList<QueryDocumentSnapshot>().iterator());
        Task<QuerySnapshot> notifTask = Tasks.forResult(emptyNotifSnapshot);

        CollectionReference notifCollRef = mock(CollectionReference.class);
        DocumentReference userDocRef = mock(DocumentReference.class);

        when(mockUsersCollectionRef.document("user-no-email")).thenReturn(userDocRef);
        when(userDocRef.collection("notifications")).thenReturn(notifCollRef);

        Query query = mock(Query.class);
        when(notifCollRef.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(query);
        when(query.get()).thenReturn(notifTask);

        // Act
        Task<NotificationRepository.NotificationResult> result = repository.getAllNotificationsWithEmails();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should be successful", result.isSuccessful());

        NotificationRepository.NotificationResult notifResult = result.getResult();
        Map<String, String> emailMap = notifResult.getUserIdToEmailMap();

        // Should use userId as fallback when email is null
        assertEquals("Should use userId as fallback", "user-no-email", emailMap.get("user-no-email"));
    }

    /**
     * Test getAllNotificationsWithEmails handles Firestore failure gracefully.
     */
    @Test
    public void getAllNotificationsWithEmails_handlesFirestoreFailure() {
        // Arrange - Firestore query fails
        Exception testException = new Exception("Firestore connection error");
        Task<QuerySnapshot> failedTask = Tasks.forException(testException);
        when(mockUsersCollectionRef.get()).thenReturn(failedTask);

        // Act
        Task<NotificationRepository.NotificationResult> result = repository.getAllNotificationsWithEmails();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        assertNotNull("Result should not be null", result);
        assertTrue("Task should complete (even with error)", result.isComplete());

        // Task completed with result containing empty lists
        NotificationRepository.NotificationResult notifResult = result.getResult();
        assertNotNull("Should return empty result on failure", notifResult);
        assertTrue("Notifications should be empty", notifResult.getNotifications().isEmpty());
        assertTrue("Email map should be empty", notifResult.getUserIdToEmailMap().isEmpty());
    }

    /**
     * Test getAllNotificationsWithEmails sorts notifications by timestamp descending.
     */
    @Test
    public void getAllNotificationsWithEmails_sortsNotificationsByTimestampDescending() {
        // Arrange - 1 user with 2 notifications at different times
        QueryDocumentSnapshot userDoc = mock(QueryDocumentSnapshot.class);
        when(userDoc.getId()).thenReturn("user-1");
        when(userDoc.getString("email")).thenReturn("user1@example.com");

        List<QueryDocumentSnapshot> userDocs = new ArrayList<>();
        userDocs.add(userDoc);

        QuerySnapshot usersSnapshot = mock(QuerySnapshot.class);
        when(usersSnapshot.iterator()).thenReturn(userDocs.iterator());

        Task<QuerySnapshot> usersTask = Tasks.forResult(usersSnapshot);
        when(mockUsersCollectionRef.get()).thenReturn(usersTask);

        // Create notifications with different timestamps
        Timestamp olderTime = new Timestamp(new Date(System.currentTimeMillis() - 10000));
        Timestamp newerTime = new Timestamp(new Date());

        QueryDocumentSnapshot notif1 = mock(QueryDocumentSnapshot.class);
        when(notif1.getId()).thenReturn("notif-old");
        Notification oldNotif = new Notification("notif-old", "user-1", "event-1", "INVITED", "Old", "Old message", olderTime, false);
        when(notif1.toObject(Notification.class)).thenReturn(oldNotif);

        QueryDocumentSnapshot notif2 = mock(QueryDocumentSnapshot.class);
        when(notif2.getId()).thenReturn("notif-new");
        Notification newNotif = new Notification("notif-new", "user-1", "event-2", "LOST", "New", "New message", newerTime, false);
        when(notif2.toObject(Notification.class)).thenReturn(newNotif);

        // Return in arbitrary order (old first)
        List<QueryDocumentSnapshot> notifDocs = new ArrayList<>();
        notifDocs.add(notif1);
        notifDocs.add(notif2);

        QuerySnapshot notifSnapshot = mock(QuerySnapshot.class);
        when(notifSnapshot.iterator()).thenReturn(notifDocs.iterator());
        Task<QuerySnapshot> notifTask = Tasks.forResult(notifSnapshot);

        CollectionReference notifCollRef = mock(CollectionReference.class);
        DocumentReference userDocRef = mock(DocumentReference.class);

        when(mockUsersCollectionRef.document("user-1")).thenReturn(userDocRef);
        when(userDocRef.collection("notifications")).thenReturn(notifCollRef);

        Query query = mock(Query.class);
        when(notifCollRef.orderBy("timestamp", Query.Direction.DESCENDING)).thenReturn(query);
        when(query.get()).thenReturn(notifTask);

        // Act
        Task<NotificationRepository.NotificationResult> result = repository.getAllNotificationsWithEmails();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccessful());

        List<Notification> notifications = result.getResult().getNotifications();
        assertEquals("Should have 2 notifications", 2, notifications.size());

        // Verify newest notification is first (descending order)
        assertEquals("First notification should be newest", "notif-new", notifications.get(0).getNotificationId());
        assertEquals("Second notification should be oldest", "notif-old", notifications.get(1).getNotificationId());
    }
}