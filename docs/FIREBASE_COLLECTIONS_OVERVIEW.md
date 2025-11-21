# Firebase Collections Overview

This document provides a comprehensive overview of all Firestore collections, subcollections, and their structures used in the Arcane Events application.

---

## Collection Structure

```
Firestore Database
├── users/                          (Top-level collection)
│   ├── {userId}/                  (User document)
│   │   └── notifications/         (Subcollection)
│   │       └── {notificationId}/  (Notification document)
│   └── ...
│
└── events/                         (Top-level collection)
    ├── {eventId}/                 (Event document)
    │   ├── waitingList/           (Subcollection)
    │   │   └── {entryId}/         (WaitingListEntry document)
    │   └── decisions/             (Subcollection)
    │       └── {decisionId}/      (Decision document)
    └── ...
```

---

## 1. Users Collection

**Path:** `/users/{userId}`

**Repository:** `UserRepository.java`

**Model Classes:** `UserProfile.java`, `Users.java`

### Document Structure

#### UserProfile Model
```java
{
  userId: String,                    // Document ID (Firebase Auth UID)
  deviceId: String,                  // Device identifier
  name: String,                      // User's name
  email: String,                     // User's email
  role: String,                      // "ENTRANT", "ORGANIZER", "ADMIN"
  geolocation: GeoPoint,             // Optional geographic location
  notificationOptOut: Boolean,       // Notification preference
  registeredEventIds: List<String>   // Workaround: list of event IDs user registered for
}
```

#### Users Model (Alternative)
```java
{
  id: String,                        // Document ID (Firebase Auth UID)
  name: String,
  email: String,
  phone: String,                     // Optional
  deviceId: String,                  // Optional
  createdAt: Timestamp,
  role: String,                      // "USER", "ORGANISER", "ADMIN"
  registeredEventIds: List<String>,
  notificationOptOut: Boolean
}
```

### Key Operations

- **Create User:** `createUser(UserProfile user)` - Creates new user document
- **Get User by ID:** `getUserById(String userId)` - Retrieves user document
- **Update User:** `updateUser(UserProfile/Users user)` - Updates user document
- **Delete User:** `deleteUser(String userId)` - Deletes user document
- **Get User by Device ID:** `getUserByDeviceId(String deviceId)` - Finds user by device ID
- **Get All Users:** `getAllUsers()` - Retrieves all users (admin use)

### Queries

- `whereEqualTo("deviceId", deviceId)` - Find user by device ID
- `whereEqualTo("role", role)` - Filter by role (if needed)

---

## 2. Notifications Subcollection

**Path:** `/users/{userId}/notifications/{notificationId}`

**Repository:** `NotificationRepository.java`

**Model Class:** `Notification.java`

### Document Structure

```java
{
  notificationId: String,            // Document ID (auto-generated)
  userId: String,                    // User ID this notification is for
  eventId: String,                   // Event ID this notification relates to
  type: String,                      // "INVITED", "LOST", "ACCEPTED", "CANCELLED"
  title: String,                     // Notification title
  message: String,                   // Notification message
  timestamp: Timestamp,              // When notification was created
  read: Boolean                      // Whether notification has been read (default: false)
}
```

### Key Operations

- **Create Notification:** `createNotification(String userId, Notification notification)` - Creates notification document
- **Get All Notifications:** `getNotificationsForUser(String userId)` - Gets all notifications for a user, ordered by timestamp DESC
- **Get Unread Notifications:** `getUnreadNotificationsForUser(String userId)` - Gets all notifications (filtered in code for unread)
- **Mark as Read:** `markAsRead(String userId, String notificationId)` - Updates `read` field to `true`
- **Get by ID:** `getNotificationById(String userId, String notificationId)` - Retrieves specific notification

### Queries

- `orderBy("timestamp", Query.Direction.DESCENDING)` - Order by creation time
- **Note:** Unread filtering is done in application code to avoid composite index requirement

### Important Notes

- **Notifications are NOT deleted** when dismissed - they are only marked as `read: true`
- This allows admins to review notification logs (US 03.08.01)
- Notifications respect user's `notificationOptOut` preference before creation

---

## 3. Events Collection

**Path:** `/events/{eventId}`

**Repository:** `EventRepository.java`

**Model Class:** `Event.java`

### Document Structure

```java
{
  eventId: String,                   // Document ID (auto-generated or specified)
  organizerId: String,               // Reference to UserProfile (organizer's userId)
  eventName: String,                 // Event name
  description: String,               // Event description
  geolocation: GeoPoint,             // Optional geographic location
  posterImageUrl: String,            // Optional URL to event poster image
  eventDate: Timestamp,              // Scheduled date/time of event
  location: String,                  // Human-readable venue/location
  cost: Double,                      // Optional price/cost (nullable for free)
  registrationStartDate: Timestamp,  // When registration opens
  registrationEndDate: Timestamp,    // When registration closes
  maxEntrants: Integer,              // Optional maximum number of entrants
  numberOfWinners: Integer,          // Number of winners to draw
  geolocationRequired: Boolean,      // Whether geolocation check-in is required
  status: String,                    // "DRAFT", "OPEN", "CLOSED", "DRAWN", "COMPLETED"
  qrCodeImageBase64: String,         // Base64-encoded QR code image
  qrStyleVersion: Integer            // QR code style version
}
```

### Key Operations

- **Create Event:** `createEvent(Event event)` - Creates new event document (auto-generates ID if null)
- **Get Event by ID:** `getEventById(String eventId)` - Retrieves event document
- **Update Event:** `updateEvent(Event event)` - Updates entire event document
- **Update Event Fields:** `updateEventFields(String eventId, Map<String, Object> updates)` - Partially updates event
- **Delete Event:** `deleteEvent(String eventId)` - Deletes event document
- **Get All Events:** `getAllEvents()` - Retrieves all events
- **Get Events by Organizer:** `getEventsByOrganizer(String organizerId)` - Gets events for specific organizer
- **Get Open Events:** `getOpenEvents()` - Gets events with status "OPEN"

### Queries

- `whereEqualTo("organizerId", organizerId)` - Filter by organizer
- `whereEqualTo("status", "OPEN")` - Filter by status

### Important Notes

- Waiting list and decisions are stored as subcollections, not in the Event document
- The `Event` model class has in-memory `waitingList` and `decisions` lists for OOP composition, but these are NOT saved to Firestore

---

## 4. Waiting List Subcollection

**Path:** `/events/{eventId}/waitingList/{entryId}`

**Repository:** `WaitingListRepository.java`

**Model Class:** `WaitingListEntry.java`

### Document Structure

```java
{
  entryId: String,                   // Document ID (auto-generated)
  entrantId: String,                 // Reference to UserProfile (entrant's userId)
  joinTimestamp: Timestamp,          // When entrant joined waiting list
  invitedAt: Timestamp               // Optional: when organizer invited this entrant
}
```

### Key Operations

- **Add to Waiting List:** `addToWaitingList(String eventId, WaitingListEntry entry)` - Creates new waiting list entry
- **Get Waiting List for Event:** `getWaitingListForEvent(String eventId)` - Gets all entries for an event
- **Get Entry by ID:** `getWaitingListEntry(String eventId, String entryId)` - Retrieves specific entry
- **Check User in Waiting List:** `checkUserInWaitingList(String eventId, String entrantId)` - Checks if user already registered
- **Remove from Waiting List:** `removeFromWaitingList(String eventId, String entryId)` - Deletes entry
- **Update Entry:** `updateWaitingListEntry(String eventId, String entryId, WaitingListEntry entry)` - Updates entry
- **Get Entries by User:** `getWaitingListEntriesByUser(String entrantId)` - **Collection Group Query** - Gets all events user registered for

### Queries

- `whereEqualTo("entrantId", entrantId)` - Filter by entrant
- **Collection Group Query:** `collectionGroup("waitingList").whereEqualTo("entrantId", entrantId)` - Query across all events

### Required Indexes

**Collection Group Index for `waitingList`:**
- Collection ID: `waitingList` (collection group)
- Fields: `entrantId` (Ascending)
- **Required for:** `getWaitingListEntriesByUser()` method

---

## 5. Decisions Subcollection

**Path:** `/events/{eventId}/decisions/{decisionId}`

**Repository:** `DecisionRepository.java`

**Model Class:** `Decision.java`

### Document Structure

```java
{
  decisionId: String,                // Document ID (auto-generated)
  entrantId: String,                 // Reference to UserProfile (entrant's userId)
  entryId: String,                   // Reference to WaitingListEntry
  status: String,                    // "PENDING", "INVITED", "ACCEPTED", "DECLINED", "CANCELLED"
  respondedAt: Timestamp,            // Optional: timestamp of entrant response
  updatedAt: Timestamp               // Last update timestamp
}
```

### Status Values

- **"PENDING"** - Initial state, waiting for lottery draw
- **"INVITED"** - Selected in lottery, invitation sent
- **"ACCEPTED"** - Entrant accepted invitation
- **"DECLINED"** - Entrant declined invitation
- **"CANCELLED"** - Organizer cancelled this entrant
- **"LOST"** - Not selected in lottery (may be used in notifications)

### Key Operations

- **Create Decision:** `createDecision(String eventId, Decision decision)` - Creates new decision document
- **Get Decisions for Event:** `getDecisionsForEvent(String eventId)` - Gets all decisions for an event
- **Get Decision for User:** `getDecisionForUser(String eventId, String entrantId)` - Gets user's decision for an event
- **Get Decision by ID:** `getDecisionById(String eventId, String decisionId)` - Retrieves specific decision
- **Update Decision:** `updateDecision(String eventId, String decisionId, Decision decision)` - Updates decision
- **Delete Decision:** `deleteDecision(String eventId, String decisionId)` - Deletes decision
- **Get Decisions by User:** `getDecisionsByUser(String entrantId)` - **Collection Group Query** - Gets all decisions for a user across all events
- **Get Decisions by Status:** `getDecisionsByStatus(String eventId, String status)` - Gets decisions with specific status

### Queries

- `whereEqualTo("entrantId", entrantId)` - Filter by entrant
- `whereEqualTo("status", status)` - Filter by status
- **Collection Group Query:** `collectionGroup("decisions").whereEqualTo("entrantId", entrantId)` - Query across all events

### Required Indexes

**Collection Group Index for `decisions`:**
- Collection ID: `decisions` (collection group)
- Fields: `entrantId` (Ascending)
- **Required for:** `getDecisionsByUser()` method

---

## Collection Relationships

### User → Events
- Users can register for multiple events (tracked via `registeredEventIds` array in User document)
- Users can be organizers of multiple events (tracked via `organizerId` field in Event documents)

### Event → Waiting List
- Each event has a `waitingList` subcollection
- Multiple users can join the same event's waiting list
- One `WaitingListEntry` per user per event

### Event → Decisions
- Each event has a `decisions` subcollection
- Decisions are created when lottery is drawn
- One `Decision` per user per event (linked to `WaitingListEntry` via `entryId`)

### User → Notifications
- Each user has a `notifications` subcollection
- Notifications are created when:
  - Lottery is drawn (winners/losers)
  - Organizer sends bulk notifications
- Notifications reference events via `eventId`

### Decision → Notification
- Decisions trigger notifications (e.g., "INVITED" status → winner notification)
- Notification `type` often matches Decision `status`

---

## Required Firestore Indexes

### 1. Collection Group Index: `waitingList`
- **Collection ID:** `waitingList` (collection group)
- **Fields:** `entrantId` (Ascending)
- **Purpose:** Query all events a user has registered for
- **Used by:** `WaitingListRepository.getWaitingListEntriesByUser()`

### 2. Collection Group Index: `decisions`
- **Collection ID:** `decisions` (collection group)
- **Fields:** `entrantId` (Ascending)
- **Purpose:** Query all decisions for a user across all events
- **Used by:** `DecisionRepository.getDecisionsByUser()`

### 3. Composite Index (Optional - Avoided)
- **Collection:** `users/{userId}/notifications`
- **Fields:** `read` (Ascending), `timestamp` (Descending)
- **Status:** **NOT REQUIRED** - Application filters unread in code to avoid this index
- **Note:** The code intentionally avoids this composite index by fetching all notifications and filtering in application code

---

## Data Flow Examples

### User Joins Waiting List
1. Create `WaitingListEntry` in `/events/{eventId}/waitingList/{entryId}`
2. Add `eventId` to user's `registeredEventIds` array (workaround)

### Lottery Draw
1. Query `waitingList` subcollection for event
2. Randomly select `numberOfWinners` entries
3. Create `Decision` documents in `/events/{eventId}/decisions/{decisionId}`:
   - Winners: `status = "INVITED"`
   - Losers: `status = "LOST"` (or "PENDING")
4. Create `Notification` documents in `/users/{userId}/notifications/{notificationId}`:
   - Winners: `type = "INVITED"`
   - Losers: `type = "LOST"`

### User Accepts Invitation
1. Update `Decision` document: `status = "ACCEPTED"`, set `respondedAt`
2. Optionally create notification: `type = "ACCEPTED"`

### Organizer Sends Bulk Notification
1. Query `decisions` subcollection by status (e.g., "INVITED", "ACCEPTED")
2. For each decision, create `Notification` in `/users/{entrantId}/notifications/{notificationId}`
3. Respects user's `notificationOptOut` preference

---

## Best Practices

1. **Subcollections vs Arrays:** Use subcollections for one-to-many relationships that may grow large (waiting lists, decisions, notifications)

2. **Collection Group Queries:** Use for cross-event queries (e.g., "all events user registered for"), but require indexes

3. **Notification Persistence:** Notifications are never deleted, only marked as read, to support admin notification logs

4. **Status Consistency:** Decision statuses and notification types should be kept in sync

5. **Index Management:** Create required indexes in Firebase Console before deploying features that use collection group queries

---

## Summary Table

| Collection/Subcollection | Path | Model Class | Repository | Key Use Case |
|-------------------------|------|-------------|------------|--------------|
| **users** | `/users/{userId}` | `UserProfile`, `Users` | `UserRepository` | User profiles and authentication |
| **notifications** | `/users/{userId}/notifications/{notificationId}` | `Notification` | `NotificationRepository` | User notifications |
| **events** | `/events/{eventId}` | `Event` | `EventRepository` | Event information |
| **waitingList** | `/events/{eventId}/waitingList/{entryId}` | `WaitingListEntry` | `WaitingListRepository` | Event registration queue |
| **decisions** | `/events/{eventId}/decisions/{decisionId}` | `Decision` | `DecisionRepository` | Lottery results and responses |

---

**Last Updated:** 2025-11-19
**Version:** 1.0

