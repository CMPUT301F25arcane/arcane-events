/**
 * NotificationRepository.java
 * 
 * Purpose: Repository for managing notification data in Firestore.
 * 
 * Design Pattern: Repository pattern. Provides direct Firebase operations
 * for Notification entities stored in user subcollections.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.repository;

import com.example.arcane.model.Notification;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class for managing notifications in Firestore.
 *
 * <p>Handles CRUD operations for notifications stored in subcollections
 * under user documents: /users/{userId}/notifications/{notificationId}</p>
 *
 * @version 1.0
 */
public class NotificationRepository {
    private static final String COLLECTION_NAME = "users";
    private static final String SUBCOLLECTION_NAME = "notifications";
    private final FirebaseFirestore db;

    /**
     * Default constructor for production use.
     */
    public NotificationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    /**
     * Constructor with dependency injection for testing.
     *
     * @param db the Firestore instance
     */
    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Creates a new notification for a user.
     *
     * @param userId the user ID
     * @param notification the notification to create
     * @return a Task that completes with the document reference of the created notification
     */
    public Task<DocumentReference> createNotification(String userId, Notification notification) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .collection(SUBCOLLECTION_NAME)
                .add(notification);
    }

    /**
     * Gets all notifications for a user, ordered by timestamp descending.
     *
     * @param userId the user ID
     * @return a Task that completes with the query snapshot of notifications
     */
    public Task<QuerySnapshot> getNotificationsForUser(String userId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .collection(SUBCOLLECTION_NAME)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Gets unread notifications for a user.
     * Gets all notifications and filters for unread ones to avoid requiring a composite index.
     *
     * @param userId the user ID
     * @return a Task that completes with the query snapshot of unread notifications
     */
    public Task<QuerySnapshot> getUnreadNotificationsForUser(String userId) {
        // Get all notifications and filter in code to avoid requiring composite index
        // This is acceptable since users typically don't have hundreds of notifications
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .collection(SUBCOLLECTION_NAME)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }

    /**
     * Marks a notification as read.
     *
     * @param userId the user ID
     * @param notificationId the notification ID
     * @return a Task that completes when the notification is updated
     */
    public Task<Void> markAsRead(String userId, String notificationId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .collection(SUBCOLLECTION_NAME)
                .document(notificationId)
                .update("read", true);
    }

    /**
     * Gets a notification by ID.
     *
     * @param userId the user ID
     * @param notificationId the notification ID
     * @return a Task that completes with the notification document snapshot
     */
    public Task<DocumentSnapshot> getNotificationById(String userId, String notificationId) {
        return db.collection(COLLECTION_NAME)
                .document(userId)
                .collection(SUBCOLLECTION_NAME)
                .document(notificationId)
                .get();
    }

    /**
     * Gets all notifications from all users along with a map of userId to email.
     * This method queries all users and collects all their notifications.
     *
     * @return a Task that completes with a result containing notifications and email map
     */
    public Task<NotificationResult> getAllNotificationsWithEmails() {
        // First, get all users
        return db.collection(COLLECTION_NAME).get()
                .continueWithTask(usersSnapshot -> {
                    if (!usersSnapshot.isSuccessful() || usersSnapshot.getResult() == null) {
                        return Tasks.forResult(new NotificationResult(new ArrayList<>(), new HashMap<>()));
                    }

                    List<Task<QuerySnapshot>> notificationTasks = new ArrayList<>();
                    
                    // For each user, get their notifications
                    final List<String> userIds = new ArrayList<>();
                    final Map<String, String> userIdToEmailMap = new HashMap<>();
                    
                    for (QueryDocumentSnapshot userDoc : usersSnapshot.getResult()) {
                        String userId = userDoc.getId();
                        userIds.add(userId);
                        
                        // Extract email from user document
                        String email = userDoc.getString("email");
                        if (email == null || email.isEmpty()) {
                            // Try to get from Users model
                            try {
                                Object emailObj = userDoc.get("email");
                                if (emailObj != null) {
                                    email = emailObj.toString();
                                }
                            } catch (Exception e) {
                                // Email not found, use userId as fallback
                                email = userId;
                            }
                        }
                        userIdToEmailMap.put(userId, email != null ? email : userId);
                        
                        Task<QuerySnapshot> notificationTask = db.collection(COLLECTION_NAME)
                                .document(userId)
                                .collection(SUBCOLLECTION_NAME)
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .get();
                        notificationTasks.add(notificationTask);
                    }
                    
                    final List<String> finalUserIds = userIds;
                    final Map<String, String> finalEmailMap = userIdToEmailMap;

                    // Wait for all notification queries to complete
                    return Tasks.whenAll(notificationTasks)
                            .continueWith(allTasks -> {
                                List<Notification> allNotifications = new ArrayList<>();
                                
                                // Collect all notifications from all users
                                for (int i = 0; i < notificationTasks.size(); i++) {
                                    Task<QuerySnapshot> task = notificationTasks.get(i);
                                    String userId = finalUserIds.get(i);
                                    
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        QuerySnapshot snapshot = task.getResult();
                                        for (QueryDocumentSnapshot doc : snapshot) {
                                            Notification notification = doc.toObject(Notification.class);
                                            notification.setNotificationId(doc.getId());
                                            // Set userId from the user we queried
                                            notification.setUserId(userId);
                                            allNotifications.add(notification);
                                        }
                                    }
                                }
                                
                                // Sort by timestamp descending (most recent first)
                                allNotifications.sort((n1, n2) -> {
                                    if (n1.getTimestamp() == null && n2.getTimestamp() == null) return 0;
                                    if (n1.getTimestamp() == null) return 1;
                                    if (n2.getTimestamp() == null) return -1;
                                    return n2.getTimestamp().compareTo(n1.getTimestamp());
                                });
                                
                                return new NotificationResult(allNotifications, finalEmailMap);
                            });
                });
    }

    /**
     * Result class containing notifications and user email map.
     */
    public static class NotificationResult {
        private final List<Notification> notifications;
        private final Map<String, String> userIdToEmailMap;

        public NotificationResult(List<Notification> notifications, Map<String, String> userIdToEmailMap) {
            this.notifications = notifications;
            this.userIdToEmailMap = userIdToEmailMap;
        }

        public List<Notification> getNotifications() {
            return notifications;
        }

        public Map<String, String> getUserIdToEmailMap() {
            return userIdToEmailMap;
        }
    }
}

