# Step-by-Step Code Execution Walkthrough

## Scenario: User 1 registers for Event 1, then views their events, organizer views registrations

---

## Part 1: User 1 Signs Up

### Step 1: Create User Profile
```
UI: Sign Up Page
    ↓
Code: UserService userService = new UserService();
     UserProfile user1 = new UserProfile("user1", "device123", "John Doe", 
                                         "john@email.com", "ENTRANT", null, false);
     userService.createUser(user1);
    ↓
UserService.createUser() calls:
    ↓
UserRepository.createUser(user1)
    ↓
Firebase: db.collection("users").document("user1").set(user1)
    ↓
Firebase Result: /users/user1 created with user data
```

**What happens:**
- `UserService` instance created
- `UserProfile` object created in memory
- `UserRepository` saves to Firebase
- Firebase automatically creates `/users` collection if it doesn't exist
- Document `/users/user1` created with all user fields

---

## Part 2: Organizer Creates Event 1

### Step 2: Create Event
```
UI: Create Event Page
    ↓
Code: EventService eventService = new EventService();
     Event event1 = new Event("event1", "organizer123", "Air Show", 
                              "Annual air show", null, null, eventDate, 
                              "Swan Airfield", 150.0, regStart, regEnd, 
                              100, 20, true, "OPEN");
     eventService.createEvent(event1);
    ↓
EventService.createEvent() calls:
    ↓
EventRepository.createEvent(event1)
    ↓
Firebase: db.collection("events").document("event1").set(event1)
    ↓
Firebase Result: /events/event1 created with event data
```

**What happens:**
- `EventService` instance created
- `Event` object created in memory (with empty `waitingList` and `decisions` lists)
- `EventRepository` saves to Firebase
- Firebase automatically creates `/events` collection
- Document `/events/event1` created (NO waitingList or decisions arrays - they're separate!)

---

## Part 3: User 1 Joins Event 1's Waiting List

### Step 3: Join Waiting List
```
UI: Event Detail Page - User clicks "Join"
    ↓
Code: EventService eventService = new EventService();
     eventService.joinWaitingList("event1", "user1");
    ↓
EventService.joinWaitingList() executes:
    ↓
    1. WaitingListRepository.checkUserInWaitingList("event1", "user1")
       → Firebase: /events/event1/waitingList where entrantId="user1"
       → Result: Empty (user not in list yet)
    ↓
    2. WaitingListRepository.addToWaitingList("event1", entry)
       → Creates: WaitingListEntry entry = new WaitingListEntry();
                  entry.setEntrantId("user1");
                  entry.setJoinTimestamp(now);
       → Firebase: /events/event1/waitingList.add(entry)
       → Firebase Result: /events/event1/waitingList/entry1 created
    ↓
    3. DecisionRepository.createDecision("event1", decision)
       → Creates: Decision decision = new Decision();
                  decision.setEntrantId("user1");
                  decision.setEntryId("entry1");
                  decision.setStatus("PENDING");
                  decision.setUpdatedAt(now);
       → Firebase: /events/event1/decisions.add(decision)
       → Firebase Result: /events/event1/decisions/decision1 created
```

**What happens:**
- Checks if user already registered (prevents duplicates)
- Creates WaitingListEntry in subcollection
- Creates Decision with "PENDING" status in subcollection
- Firebase automatically creates subcollections when first document added

**Firebase Structure Now:**
```
/users/user1
  - userId, name, email, etc.

/events/event1
  - eventName, organizerId, etc.
  - (NO waitingList or decisions arrays here!)

/events/event1/waitingList/entry1
  - entrantId: "user1"
  - joinTimestamp: [timestamp]

/events/event1/decisions/decision1
  - entrantId: "user1"
  - entryId: "entry1"
  - status: "PENDING"
  - updatedAt: [timestamp]
```

---

## Part 4: User 1 Views Their Events

### Step 4: Get User's Events with Decisions
```
UI: Profile/My Events Page
    ↓
Code: UserService userService = new UserService();
     userService.getUserEventsWithDecisions("user1");
    ↓
UserService.getUserEventsWithDecisions() executes:
    ↓
    1. DecisionRepository.getDecisionsByUser("user1")
       → Firebase: Collection group query on "decisions"
                 where entrantId="user1"
       → Firebase searches ALL /events/{eventId}/decisions subcollections
       → Result: Found decision1 in /events/event1/decisions
    ↓
    2. For each decision found:
       → Extract eventId from path: "event1"
       → EventRepository.getEventById("event1")
          → Firebase: /events/event1.get()
          → Result: Event object with eventName, location, etc.
    ↓
    3. Combine data:
       → Event details + Decision status + Timestamps
       → Returns: List of maps with event info and decision status
```

**Code Flow:**
```java
UserService userService = new UserService();
Task<List<Map<String, Object>>> task = 
    userService.getUserEventsWithDecisions("user1");

task.addOnSuccessListener(events -> {
    for (Map<String, Object> event : events) {
        String eventName = (String) event.get("eventName");
        String status = (String) event.get("status");  // "PENDING"
        Timestamp joinDate = (Timestamp) event.get("joinTimestamp");
        // Display in UI
    }
});
```

**What User Sees:**
- Event Name: "Air Show"
- Status: "PENDING"
- Join Date: [when they joined]
- Location: "Swan Airfield"

---

## Part 5: Organizer Views Event 1 Registrations

### Step 5: Get Event Registrations
```
UI: Event Detail Page - Organizer clicks "View Entrants"
    ↓
Code: EventService eventService = new EventService();
     eventService.getEventRegistrations("event1");
    ↓
EventService.getEventRegistrations() executes:
    ↓
    1. WaitingListRepository.getWaitingListForEvent("event1")
       → Firebase: /events/event1/waitingList.get()
       → Result: [entry1 (user1)]
    ↓
    2. DecisionRepository.getDecisionsForEvent("event1")
       → Firebase: /events/event1/decisions.get()
       → Result: [decision1 (user1, status="PENDING")]
    ↓
    3. Combine entries with decisions:
       → Match by entrantId: entry1.entrantId == decision1.entrantId
       → Create registration map:
          {
            entrantId: "user1",
            status: "PENDING",
            joinTimestamp: [from entry1],
            updatedAt: [from decision1]
          }
    ↓
    4. For each entrantId, fetch user details:
       → UserRepository.getUserById("user1")
          → Firebase: /users/user1.get()
          → Result: UserProfile with name, email, etc.
    ↓
    5. Return combined data: List of registrations with user info
```

**Code Flow:**
```java
EventService eventService = new EventService();
Task<Map<String, Object>> task = 
    eventService.getEventRegistrations("event1");

task.addOnSuccessListener(result -> {
    List<Map<String, Object>> registrations = 
        (List) result.get("registrations");
    
    for (Map<String, Object> reg : registrations) {
        String entrantId = (String) reg.get("entrantId");
        String status = (String) reg.get("status");
        Timestamp joinDate = (Timestamp) reg.get("joinTimestamp");
        
        // Fetch user details
        UserRepository userRepo = new UserRepository();
        userRepo.getUserById(entrantId).addOnSuccessListener(userDoc -> {
            UserProfile user = userDoc.toObject(UserProfile.class);
            String userName = user.getName();
            // Display: userName + status + joinDate
        });
    }
});
```

**What Organizer Sees:**
- User Name: "John Doe"
- Email: "john@email.com"
- Status: "PENDING"
- Join Date: [when they joined]
- Updated At: [last status change]

---

## How Event Stores Decisions

**Event 1 does NOT have a list of decisions in its document.**

Instead:
- Event document: `/events/event1` (just event data)
- Decisions subcollection: `/events/event1/decisions/{decisionId}` (separate documents)

**To get decisions:**
```java
// Query the subcollection
DecisionRepository decisionRepo = new DecisionRepository();
decisionRepo.getDecisionsForEvent("event1")
    .addOnSuccessListener(querySnapshot -> {
        // Get all decision documents
        for (QueryDocumentSnapshot doc : querySnapshot) {
            Decision decision = doc.toObject(Decision.class);
            String status = decision.getStatus();  // "PENDING", "ACCEPTED", etc.
        }
    });
```

**Why separate?**
- Scalability: Event document doesn't grow with 1000s of decisions
- Queryability: Can filter by status, entrantId, etc.
- Performance: Only load decisions when needed

---

## Summary

1. **User Signs Up**: `UserService.createUser()` → Creates `/users/user1`
2. **Event Created**: `EventService.createEvent()` → Creates `/events/event1`
3. **User Joins**: `EventService.joinWaitingList()` → Creates both:
   - `/events/event1/waitingList/entry1`
   - `/events/event1/decisions/decision1`
4. **User Views Events**: `UserService.getUserEventsWithDecisions()` → Queries decisions across all events
5. **Organizer Views**: `EventService.getEventRegistrations()` → Queries both subcollections and combines

**Key Point:** Event document is separate from waiting list and decisions. They're in subcollections that are queried separately!

