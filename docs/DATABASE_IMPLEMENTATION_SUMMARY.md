# Database Implementation Summary

## Overview
Complete Firebase database implementation with OOP design. Classes support composition in code while Firebase uses separate collections for scalability.

---

## Files Created/Modified

### **Model Classes** (`/model/`)

#### 1. **UserProfile.java** ✅ CREATED
- **Purpose**: Represents user account data
- **OOP Composition**: `List<String> registeredEventIds` (in-memory only, not saved to Firestore)
- **Firebase Collection**: `/users/{userId}`
- **Key Methods**: Getters/setters for all fields
- **Usage**: Sign up page creates UserProfile, saves to `/users/{userId}`

#### 2. **Event.java** ✅ CREATED
- **Purpose**: Represents event data
- **OOP Composition**: 
  - `List<WaitingListEntry> waitingList` (in-memory, loaded from subcollection)
  - `List<Decision> decisions` (in-memory, loaded from subcollection)
- **Firebase Collection**: `/events/{eventId}`
- **Key Methods**: 
  - `addToWaitingList()` - OOP method to add entry
  - `getWaitingList()` / `setWaitingList()` - Access composed lists
- **Usage**: Event creation creates Event document, waiting list/decisions stored separately

#### 3. **WaitingListEntry.java** ✅ CREATED
- **Purpose**: Represents a user joining an event's waiting list
- **Firebase Collection**: `/events/{eventId}/waitingList/{entryId}` (subcollection)
- **Key Fields**: `entrantId`, `joinTimestamp`, `invitedAt`
- **Usage**: Created when user joins waiting list

#### 4. **Decision.java** ✅ CREATED
- **Purpose**: Represents user's decision/status for an event
- **Firebase Collection**: `/events/{eventId}/decisions/{decisionId}` (subcollection)
- **Key Fields**: `entrantId`, `entryId`, `status`, `updatedAt`, `respondedAt`
- **Status Values**: "PENDING", "INVITED", "ACCEPTED", "DECLINED", "CANCELLED"
- **Usage**: Tracks user's response to lottery invitations

---

### **Repository Classes** (`/repository/`)

#### 5. **UserRepository.java** ✅ CREATED
- **Purpose**: Direct Firebase operations for UserProfile
- **Key Methods**:
  - `createUser()` - Save new user to `/users/{userId}`
  - `getUserById()` - Get user by ID
  - `updateUser()` - Update user profile
  - `deleteUser()` - Delete user
  - `getUserByDeviceId()` - Find user by device ID (for authentication)
- **Usage**: Called by UserService for all user operations

#### 6. **EventRepository.java** ✅ CREATED
- **Purpose**: Direct Firebase operations for Event
- **Key Methods**:
  - `createEvent()` - Save new event to `/events/{eventId}`
  - `getEventById()` - Get event by ID
  - `updateEvent()` - Update event
  - `getAllEvents()` - Get all events
  - `getEventsByOrganizer()` - Get organizer's events
  - `getOpenEvents()` - Get events open for registration
- **Usage**: Called by EventService for event operations

#### 7. **WaitingListRepository.java** ✅ CREATED
- **Purpose**: Firebase operations for WaitingListEntry subcollection
- **Key Methods**:
  - `addToWaitingList()` - Add entry to `/events/{eventId}/waitingList`
  - `getWaitingListForEvent()` - Get all entries for an event
  - `checkUserInWaitingList()` - Check if user already registered
  - `removeFromWaitingList()` - Remove user from waiting list
  - `getWaitingListEntriesByUser()` - Get all events user registered for (collection group query)
- **Usage**: Called by EventService when users join waiting list

#### 8. **DecisionRepository.java** ✅ CREATED
- **Purpose**: Firebase operations for Decision subcollection
- **Key Methods**:
  - `createDecision()` - Create decision in `/events/{eventId}/decisions`
  - `getDecisionsForEvent()` - Get all decisions for an event (organizer view)
  - `getDecisionForUser()` - Get user's decision for an event
  - `updateDecision()` - Update decision status
  - `getDecisionsByUser()` - Get all decisions for a user across all events (collection group query)
  - `getDecisionsByStatus()` - Get decisions by status (e.g., all "PENDING")
- **Usage**: Called by EventService and UserService for decision tracking

---

### **Service Classes** (`/service/`)

#### 9. **EventService.java** ✅ CREATED
- **Purpose**: Orchestrates event-related business logic
- **Key Methods**:
  - `createEvent()` - Create new event (saves to `/events/{eventId}`)
  - `getEventWithDetails()` - Load event with waiting list and decisions (OOP composition)
  - `joinWaitingList()` - User joins event (creates both WaitingListEntry AND Decision)
  - `getEventRegistrations()` - Organizer gets all users registered with decisions
- **OOP Features**: 
  - Loads subcollections into Event's `waitingList` and `decisions` lists
  - Maintains OOP composition while using Firebase subcollections
- **Usage**: Called by UI when creating events, joining waiting lists, viewing registrations

#### 10. **UserService.java** ✅ CREATED
- **Purpose**: Orchestrates user-related business logic
- **Key Methods**:
  - `createUser()` - Create new user (sign up functionality)
  - `getUserById()` - Get user profile
  - `updateUser()` - Update user profile
  - `getUserEventsWithDecisions()` - **MAIN FEATURE**: Get all events user registered for with decisions
  - `updateUserDecision()` - User accepts/declines invitation
- **Usage**: Called by UI for sign up, profile updates, "My Events" view

---

## How It Works - Flow Examples

### **Example 1: User Signs Up**
```
Sign Up Page
    ↓
UserService.createUser(userProfile)
    ↓
UserRepository.createUser(userProfile)
    ↓
Firebase: /users/{userId} created with user data
```

### **Example 2: User Joins Event Waiting List**
```
User clicks "Join Event"
    ↓
EventService.joinWaitingList(eventId, userId)
    ↓
1. WaitingListRepository.addToWaitingList()
   → Firebase: /events/{eventId}/waitingList/{entryId} created
    ↓
2. DecisionRepository.createDecision()
   → Firebase: /events/{eventId}/decisions/{decisionId} created with status="PENDING"
```

### **Example 3: User Views "My Events"**
```
User clicks "My Events"
    ↓
UserService.getUserEventsWithDecisions(userId)
    ↓
1. DecisionRepository.getDecisionsByUser(userId)
   → Queries all /events/{eventId}/decisions where entrantId=userId
    ↓
2. For each decision, fetch Event from /events/{eventId}
    ↓
3. Combine: Event details + Decision status + WaitingListEntry info
    ↓
Display: Event name, status, join date, etc.
```

### **Example 4: Organizer Views Event Registrations**
```
Organizer clicks "View Entrants" for Event
    ↓
EventService.getEventRegistrations(eventId)
    ↓
1. WaitingListRepository.getWaitingListForEvent(eventId)
   → Get all /events/{eventId}/waitingList entries
    ↓
2. DecisionRepository.getDecisionsForEvent(eventId)
   → Get all /events/{eventId}/decisions
    ↓
3. Combine entries with decisions (match by entrantId)
    ↓
Display: User name, status, join date, response date
```

---

## Firebase Collection Structure

```
/users/{userId}
  - UserProfile data

/events/{eventId}
  - Event data (NO waitingList or decisions arrays)

/events/{eventId}/waitingList/{entryId}
  - WaitingListEntry data

/events/{eventId}/decisions/{decisionId}
  - Decision/Status data
```

---

## Key Features

✅ **OOP Design**: Classes use composition (`Event` has `List<WaitingListEntry>`)  
✅ **Firebase Separation**: Data stored in separate collections for scalability  
✅ **Service Layer**: Orchestrates complex operations  
✅ **Repository Pattern**: Clean separation of Firebase logic  
✅ **Complete Flow**: Sign up → Join event → View events → Track decisions  

---

## Usage Examples

### Create User (Sign Up)
```java
UserService userService = new UserService();
UserProfile user = new UserProfile(userId, deviceId, name, email, role, geolocation, false);
userService.createUser(user).addOnSuccessListener(...);
```

### Join Event Waiting List
```java
EventService eventService = new EventService();
eventService.joinWaitingList(eventId, userId).addOnSuccessListener(result -> {
    String status = result.get("status");
    if (status.equals("success")) {
        // User successfully joined
    }
});
```

### Get User's Events with Decisions
```java
UserService userService = new UserService();
userService.getUserEventsWithDecisions(userId).addOnSuccessListener(events -> {
    for (Map<String, Object> event : events) {
        String eventName = (String) event.get("eventName");
        String status = (String) event.get("status");
        // Display in UI
    }
});
```

### Get Event Registrations (Organizer View)
```java
EventService eventService = new EventService();
eventService.getEventRegistrations(eventId).addOnSuccessListener(result -> {
    List<Map<String, Object>> registrations = (List) result.get("registrations");
    // Display all users with their decisions
});
```

---

## Files Summary

| File | Type | Purpose |
|------|------|---------|
| UserProfile.java | Model | User data model |
| Event.java | Model | Event data model with OOP composition |
| WaitingListEntry.java | Model | Waiting list entry model |
| Decision.java | Model | Decision/status model |
| UserRepository.java | Repository | User Firebase operations |
| EventRepository.java | Repository | Event Firebase operations |
| WaitingListRepository.java | Repository | Waiting list Firebase operations |
| DecisionRepository.java | Repository | Decision Firebase operations |
| EventService.java | Service | Event business logic |
| UserService.java | Service | User business logic |

---

## Next Steps

1. **Create Firebase Indexes**: 
   - Collection group index for `waitingList` on `entrantId`
   - Collection group index for `decisions` on `entrantId`

2. **Integrate with UI**: 
   - Use `UserService.createUser()` in sign up fragment
   - Use `EventService.joinWaitingList()` in event detail fragment
   - Use `UserService.getUserEventsWithDecisions()` in profile/history fragment

3. **Error Handling**: Add proper error handling and user feedback in UI

4. **Testing**: Test all flows with real Firebase data

