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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
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

    /**
     * Constructs a new EventService instance.
     */
    public EventService() {
        this.eventRepository = new EventRepository();
        this.waitingListRepository = new WaitingListRepository();
        this.decisionRepository = new DecisionRepository();
        this.userRepository = new UserRepository();
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
     * User leaves waiting list for an event.
     *
     * <p>Removes WaitingListEntry and updates user's registeredEventIds.</p>
     *
     * @param eventId the event ID
     * @param entrantId the entrant's user ID
     * @param entryId the waiting list entry ID to remove
     * @return a Task that completes when the user has left the waiting list
     */
    public Task<Void> leaveWaitingList(String eventId, String entrantId, String entryId) {
        // Remove from waiting list
        return waitingListRepository.removeFromWaitingList(eventId, entryId)
                .continueWithTask(removeTask -> {
                    if (!removeTask.isSuccessful()) {
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
}

