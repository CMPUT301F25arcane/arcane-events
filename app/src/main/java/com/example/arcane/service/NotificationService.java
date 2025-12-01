/**
 * NotificationService.java
 * 
 * Purpose: Service layer for managing notification operations.
 * 
 * Design Pattern: Service Layer pattern. Orchestrates notification-related
 * business logic and coordinates with repositories.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.service;

import com.example.arcane.model.Notification;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.NotificationRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.repository.WaitingListRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for managing notification operations.
 *
 * <p>Handles notification creation, retrieval, and read status updates.
 * Checks user notification preferences before sending notifications.</p>
 *
 * @version 1.0
 */
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DecisionRepository decisionRepository;
    private final WaitingListRepository waitingListRepository;

    /**
     * Constructs a new NotificationService instance.
     */
    public NotificationService() {
        this(new NotificationRepository(), new UserRepository(), new DecisionRepository(), new WaitingListRepository());
    }

    /**
     * Constructor for dependency injection (used in tests).
     *
     * @param notificationRepository the notification repository
     * @param userRepository the user repository
     * @param decisionRepository the decision repository
     * @param waitingListRepository the waiting list repository
     */
    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, DecisionRepository decisionRepository, WaitingListRepository waitingListRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.decisionRepository = decisionRepository;
        this.waitingListRepository = waitingListRepository;
    }

    /**
     * Sends a notification to a user if they haven't opted out.
     *
     * @param userId the user ID
     * @param eventId the event ID
     * @param type the notification type ("INVITED" or "LOST")
     * @param title the notification title
     * @param message the notification message
     * @return a Task that completes with the document reference if sent, or null if user opted out
     */
    public Task<DocumentReference> sendNotification(String userId, String eventId, String type,
                                                    String title, String message) {
        // Check if user has opted out
        return userRepository.getUserById(userId)
                .continueWithTask(userTask -> {
                    if (!userTask.isSuccessful() || userTask.getResult() == null || !userTask.getResult().exists()) {
                        // User not found, don't send notification
                        return com.google.android.gms.tasks.Tasks.forResult((DocumentReference) null);
                    }

                    DocumentSnapshot userDoc = userTask.getResult();
                    Boolean notificationOptOut = userDoc.getBoolean("notificationOptOut");
                    
                    if (Boolean.TRUE.equals(notificationOptOut)) {
                        // User has opted out, don't send notification
                        return com.google.android.gms.tasks.Tasks.forResult((DocumentReference) null);
                    }

                    // User hasn't opted out, create notification
                    Notification notification = new Notification(
                            null,  // notificationId will be set by Firestore
                            userId,
                            eventId,
                            type,
                            title,
                            message,
                            Timestamp.now(),
                            false  // unread by default
                    );

                    return notificationRepository.createNotification(userId, notification);
                });
    }

    /**
     * Sends notifications to all entrants with a specific status for an event.
     *
     * @param eventId the event ID
     * @param status the decision status to filter by
     * @param title the notification title
     * @param message the notification message
     * @return a Task that completes with a map containing the count of notifications sent
     */
    public Task<Map<String, Object>> sendNotificationsToEntrantsByStatus(String eventId, String status, String title, String message) {
        return decisionRepository.getDecisionsByStatus(eventId, status)
                .continueWithTask(decisionsTask -> {
                    if (!decisionsTask.isSuccessful()) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("status", "error");
                        errorResult.put("count", 0);
                        errorResult.put("message", "Failed to get decisions");
                        return com.google.android.gms.tasks.Tasks.forResult(errorResult);
                    }

                    QuerySnapshot decisionsSnapshot = decisionsTask.getResult();
                    if (decisionsSnapshot == null || decisionsSnapshot.isEmpty()) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("status", "success");
                        result.put("count", 0);
                        result.put("message", "No entrants found with status " + status);
                        return com.google.android.gms.tasks.Tasks.forResult(result);
                    }

                    List<Task<DocumentReference>> notificationTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : decisionsSnapshot) {
                        String entrantId = doc.getString("entrantId");
                        if (entrantId != null) {
                            Task<DocumentReference> notificationTask = sendNotification(entrantId, eventId, status, title, message);
                            notificationTasks.add(notificationTask);
                        }
                    }

                    return com.google.android.gms.tasks.Tasks.whenAll(notificationTasks)
                            .continueWith(allTasks -> {
                                int sentCount = 0;
                                for (Task<DocumentReference> task : notificationTasks) {
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        sentCount++;
                                    }
                                }

                                Map<String, Object> result = new HashMap<>();
                                result.put("status", "success");
                                result.put("count", sentCount);
                                result.put("message", "Sent " + sentCount + " notifications");
                                return result;
                            });
                });
    }

    /**
     * Gets all notifications for a user.
     *
     * @param userId the user ID
     * @return a Task that completes with the query snapshot of notifications
     */
    public Task<QuerySnapshot> getUserNotifications(String userId) {
        return notificationRepository.getNotificationsForUser(userId);
    }

    /**
     * Gets unread notifications for a user.
     * Returns all notifications (caller should filter for unread) to avoid requiring a composite index.
     *
     * @param userId the user ID
     * @return a Task that completes with the query snapshot of all notifications (filter for unread in calling code)
     */
    public Task<QuerySnapshot> getUnreadNotifications(String userId) {
        // Get all notifications ordered by timestamp - caller will filter for unread
        // This avoids requiring a composite index for whereEqualTo + orderBy
        return notificationRepository.getUnreadNotificationsForUser(userId);
    }

    /**
     * Sends notifications to all entrants on the waiting list for an event.
     * This is used for "Enrolled" entrants who are on the waiting list but haven't been selected yet.
     *
     * @param eventId the event ID
     * @param title the notification title
     * @param message the notification message
     * @return a Task that completes with a map containing the count of notifications sent
     */
    public Task<Map<String, Object>> sendNotificationsToWaitingListEntrants(String eventId, String title, String message) {
        return waitingListRepository.getWaitingListForEvent(eventId)
                .continueWithTask(waitingListTask -> {
                    if (!waitingListTask.isSuccessful()) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("status", "error");
                        errorResult.put("count", 0);
                        errorResult.put("message", "Failed to get waiting list");
                        return com.google.android.gms.tasks.Tasks.forResult(errorResult);
                    }

                    QuerySnapshot waitingListSnapshot = waitingListTask.getResult();
                    if (waitingListSnapshot == null || waitingListSnapshot.isEmpty()) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("status", "success");
                        result.put("count", 0);
                        result.put("message", "No entrants found on waiting list");
                        return com.google.android.gms.tasks.Tasks.forResult(result);
                    }

                    List<Task<DocumentReference>> notificationTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : waitingListSnapshot) {
                        String entrantId = doc.getString("entrantId");
                        if (entrantId != null) {
                            Task<DocumentReference> notificationTask = sendNotification(entrantId, eventId, "ENROLLED", title, message);
                            notificationTasks.add(notificationTask);
                        }
                    }

                    return com.google.android.gms.tasks.Tasks.whenAll(notificationTasks)
                            .continueWith(allTasks -> {
                                int sentCount = 0;
                                for (Task<DocumentReference> task : notificationTasks) {
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        sentCount++;
                                    }
                                }

                                Map<String, Object> result = new HashMap<>();
                                result.put("status", "success");
                                result.put("count", sentCount);
                                result.put("message", "Sent " + sentCount + " notifications");
                                return result;
                            });
                });
    }

    /**
     * Marks a notification as read.
     *
     * @param userId the user ID
     * @param notificationId the notification ID
     * @return a Task that completes when the notification is marked as read
     */
    public Task<Void> markNotificationRead(String userId, String notificationId) {
        return notificationRepository.markAsRead(userId, notificationId);
    }
}

