package com.example.arcane.service;

/**
 * This file defines the EventService class, which provides business logic for event-related
 * operations. It orchestrates complex operations involving events, waiting lists, decisions,
 * and user registrations. Handles OOP composition by loading waiting list entries and decisions
 * into Event objects. Manages lottery drawing, waitlist joining/leaving, and decision acceptance/decline.
 *
 * Design Pattern: Service Layer Pattern, Facade Pattern, OOP Composition
 * - Provides business logic for event operations
 * - Acts as a facade to multiple repositories (Event, WaitingList, Decision, User)
 * - Handles OOP composition by loading related entities into Event objects
 * - Manages complex async operations with nested Task continuations
 *
 * Outstanding Issues:
 * - Nested Task continuations could be simplified with better error handling
 * - registeredEventIds workaround may need to be replaced with proper subcollection queries
 */
import com.example.arcane.model.Decision;
import com.example.arcane.model.Event;
import com.example.arcane.model.UserProfile;
import com.example.arcane.model.WaitingListEntry;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.repository.UserRepository;
import com.example.arcane.repository.WaitingListRepository;
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
 * Service class that orchestrates event-related operations.
 * Handles OOP composition while managing Firebase subcollections.
 *
 * @version 1.0
 */
public class EventService {
    private final EventRepository eventRepository;
    private final WaitingListRepository waitingListRepository;
    private final DecisionRepository decisionRepository;
    private final UserRepository userRepository;

    /**
     * Constructs a new EventService with default repository instances.
     *
     * @version 1.0
     */
    public EventService() {
        this.eventRepository = new EventRepository();
        this.waitingListRepository = new WaitingListRepository();
        this.decisionRepository = new DecisionRepository();
        this.userRepository = new UserRepository();
    }

    /**
     * Constructs a new EventService with injected repository dependencies.
     * Used for dependency injection in tests.
     *
     * @param eventRepository The EventRepository instance to use
     * @param waitingListRepository The WaitingListRepository instance to use
     * @param decisionRepository The DecisionRepository instance to use
     * @param userRepository The UserRepository instance to use
     * @version 1.0
     */
    public EventService(EventRepository eventRepository, 
                       WaitingListRepository waitingListRepository,
                       DecisionRepository decisionRepository,
                       UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.waitingListRepository = waitingListRepository;
        this.decisionRepository = decisionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new event in Firestore.
     *
     * @param event The Event object to create
     * @return A Task that completes with the document reference of the created event
     */
    public Task<DocumentReference> createEvent(Event event) {
        return eventRepository.createEvent(event);
    }

    /**
     * Gets an event with all waiting list entries and decisions loaded (OOP composition).
     * Loads the event from Firestore and populates its waiting list and decisions lists.
     *
     * @param eventId The unique identifier of the event
     * @return A Task that completes with the Event object containing loaded waiting list and decisions
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
     * Loads waiting list entries into the Event object (OOP composition).
     * Fetches all waiting list entries for the event and sets them on the Event object.
     *
     * @param event The Event object to populate
     * @param eventId The unique identifier of the event
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
     * Loads decisions into the Event object (OOP composition).
     * Fetches all decisions for the event and sets them on the Event object.
     *
     * @param event The Event object to populate
     * @param eventId The unique identifier of the event
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
     * Adds a user to the waiting list for an event.
     * Creates both a WaitingListEntry and a Decision with PENDING status.
     * Also updates the user's registeredEventIds as a workaround.
     *
     * @param eventId The unique identifier of the event
     * @param entrantId The unique identifier of the user joining the waitlist
     * @return A Task that completes with a map containing status, entryId, and decisionId
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
                });
    }

    /**
     * Removes a user from the waiting list for an event.
     * Deletes both the WaitingListEntry and Decision, and updates the user's registeredEventIds.
     *
     * @param eventId The unique identifier of the event
     * @param entrantId The unique identifier of the user leaving the waitlist
     * @param entryId The unique identifier of the waiting list entry to remove
     * @param decisionId The unique identifier of the decision to remove (can be null, will be looked up)
     * @return A Task that completes when the user is removed from the waitlist
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
     * Handles a user accepting a won lottery spot.
     * Updates the Decision status to ACCEPTED and sets the respondedAt timestamp.
     *
     * @param eventId The unique identifier of the event
     * @param userId The unique identifier of the user accepting
     * @param decisionId The unique identifier of the decision to update
     * @return A Task that completes when the decision is updated
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
     * Handles a user declining a won lottery spot.
     * Updates the Decision status to DECLINED and sets the respondedAt timestamp.
     *
     * @param eventId The unique identifier of the event
     * @param userId The unique identifier of the user declining
     * @param decisionId The unique identifier of the decision to update
     * @return A Task that completes when the decision is updated
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
                                
                                // Update userEvents mirror (if userEvents collection exists)
                                // For now, keep registeredEventIds as-is
                                
                                return com.google.android.gms.tasks.Tasks.forResult(null);
                            });
                });
    }

    /**
     * Draws the lottery for an event by randomly selecting winners from PENDING decisions.
     * Winners get status INVITED, losers get status LOST. Uses a batch write for efficiency.
     *
     * @param eventId The unique identifier of the event
     * @return A Task that completes with a map containing status, winnersCount, losersCount, and message
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
                                        .continueWith(batchTask -> {
                                            Map<String, Object> result = new HashMap<>();
                                            if (batchTask.isSuccessful()) {
                                                result.put("status", "success");
                                                result.put("winnersCount", winners.size());
                                                result.put("losersCount", losers.size());
                                                result.put("message", "Lottery drawn successfully");
                                            } else {
                                                result.put("status", "error");
                                                result.put("message", "Failed to update decisions: " + 
                                                    (batchTask.getException() != null ? batchTask.getException().getMessage() : "Unknown error"));
                                            }
                                            return result;
                                        });
                            });
                });
    }

    /**
     * Gets all users registered for an event with their decision status.
     * Combines waiting list entries and decisions to provide complete registration information.
     *
     * @param eventId The unique identifier of the event
     * @return A Task that completes with a map containing status and a list of registrations
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
}

