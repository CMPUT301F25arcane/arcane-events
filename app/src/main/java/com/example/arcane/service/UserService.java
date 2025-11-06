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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class to orchestrate user-related operations
 * Handles user registration and getting user's events with decisions
 */
public class UserService {
    private final UserRepository userRepository;
    private final DecisionRepository decisionRepository;
    private final EventRepository eventRepository;
    private final WaitingListRepository waitingListRepository;

    public UserService() {
        this.userRepository = new UserRepository();
        this.decisionRepository = new DecisionRepository();
        this.eventRepository = new EventRepository();
        this.waitingListRepository = new WaitingListRepository();
    }

    /**
     * Create a new user profile (sign up)
     */
    public Task<Void> createUser(UserProfile user) {
        // Initialize registeredEventIds list
        if (user.getRegisteredEventIds() == null) {
            user.setRegisteredEventIds(new ArrayList<>());
        }
        return userRepository.createUser(user);
    }

    /**
     * Get user by ID
     */
    public Task<UserProfile> getUserById(String userId) {
        return userRepository.getUserById(userId)
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().toObject(UserProfile.class);
                    }
                    return null;
                });
    }

    /**
     * Update user profile
     */
    public Task<Void> updateUser(UserProfile user) {
        return userRepository.updateUser(user);
    }

    /**
     * Get all events that a user has registered for with their decisions
     * This implements the user's "My Events" view
     * WORKAROUND: Uses registeredEventIds from UserProfile instead of collection group query
     */
    public Task<List<Map<String, Object>>> getUserEventsWithDecisions(String userId) {
        // Get user profile with registeredEventIds
        return userRepository.getUserById(userId)
                .continueWithTask(userTask -> {
                    if (!userTask.isSuccessful() || userTask.getResult() == null) {
                        return Tasks.forResult(new ArrayList<>());
                    }
                    
                    UserProfile user = userTask.getResult().toObject(UserProfile.class);
                    if (user == null || user.getRegisteredEventIds() == null || user.getRegisteredEventIds().isEmpty()) {
                        return Tasks.forResult(new ArrayList<>());
                    }
                    
                    // Create tasks to fetch each event and its decision
                    List<Task<Map<String, Object>>> eventTasks = new ArrayList<>();
                    
                    for (String eventId : user.getRegisteredEventIds()) {
                        Task<Map<String, Object>> eventTask = eventRepository.getEventById(eventId)
                                .continueWithTask(eventDocTask -> {
                                    if (!eventDocTask.isSuccessful() || eventDocTask.getResult() == null || !eventDocTask.getResult().exists()) {
                                        return Tasks.forResult((Map<String, Object>) null);
                                    }
                                    
                                    Event event = eventDocTask.getResult().toObject(Event.class);
                                    if (event == null) {
                                        return Tasks.forResult((Map<String, Object>) null);
                                    }
                                    
                                    String eventIdInner = eventDocTask.getResult().getId();
                                    Map<String, Object> userEvent = new HashMap<>();
                                    userEvent.put("eventId", eventIdInner);
                                    userEvent.put("eventName", event.getEventName());
                                    userEvent.put("eventDate", event.getEventDate());
                                    userEvent.put("location", event.getLocation());
                                    userEvent.put("cost", event.getCost());
                                    
                                    // Get decision for this event
                                    return decisionRepository.getDecisionForUser(eventIdInner, userId)
                                            .continueWithTask(decisionTask -> {
                                                if (decisionTask.isSuccessful() && !decisionTask.getResult().isEmpty()) {
                                                    QueryDocumentSnapshot doc = (QueryDocumentSnapshot) decisionTask.getResult().getDocuments().get(0);
                                                    Decision decision = doc.toObject(Decision.class);
                                                    if (decision != null) {
                                                        userEvent.put("status", decision.getStatus());
                                                        userEvent.put("updatedAt", decision.getUpdatedAt());
                                                        userEvent.put("respondedAt", decision.getRespondedAt());
                                                    }
                                                }
                                                
                                                // Get waiting list entry for join timestamp
                                                return waitingListRepository.checkUserInWaitingList(eventIdInner, userId)
                                                        .continueWith(waitingTask -> {
                                                            if (waitingTask.isSuccessful() && !waitingTask.getResult().isEmpty()) {
                                                                QueryDocumentSnapshot doc = (QueryDocumentSnapshot) waitingTask.getResult().getDocuments().get(0);
                                                                WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                                                                if (entry != null) {
                                                                    userEvent.put("joinTimestamp", entry.getJoinTimestamp());
                                                                    userEvent.put("invitedAt", entry.getInvitedAt());
                                                                }
                                                            }
                                                            return userEvent;
                                                        });
                                            });
                                });
                        
                        eventTasks.add(eventTask);
                    }
                    
                    // Wait for all event tasks to complete
                    return Tasks.whenAllSuccess(eventTasks)
                            .continueWith(allEventsTask -> {
                                if (!allEventsTask.isSuccessful()) {
                                    return new ArrayList<>();
                                }
                                
                                List<Map<String, Object>> userEvents = new ArrayList<>();
                                
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> results = (List<Map<String, Object>>) allEventsTask.getResult();
                                
                                for (Map<String, Object> event : results) {
                                    if (event != null) {
                                        userEvents.add(event);
                                    }
                                }
                                
                                return userEvents;
                            });
                });
    }


    /**
     * User updates their decision (ACCEPT/DECLINE)
     */
    public Task<Void> updateUserDecision(String eventId, String decisionId, String newStatus) {
        return decisionRepository.getDecisionById(eventId, decisionId)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return Tasks.forException(new Exception("Decision not found"));
                    }
                    
                    Decision decision = task.getResult().toObject(Decision.class);
                    if (decision == null) {
                        return Tasks.forException(new Exception("Decision not found"));
                    }
                    
                    decision.setStatus(newStatus);
                    decision.setUpdatedAt(com.google.firebase.Timestamp.now());
                    
                    if (newStatus.equals("ACCEPTED") || newStatus.equals("DECLINED")) {
                        decision.setRespondedAt(com.google.firebase.Timestamp.now());
                    }
                    
                    return decisionRepository.updateDecision(eventId, decisionId, decision);
                });
    }
}

