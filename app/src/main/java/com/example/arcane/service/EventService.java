/**
 * EventService.java
 * 
 * Purpose: Service layer for orchestrating complex event-related operations.
 * 
 * Design Pattern: Service Layer pattern with Facade pattern. Coordinates multiple
 * repositories (EventRepository, WaitingListRepository, DecisionRepository, UserRepository)
 * to provide high-level business operations. Implements OOP Composition by loading
 * related entities (waiting list, decisions) into Event objects.
 * 
 * Outstanding Issues:
 * - The joinWaitingList method has complex nested Task continuations that could be
 *   simplified with better error handling
 * - The workaround for updating user's registeredEventIds should be refactored
 *   to use proper subcollection queries
 * 
 * @version 1.0
 */
package com.example.arcane.service;

import androidx.annotation.NonNull;

import com.example.arcane.model.Decision;
import com.example.arcane.model.Event;
import com.example.arcane.model.UserProfile;
import com.example.arcane.model.WaitingListEntry;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.repository.WaitingListRepository;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class to orchestrate event-related operations.
 *
 * <p>Handles OOP composition while managing Firebase subcollections.
 * Coordinates between EventRepository, WaitingListRepository, DecisionRepository,
 * and UserRepository to provide high-level event management functionality.</p>
 *
 * @version 1.0
 */
public class EventService {
    private final EventRepository eventRepository;
    private final WaitingListRepository waitingListRepository;
    private final DecisionRepository decisionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;


    /**
     * Constructs a new EventService instance.
     */
    public EventService() {
        this(new EventRepository(), new WaitingListRepository(), new DecisionRepository(), new UserRepository(), new NotificationService());
    }

    /**
     * Constructor for dependency injection (used in tests)
     */
    public EventService(EventRepository eventRepository, 
                       WaitingListRepository waitingListRepository,
                       DecisionRepository decisionRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.waitingListRepository = waitingListRepository;
        this.decisionRepository = decisionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Creates a new event.
     *
     * @param event the event to create
     * @return a Task that completes with the document reference of the created event
     */
    public Task<DocumentReference> createEvent(Event event) {
        return eventRepository.createEvent(event);
    }

    /**
     * Updates an existing event.
     *
     * @param event the event with updated fields
     * @return task completing when save succeeds
     */
    public Task<Void> updateEvent(Event event) {
        return eventRepository.updateEvent(event);
    }

    /**
     * Partially updates an event document with provided fields.
     *
     * @param eventId the event ID
     * @param updates the map of fields to update
     * @return a Task that completes when the event is updated
     */
    public Task<Void> updateEventFields(String eventId, Map<String, Object> updates) {
        return eventRepository.updateEventFields(eventId, updates);
    }

    /**
     * Gets an event with all waiting list entries and decisions loaded (OOP composition).
     *
     * @param eventId the event ID to retrieve
     * @return a Task that completes with the event including waiting list and decisions
     */
    public Task<Event> getEventWithDetails(String eventId) {
        return eventRepository.getEventById(eventId)
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return null;
                    }
                    
                    DocumentSnapshot eventDoc = task.getResult();
                    Event event = eventDoc.toObject(Event.class);
                    if (event == null) return null;
                    
                    // Load waiting list (OOP composition)
                    loadWaitingList(event, eventId);
                    
                    // Load decisions (OOP composition)
                    loadDecisions(event, eventId);
                    
                    return event;
                });
    }

    /**
     * Loads waiting list into Event object (OOP composition).
     *
     * @param event the event to load waiting list into
     * @param eventId the event ID
     */
    private void loadWaitingList(Event event, String eventId) {
        waitingListRepository.getWaitingListForEvent(eventId)
                .addOnSuccessListener(querySnapshot -> {
                    List<WaitingListEntry> waitingList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                        entry.setEntryId(doc.getId());
                        waitingList.add(entry);
                    }
                    event.setWaitingList(waitingList);
                });
    }

    /**
     * Loads decisions into Event object (OOP composition).
     *
     * @param event the event to load decisions into
     * @param eventId the event ID
     */
    private void loadDecisions(Event event, String eventId) {
        decisionRepository.getDecisionsForEvent(eventId)
                .addOnSuccessListener(querySnapshot -> {
                    List<Decision> decisions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Decision decision = doc.toObject(Decision.class);
                        decision.setDecisionId(doc.getId());
                        decisions.add(decision);
                    }
                    event.setDecisions(decisions);
                });
    }

    /**
     * User joins waiting list for an event.
     *
     * <p>Creates both WaitingListEntry and Decision records.</p>
     *
     * @param eventId the event ID
     * @param entrantId the entrant's user ID
     * @return a Task that completes with a map containing status and IDs
     */
    public Task<Map<String, String>> joinWaitingList(String eventId, String entrantId) {
        // Check if already in waiting list
        return waitingListRepository.checkUserInWaitingList(eventId, entrantId)
                .continueWithTask(checkTask -> {
                    if (checkTask.isSuccessful() && !checkTask.getResult().isEmpty()) {
                        // User already in waiting list
                        Map<String, String> result = new HashMap<>();
                        result.put("status", "already_exists");
                        return com.google.android.gms.tasks.Tasks.forResult(result);
                    }
                    
                    // Check maxEntrants limit if set
                    return eventRepository.getEventById(eventId)
                            .continueWithTask(eventTask -> {
                                if (eventTask.isSuccessful() && eventTask.getResult() != null && eventTask.getResult().exists()) {
                                    Event event = eventTask.getResult().toObject(Event.class);
                                    if (event != null && event.getMaxEntrants() != null && event.getMaxEntrants() > 0) {
                                        // Check current waiting list size (only count valid users)
                                        return getValidWaitingListCount(eventId)
                                                .continueWithTask(countTask -> {
                                                    if (countTask.isSuccessful()) {
                                                        int currentSize = countTask.getResult();
                                                        if (currentSize >= event.getMaxEntrants()) {
                                                            Map<String, String> result = new HashMap<>();
                                                            result.put("status", "limit_reached");
                                                            return com.google.android.gms.tasks.Tasks.forResult(result);
                                                        }
                                                    }
                                                    // Continue with adding to waiting list
                                                    return addUserToWaitingList(eventId, entrantId);
                                                });
                                    }
                                }
                                // No limit set, continue with adding to waiting list
                                return addUserToWaitingList(eventId, entrantId);
                            });
                });
    }

    /**
     * Gets the count of valid waiting list entries (only entries where the user still exists).
     * This ensures deleted users don't count toward the waitlist limit.
     *
     * @param eventId the event ID
     * @return a Task that completes with the count of valid entries
     */
    public Task<Integer> getValidWaitingListCount(String eventId) {
        return waitingListRepository.getWaitingListForEvent(eventId)
                .continueWithTask(listTask -> {
                    if (!listTask.isSuccessful() || listTask.getResult() == null) {
                        return com.google.android.gms.tasks.Tasks.forResult(0);
                    }
                    
                    QuerySnapshot snapshot = listTask.getResult();
                    if (snapshot.isEmpty()) {
                        return com.google.android.gms.tasks.Tasks.forResult(0);
                    }
                    
                    // Check each entry to see if the user still exists
                    List<Task<Boolean>> userCheckTasks = new ArrayList<>();
                    List<WaitingListEntry> entries = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : snapshot) {
                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                        if (entry != null && entry.getEntrantId() != null) {
                            entries.add(entry);
                            userCheckTasks.add(
                                userRepository.getUserById(entry.getEntrantId())
                                    .continueWith(userTask -> {
                                        return userTask.isSuccessful() && 
                                               userTask.getResult() != null && 
                                               userTask.getResult().exists();
                                    })
                            );
                        }
                    }
                    
                    if (userCheckTasks.isEmpty()) {
                        return com.google.android.gms.tasks.Tasks.forResult(0);
                    }
                    
                    // Wait for all user checks to complete, then count valid ones
                    return com.google.android.gms.tasks.Tasks.whenAll(userCheckTasks)
                            .continueWith(checkTask -> {
                                int validCount = 0;
                                for (int i = 0; i < userCheckTasks.size(); i++) {
                                    try {
                                        Task<Boolean> userCheckTask = userCheckTasks.get(i);
                                        if (userCheckTask.isSuccessful()) {
                                            Boolean isValid = userCheckTask.getResult();
                                            if (Boolean.TRUE.equals(isValid)) {
                                                validCount++;
                                            }
                                        }
                                    } catch (Exception e) {
                                        // Skip invalid entries
                                    }
                                }
                                
                                return validCount;
                            });
                });
    }

    private Task<Map<String, String>> addUserToWaitingList(String eventId, String entrantId) {
        // Create waiting list entry
        WaitingListEntry entry = new WaitingListEntry();
        entry.setEntrantId(entrantId);
        entry.setJoinTimestamp(Timestamp.now());
        
        return waitingListRepository.addToWaitingList(eventId, entry)
                            .continueWithTask(addTask -> {
                                if (!addTask.isSuccessful()) {
                                    Map<String, String> result = new HashMap<>();
                                    result.put("status", "error");
                                    return com.google.android.gms.tasks.Tasks.forResult(result);
                                }
                                
                                String entryId = addTask.getResult().getId();
                                
                                // Create decision with PENDING status
                                Decision decision = new Decision();
                                decision.setEntrantId(entrantId);
                                decision.setEntryId(entryId);
                                decision.setStatus("PENDING");
                                decision.setUpdatedAt(Timestamp.now());
                                
                                return decisionRepository.createDecision(eventId, decision)
                                        .continueWithTask(decTask -> {
                                            Map<String, String> result = new HashMap<>();
                                            if (decTask.isSuccessful()) {
                                                result.put("entryId", entryId);
                                                result.put("decisionId", decTask.getResult().getId());
                                                result.put("status", "success");
                                                
                                                // Workaround: Add eventId to user's registeredEventIds
                                                return userRepository.getUserById(entrantId)
                                                        .continueWithTask(userTask -> {
                                                            if (userTask.isSuccessful() && userTask.getResult() != null) {
                                                                UserProfile user = userTask.getResult().toObject(UserProfile.class);
                                                                if (user != null) {
                                                                    if (user.getRegisteredEventIds() == null) {
                                                                        user.setRegisteredEventIds(new ArrayList<>());
                                                                    }
                                                                    if (!user.getRegisteredEventIds().contains(eventId)) {
                                                                        user.getRegisteredEventIds().add(eventId);
                                                                        userRepository.updateUser(user);
                                                                    }
                                                                }
                                                            }
                                                            return com.google.android.gms.tasks.Tasks.forResult(result);
                                                        });
                                            } else {
                                                result.put("status", "error");
                                                return com.google.android.gms.tasks.Tasks.forResult(result);
                                            }
                                        });
                            });
    }

    /**
     * User leaves waiting list for an event.
     *
     * <p>Removes WaitingListEntry, Decision, and updates user's registeredEventIds.</p>
     *
     * @param eventId the event ID
     * @param entrantId the entrant's user ID
     * @param entryId the waiting list entry ID to remove
     * @param decisionId the decision ID to remove (can be null)
     * @return a Task that completes when the user has left the waiting list
     */
    public Task<Void> leaveWaitingList(String eventId, String entrantId, String entryId, String decisionId) {
        // Get decision first to find decisionId if not provided
        Task<String> decisionIdTask;
        if (decisionId != null && !decisionId.isEmpty()) {
            decisionIdTask = com.google.android.gms.tasks.Tasks.forResult(decisionId);
        } else {
            // Find decision by entryId or entrantId
            decisionIdTask = decisionRepository.getDecisionForUser(eventId, entrantId)
                    .continueWith(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            return task.getResult().getDocuments().get(0).getId();
                        }
                        return null;
                    });
        }
        
        return decisionIdTask
                .continueWithTask(decIdTask -> {
                    // Delete decision if it exists
                    String decId = decIdTask.getResult();
                    Task<Void> deleteDecisionTask = (decId != null) 
                            ? decisionRepository.deleteDecision(eventId, decId)
                            : com.google.android.gms.tasks.Tasks.forResult(null);
                    
                    // Remove from waiting list (parallel operation)
                    Task<Void> removeWaitingListTask = waitingListRepository.removeFromWaitingList(eventId, entryId);
                    
                    // Execute both operations
                    return com.google.android.gms.tasks.Tasks.whenAll(deleteDecisionTask, removeWaitingListTask)
                            .continueWithTask(combinedTask -> {
                                if (!combinedTask.isSuccessful()) {
                                    return com.google.android.gms.tasks.Tasks.forException(new Exception("Failed to remove from waiting list"));
                                }
                                
                                // Remove eventId from user's registeredEventIds (workaround)
                                return userRepository.getUserById(entrantId)
                                        .continueWithTask(userTask -> {
                                            if (userTask.isSuccessful() && userTask.getResult() != null) {
                                                UserProfile user = userTask.getResult().toObject(UserProfile.class);
                                                if (user != null && user.getRegisteredEventIds() != null) {
                                                    user.getRegisteredEventIds().remove(eventId);
                                                    userRepository.updateUser(user);
                                                }
                                            }
                                            return com.google.android.gms.tasks.Tasks.forResult(null);
                                        });
                            });
                });
    }

    /**
     * User accepts a won lottery spot
     * Updates Decision status to ACCEPTED and updates both collections
     */
    public Task<Void> acceptWin(String eventId, String userId, String decisionId) {
        return decisionRepository.getDecisionById(eventId, decisionId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        return com.google.android.gms.tasks.Tasks.forException(new Exception("Decision not found"));
                    }
                    
                    Decision decision = task.getResult().toObject(Decision.class);
                    if (decision == null || !userId.equals(decision.getEntrantId())) {
                        return com.google.android.gms.tasks.Tasks.forException(new Exception("Invalid decision for user"));
                    }
                    
                    // Update decision status to ACCEPTED
                    decision.setStatus("ACCEPTED");
                    decision.setUpdatedAt(Timestamp.now());
                    decision.setRespondedAt(Timestamp.now());
                    
                    return decisionRepository.updateDecision(eventId, decisionId, decision)
                            .continueWithTask(updateTask -> {
                                if (!updateTask.isSuccessful()) {
                                    return com.google.android.gms.tasks.Tasks.forException(new Exception("Failed to update decision"));
                                }
                                
                                // Update userEvents mirror (if userEvents collection exists)
                                // For now, keep registeredEventIds as-is (user already has eventId)
                                
                                return com.google.android.gms.tasks.Tasks.forResult(null);
                            });
                });
    }

    /**
     * User declines a won lottery spot.
     * Updates Decision status to DECLINED and updates both collections.
     *
     * @param eventId the event ID
     * @param userId the user ID
     * @param decisionId the decision ID
     * @return a Task that completes when the decision is declined
     */
    public Task<Void> declineWin(String eventId, String userId, String decisionId) {
        return decisionRepository.getDecisionById(eventId, decisionId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        return com.google.android.gms.tasks.Tasks.forException(new Exception("Decision not found"));
                    }
                    
                    Decision decision = task.getResult().toObject(Decision.class);
                    if (decision == null || !userId.equals(decision.getEntrantId())) {
                        return com.google.android.gms.tasks.Tasks.forException(new Exception("Invalid decision for user"));
                    }
                    
                    // Update decision status to DECLINED
                    decision.setStatus("DECLINED");
                    decision.setUpdatedAt(Timestamp.now());
                    decision.setRespondedAt(Timestamp.now());
                    
                    return decisionRepository.updateDecision(eventId, decisionId, decision)
                            .continueWithTask(updateTask -> {
                                if (!updateTask.isSuccessful()) {
                                    return com.google.android.gms.tasks.Tasks.forException(new Exception("Failed to update decision"));
                                }
                                
                                // Try to promote a replacement winner, but don't fail the decline if it errors out
                                return promoteNextWinner(eventId)
                                        .continueWith(taskResult -> {
                                            if (!taskResult.isSuccessful()) {
                                                Log.w("EventService", "Failed to promote replacement winner", taskResult.getException());
                                            }
                                            return null;
                                        });
                            });
                });
    }

    /**
     * Draw lottery for an event - randomly select winners from PENDING decisions.
     * Winners get status INVITED, losers get status LOST.
     *
     * @param eventId the event ID
     * @return a Task that completes with a map containing lottery results
     */
    public Task<Map<String, Object>> drawLottery(String eventId) {
        // Get event to check numberOfWinners
        return eventRepository.getEventById(eventId)
                .continueWithTask(eventTask -> {
                    if (!eventTask.isSuccessful() || eventTask.getResult() == null || !eventTask.getResult().exists()) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("status", "error");
                        errorResult.put("message", "Event not found");
                        return com.google.android.gms.tasks.Tasks.forResult(errorResult);
                    }

                    Event event = eventTask.getResult().toObject(Event.class);
                    if (event == null || event.getNumberOfWinners() == null || event.getNumberOfWinners() <= 0) {
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("status", "error");
                        errorResult.put("message", "Event does not have valid numberOfWinners");
                        return com.google.android.gms.tasks.Tasks.forResult(errorResult);
                    }

                    int numberOfWinners = event.getNumberOfWinners();
                    String eventName = event.getEventName();

                    // Get all PENDING decisions for this event
                    return decisionRepository.getDecisionsByStatus(eventId, "PENDING")
                            .continueWithTask(pendingTask -> {
                                if (!pendingTask.isSuccessful()) {
                                    Map<String, Object> errorResult = new HashMap<>();
                                    errorResult.put("status", "error");
                                    errorResult.put("message", "Failed to get pending decisions");
                                    return com.google.android.gms.tasks.Tasks.forResult(errorResult);
                                }

                                List<Decision> pendingDecisions = new ArrayList<>();
                                List<String> decisionIds = new ArrayList<>();
                                
                                for (QueryDocumentSnapshot doc : pendingTask.getResult()) {
                                    Decision decision = doc.toObject(Decision.class);
                                    decision.setDecisionId(doc.getId());
                                    pendingDecisions.add(decision);
                                    decisionIds.add(doc.getId());
                                }

                                if (pendingDecisions.isEmpty()) {
                                    Map<String, Object> errorResult = new HashMap<>();
                                    errorResult.put("status", "error");
                                    errorResult.put("message", "No pending entries to draw from");
                                    return com.google.android.gms.tasks.Tasks.forResult(errorResult);
                                }

                                // Shuffle randomly
                                Collections.shuffle(pendingDecisions);

                                // Determine winners (if fewer users than winners, all win)
                                int winnersCount = Math.min(numberOfWinners, pendingDecisions.size());
                                List<Decision> winners = pendingDecisions.subList(0, winnersCount);
                                List<Decision> losers = pendingDecisions.subList(winnersCount, pendingDecisions.size());

                                // Batch update decisions
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                WriteBatch batch = db.batch();
                                Timestamp now = Timestamp.now();

                                // Update winners to INVITED
                                for (Decision winner : winners) {
                                    winner.setStatus("INVITED");
                                    winner.setUpdatedAt(now);
                                    DocumentReference winnerRef = db.collection("events")
                                            .document(eventId)
                                            .collection("decisions")
                                            .document(winner.getDecisionId());
                                    batch.set(winnerRef, winner);
                                }

                                // Update losers to LOST
                                for (Decision loser : losers) {
                                    loser.setStatus("LOST");
                                    loser.setUpdatedAt(now);
                                    DocumentReference loserRef = db.collection("events")
                                            .document(eventId)
                                            .collection("decisions")
                                            .document(loser.getDecisionId());
                                    batch.set(loserRef, loser);
                                }

                                // Execute batch
                                return batch.commit()
                                        .continueWithTask(batchTask -> {
                                            if (!batchTask.isSuccessful()) {
                                                Map<String, Object> errorResult = new HashMap<>();
                                                errorResult.put("status", "error");
                                                errorResult.put("message", "Failed to update decisions: " + 
                                                    (batchTask.getException() != null ? batchTask.getException().getMessage() : "Unknown error"));
                                                return com.google.android.gms.tasks.Tasks.forResult(errorResult);
                                            }

                                            // Create notifications for winners
                                            List<Task<DocumentReference>> winnerNotificationTasks = new ArrayList<>();
                                            for (Decision winner : winners) {
                                                Task<DocumentReference> notificationTask = notificationService.sendNotification(
                                                        winner.getEntrantId(),
                                                        eventId,
                                                        "INVITED",
                                                        "You won the lottery!",
                                                        "Congratulations! You have been selected for " + eventName + ". Please accept or decline your invitation."
                                                );
                                                winnerNotificationTasks.add(notificationTask);
                                            }

                                            // Create notifications for losers
                                            List<Task<DocumentReference>> loserNotificationTasks = new ArrayList<>();
                                            for (Decision loser : losers) {
                                                Task<DocumentReference> notificationTask = notificationService.sendNotification(
                                                        loser.getEntrantId(),
                                                        eventId,
                                                        "LOST",
                                                        "Lottery results",
                                                        "Unfortunately, you were not selected for " + eventName + ". You may still have a chance if someone declines."
                                                );
                                                loserNotificationTasks.add(notificationTask);
                                            }

                                            // Wait for all notifications to be sent
                                            List<Task<DocumentReference>> allNotificationTasks = new ArrayList<>();
                                            allNotificationTasks.addAll(winnerNotificationTasks);
                                            allNotificationTasks.addAll(loserNotificationTasks);

                                            return com.google.android.gms.tasks.Tasks.whenAll(allNotificationTasks)
                                                    .continueWith(notificationTask -> {
                                                        Map<String, Object> result = new HashMap<>();
                                                        result.put("status", "success");
                                                        result.put("winnersCount", winners.size());
                                                        result.put("losersCount", losers.size());
                                                        result.put("message", "Lottery drawn successfully");
                                                        return result;
                                                    });
                                        });
                            });
                });
    }

    /**
     * Promote the next entrant from the waiting list when a winner declines.
     * Prefers PENDING entrants (newly joined) before previously LOST entrants.
     */
    private Task<Void> promoteNextWinner(String eventId) {
        return fetchReplacementCandidate(eventId, "PENDING")
                .continueWithTask(candidateTask -> {
                    Decision candidate = candidateTask.getResult();
                    if (candidate != null) {
                        return promoteCandidateToWinner(eventId, candidate);
                    }
                    return fetchReplacementCandidate(eventId, "LOST")
                            .continueWithTask(lostTask -> {
                                Decision fallback = lostTask.getResult();
                                if (fallback != null) {
                                    return promoteCandidateToWinner(eventId, fallback);
                                }
                                return com.google.android.gms.tasks.Tasks.forResult(null);
                            });
                });
    }

    private Task<Decision> fetchReplacementCandidate(String eventId, String status) {
        return decisionRepository.getDecisionsByStatus(eventId, status)
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || task.getResult().isEmpty()) {
                        return null;
                    }
                    List<Decision> candidates = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Decision decision = doc.toObject(Decision.class);
                        decision.setDecisionId(doc.getId());
                        candidates.add(decision);
                    }
                    if (candidates.isEmpty()) {
                        return null;
                    }
                    Collections.shuffle(candidates);
                    return candidates.get(0);
                });
    }

    private Task<Void> promoteCandidateToWinner(String eventId, Decision candidate) {
        candidate.setStatus("INVITED");
        candidate.setUpdatedAt(Timestamp.now());
        candidate.setRespondedAt(null);

        return decisionRepository.updateDecision(eventId, candidate.getDecisionId(), candidate)
                .continueWithTask(updateTask -> {
                    if (!updateTask.isSuccessful()) {
                        return com.google.android.gms.tasks.Tasks.forException(new Exception("Failed to promote replacement winner"));
                    }

                    return notificationService.sendNotification(
                            candidate.getEntrantId(),
                            eventId,
                            "INVITED",
                            "Spot available!",
                            "A spot opened up and you have been invited from the waiting list."
                    ).continueWith(notificationTask -> {
                        if (!notificationTask.isSuccessful()) {
                            Log.w("EventService", "Failed to notify replacement winner", notificationTask.getException());
                        }
                        return null;
                    });
                });
    }

    /**
     * Gets all users registered for an event with their decisions.
     *
     * <p>Used by organizers to view all registrations and their statuses.</p>
     *
     * @param eventId the event ID
     * @return a Task that completes with a map containing registrations and status
     */
    public Task<Map<String, Object>> getEventRegistrations(String eventId) {
        // Get waiting list entries
        Task<QuerySnapshot> waitingListTask = waitingListRepository.getWaitingListForEvent(eventId);
        // Get decisions
        Task<QuerySnapshot> decisionsTask = decisionRepository.getDecisionsForEvent(eventId);
        
        return com.google.android.gms.tasks.Tasks.whenAll(waitingListTask, decisionsTask)
                .continueWith(combinedTask -> {
                    Map<String, Object> result = new HashMap<>();
                    
                    if (!combinedTask.isSuccessful()) {
                        result.put("status", "error");
                        result.put("registrations", new ArrayList<>());
                        return result;
                    }
                    
                    // Process waiting list
                    List<Map<String, Object>> registrations = new ArrayList<>();
                    Map<String, WaitingListEntry> entryMap = new HashMap<>();
                    
                    if (waitingListTask.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : waitingListTask.getResult()) {
                            WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                            entry.setEntryId(doc.getId());
                            entryMap.put(entry.getEntrantId(), entry);
                        }
                    }
                    
                    // Process decisions and combine with entries
                    if (decisionsTask.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : decisionsTask.getResult()) {
                            Decision decision = doc.toObject(Decision.class);
                            decision.setDecisionId(doc.getId());
                            
                            Map<String, Object> registration = new HashMap<>();
                            registration.put("entrantId", decision.getEntrantId());
                            registration.put("status", decision.getStatus());
                            registration.put("updatedAt", decision.getUpdatedAt());
                            registration.put("respondedAt", decision.getRespondedAt());
                            
                            // Add waiting list entry info
                            WaitingListEntry entry = entryMap.get(decision.getEntrantId());
                            if (entry != null) {
                                registration.put("joinTimestamp", entry.getJoinTimestamp());
                                registration.put("invitedAt", entry.getInvitedAt());
                            }
                            
                            registrations.add(registration);
                        }
                    }
                    
                    result.put("registrations", registrations);
                    result.put("status", "success");
                    return result;
                });
    }

    /**
     * Sends notifications to entrants with specified statuses for an event.
     *
     * @param eventId the event ID
     * @param statuses list of statuses to send notifications for
     * @param title the notification title
     * @param message the notification message
     * @return a Task that completes with a map containing notification results
     */
    public Task<Map<String, Object>> sendNotificationsToEntrants(String eventId, List<String> statuses, String title, String message) {
        if (statuses == null || statuses.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "error");
            result.put("message", "No statuses selected");
            return com.google.android.gms.tasks.Tasks.forResult(result);
        }

        List<Task<Map<String, Object>>> statusTasks = new ArrayList<>();
        for (String status : statuses) {
            Task<Map<String, Object>> statusTask = notificationService.sendNotificationsToEntrantsByStatus(eventId, status, title, message);
            statusTasks.add(statusTask);
        }

        return com.google.android.gms.tasks.Tasks.whenAll(statusTasks)
                .continueWith(allTasks -> {
                    int totalSent = 0;
                    Map<String, Integer> statusCounts = new HashMap<>();
                    
                    for (int i = 0; i < statusTasks.size(); i++) {
                        Task<Map<String, Object>> task = statusTasks.get(i);
                        if (task.isSuccessful() && task.getResult() != null) {
                            Map<String, Object> result = task.getResult();
                            Integer count = (Integer) result.get("count");
                            if (count != null) {
                                totalSent += count;
                                statusCounts.put(statuses.get(i), count);
                            }
                        }
                    }

                    Map<String, Object> finalResult = new HashMap<>();
                    finalResult.put("status", "success");
                    finalResult.put("totalSent", totalSent);
                    finalResult.put("statusCounts", statusCounts);
                    finalResult.put("message", "Sent " + totalSent + " notifications");
                    return finalResult;
                });
    }

    /**
     * Sends notifications to entrants with status-specific messages.
     *
     * @param eventId the event ID
     * @param status the decision status to filter by
     * @param title the notification title
     * @param message the notification message
     * @return a Task that completes with a map containing notification results
     */
    public Task<Map<String, Object>> sendNotificationsToEntrantsByStatus(String eventId, String status, String title, String message) {
        return notificationService.sendNotificationsToEntrantsByStatus(eventId, status, title, message);
    }
}

