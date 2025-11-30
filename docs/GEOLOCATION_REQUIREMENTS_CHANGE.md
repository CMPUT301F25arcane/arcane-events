# Geolocation Requirements Change - New Logic and Restructured Commits

## 📋 Requirements Change Summary

**OLD APPROACH (Commits 1-7):**
- User logs in → Dialog appears asking "Enable location tracking?"
- User chooses Allow/Don't Allow
- Preference saved to `Users.locationTrackingEnabled` in Firestore
- When user joins event → Check preference → If enabled, capture location at that moment

**NEW APPROACH (Updated):**
- User logs in → **Automatically capture location** (no dialog, no user choice)
- Location stored in **session** (SharedPreferences) for the login session
- When user joins event → Use **session location** (captured at login) → Store in `WaitingListEntry.joinLocation`
- Same session location used for all events joined during that session

---

## 🔄 New Logic Flow

### **1. User Login Flow**

```
User logs in
  ↓
LoginFragment.routeByRole() called
  ↓
Check if location permission granted
  ↓
If YES:
  → Automatically capture location using LocationService
  → Store location in SharedPreferences (session storage)
  → Key: "session_location_lat" and "session_location_lng"
  → Navigate to home
  ↓
If NO:
  → Request location permission (system dialog, not our custom dialog)
  → After permission granted → Capture location → Store in session
  → Navigate to home
```

**Key Points:**
- **NO custom dialog** - We don't ask user's preference
- **Automatic capture** - Location is captured immediately after login
- **Session-based** - Location stored in SharedPreferences (cleared on logout)
- **One location per session** - Same location used for all events joined during that session

---

### **2. User Joins Event Flow**

```
User clicks "Join Waitlist" button
  ↓
EventDetailFragment.handleJoinWaitlist() called
  ↓
Get session location from SharedPreferences
  ↓
If session location exists:
  → Create WaitingListEntry with joinLocation = session location
  → Call EventService.joinWaitingList(eventId, userId, sessionLocation)
  ↓
If session location doesn't exist (shouldn't happen, but handle gracefully):
  → Create WaitingListEntry with joinLocation = null
  → Log warning
  ↓
EventService.addUserToWaitingList() creates entry
  ↓
WaitingListEntry.joinLocation stored in Firestore
```

**Key Points:**
- **Uses session location** - Not captured at join time, uses location from login
- **Same location for all events** - If user joins Event A and Event B in same session, both use same location
- **No permission check at join** - Permission already handled at login

---

### **3. Organizer Views Entrants Map Flow**

```
Organizer clicks event card
  ↓
EventDetailFragment (organizer view)
  ↓
Clicks "Show Entrants" button
  ↓
EntrantsFragment loads all WaitingListEntry documents
  ↓
Clicks "View Map" button
  ↓
EntrantsMapFragment loads:
  → All WaitingListEntry documents for this event
  → Filter: Only entries where joinLocation != null
  → Display: One marker per entrant's join location
  → Also show: Event location marker (if event.geolocation exists)
```

**Key Points:**
- **No filtering by user preference** - All entries with `joinLocation` are shown
- **Shows all entrant locations** - Every entrant who joined during a session with location capture
- **Event location also shown** - Different marker/color for event location

---

### **4. Legacy Events Handling**

**Problem:**
- Events created before geolocation feature don't have `Event.geolocation`
- Entrants from before the feature don't have `WaitingListEntry.joinLocation`

**Solution:**
- **EventDetailFragment:** If `event.geolocation == null`, show "Location: Unknown" (no map)
- **EntrantsMapFragment:** If `entry.joinLocation == null`, skip that entrant (no marker)
- **Event Cards:** Show "Unknown" chip/tag if no geolocation

---

## 📊 Who Sees What - Complete Breakdown

### **Regular User Views Event Card:**

**What they see:**
1. **Event location name** (text) - Always shown: `event.location` (e.g., "123 Main St, New York")
2. **Event location on map** (Commit 18) - Small map view:
   - If `event.geolocation != null`: Show map with event location marker
   - If `event.geolocation == null`: Show "Location: Unknown" (no map)
3. **Other event details** - Name, description, date, cost, etc.
4. **Join/Abandon button** - Based on their status

**Flow:**
```
User clicks event card (from My Events or Global Events)
  ↓
EventDetailFragment (user view)
  ↓
Shows:
  - Event name, description, date
  - Location: "123 Main St" (text)
  - [Map View] (if geolocation exists, shows event location marker)
  - Cost: $50
  - Status: WAITING
  - [Join Waitlist] button
```

---

### **Organizer Views Their Event:**

**What they see:**
1. **Event location name** (text) - Same as users: `event.location`
2. **Event location on map** (Commit 18) - Same as users: Map with event location marker (if geolocation exists)
3. **Organizer-specific actions:**
   - "Draw Lottery" button
   - "Show Entrants" button → EntrantsFragment
   - "Edit Event" button
   - "Send Notification" button

**Flow:**
```
Organizer clicks their event card (from My Events)
  ↓
EventDetailFragment (organizer view)
  ↓
Shows:
  - Event name, description, date
  - Location: "123 Main St" (text)
  - [Map View] (if geolocation exists, shows event location marker)
  - Cost: $50
  - [Draw Lottery] button
  - [Show Entrants] button
  - [Edit Event] button
  ↓
Clicks "Show Entrants"
  ↓
EntrantsFragment (list of entrants with status)
  ↓
Clicks "View Map" button
  ↓
EntrantsMapFragment (map showing all entrant join locations)
```

---

### **Organizer Views Entrants Map (US 02.02.02):**

**What they see:**
- **All entrant join locations** on one map (no filtering by user)
- **Event location marker** (different color/icon)
- **Clicking a marker** shows entrant name
- **Zoom/pan** functionality

**Key Points:**
- **All entrants together** - Not filtered by user, all shown on same map
- **One marker per entrant** - Each `WaitingListEntry` with `joinLocation != null` gets a marker
- **Event location also shown** - Helps organizer see where event is vs where entrants joined from

---

## 🎯 Remaining User Stories

### **US 02.02.02 — Organizer sees entrants on a map**
**Status:** Will be implemented in Commits 15-17

**How it works:**
- Organizer clicks event → "Show Entrants" → "View Map"
- Map shows all entrant join locations (from `WaitingListEntry.joinLocation`)
- Event location also displayed
- All entrants shown together (no filtering)

**Implementation:**
- `EntrantsMapFragment` loads all `WaitingListEntry` documents for the event
- Filters entries where `joinLocation != null`
- Displays markers for each entrant's join location
- Different marker for event location

---

### **US 02.02.03 — Organizer enables/disables geolocation requirement**
**Status:** Will be implemented in Commit 13

**How it works:**
- **Where:** In `CreateEventFragment` (event creation form)
- **What:** Toggle/checkbox: "Require geolocation for this event"
- **Stored in:** `Event.geolocationRequired` field (Boolean)
- **Implications:**
  - **If `true`:** When user joins this event, their session location IS stored in `WaitingListEntry.joinLocation`
  - **If `false`:** When user joins this event, their session location is NOT stored (set `joinLocation = null`)
  - **Important:** Location is always captured at login and stored in session (separate from event join)
  - **Logic:** Session location exists independently; event's `geolocationRequired` determines if it gets stored when joining

**Implementation:**
- Add toggle/checkbox in `CreateEventFragment` layout
- Save `geolocationRequired` value when creating event
- Display in event detail page (organizer view)
- **Commit 9:** Check `event.geolocationRequired` in `EventService.joinWaitingList()`:
  - If `true`: Store session location in `WaitingListEntry.joinLocation`
  - If `false`: Set `joinLocation = null` (don't store)

---

## 📝 Restructured Commits

### **Phase 1: Foundation (Commits 1-6) - NO CHANGES**
- ✅ Commit 1: Add location permissions to AndroidManifest
- ✅ Commit 2: Add geolocation field to WaitingListEntry model
- ✅ Commit 3: Add location tracking preference to Users model (still needed for backward compatibility, but not used for new logic)
- ✅ Commit 4: Add Google Maps SDK dependency
- ✅ Commit 5: Create LocationPermissionHelper utility class
- ✅ Commit 6: Create LocationService utility class

---

### **Phase 2: Session Location Management (Commits 7-8) - CHANGED**

#### **Commit 7: Auto-capture location on login (REPLACES old Commit 7)**

**What was done (OLD):**
- Added location permission dialog on login
- User chooses Allow/Don't Allow
- Preference saved to Firestore

**What will be done (NEW):**
- **Remove** location permission dialog
- **Automatically** request location permission (system dialog, not custom)
- **Automatically** capture location after permission granted
- **Store** location in SharedPreferences (session storage)
- **Keys:** `"session_location_lat"` and `"session_location_lng"` (as doubles)
- **Clear** session location on logout

**Why this is important:**
- **Problem solved:** Users don't need to make a choice - location is automatically captured for the session
- **Simpler UX:** No dialog interruption, seamless experience
- **Session-based:** One location per login session, used for all events joined

**How it solves our overall problem:**
- **Foundation for location capture:** Every user who logs in gets their location captured
- **Enables event join:** When user joins event, we use this session location
- **No user friction:** No dialogs, no choices, just works

**Files to modify:**
- `LoginFragment.java` - Remove dialog, add auto-capture logic
- `NotificationsFragment.java` - Clear session location on logout
- `LocationService.java` - Already exists, will be used

**Commit message:**
```
feat: Auto-capture user location on login for session

- Remove location permission dialog (no user choice needed)
- Automatically request location permission after login
- Capture location using LocationService
- Store location in SharedPreferences (session storage)
- Clear session location on logout
- Location will be used when user joins events during this session
```

---

#### **Commit 8: Create SessionLocationManager utility class (NEW)**

**What will be done:**
- Create `SessionLocationManager.java` utility class
- Methods to:
  - `saveSessionLocation(GeoPoint location)` - Store in SharedPreferences
  - `getSessionLocation(Context context)` - Retrieve from SharedPreferences
  - `clearSessionLocation(Context context)` - Clear on logout
  - `hasSessionLocation(Context context)` - Check if location exists

**Why this is important:**
- **Problem solved:** Centralized location for session location management
- **Code reusability:** One place to handle session location storage/retrieval
- **Consistency:** All fragments use same methods

**How it solves our overall problem:**
- **Clean abstraction:** Fragments don't need to know about SharedPreferences keys
- **Easy to use:** Simple methods for save/get/clear
- **Maintainable:** If we change storage mechanism, only one file to update

**Files to create:**
- `app/src/main/java/com/example/arcane/util/SessionLocationManager.java`

**Commit message:**
```
feat: Create SessionLocationManager utility for session location storage

- Centralized utility for managing session location in SharedPreferences
- Methods: saveSessionLocation(), getSessionLocation(), clearSessionLocation()
- Used by LoginFragment to store location and EventDetailFragment to retrieve it
```

---

### **Phase 3: Capture Location on Join Waitlist (Commits 9-11) - CHANGED**

#### **Commit 9: Update EventService to use session location on join (CHANGED)**

**What was done (OLD):**
- Update `EventService.joinWaitingList()` to capture location at join time
- Check user preference before capturing

**What will be done (NEW):**
- Update `EventService.joinWaitingList()` to **accept session location as parameter**
- **Check `event.geolocationRequired`** before storing location:
  - If `true`: Store session location in `WaitingListEntry.joinLocation`
  - If `false`: Set `joinLocation = null` (don't store location for this event)
- **Use session location** (passed from fragment) instead of capturing at join time

**Why this is important:**
- **Problem solved:** Uses location captured at login, not at join time
- **Respects organizer preference:** Only stores location if event requires it (`geolocationRequired = true`)
- **Consistent:** Same session location used for all events that require it
- **Privacy-aware:** Organizers can disable location tracking for specific events

**How it solves our overall problem:**
- **Data storage:** Entrant join locations are stored in database (only for events that require it)
- **Enables map feature:** Organizers can see where entrants joined from (for events with location enabled)
- **Session-based:** Location represents where user was when they logged in
- **US 02.02.03:** Implements organizer's ability to enable/disable geolocation requirement per event

**Files to modify:**
- `EventService.java` - Update `joinWaitingList()` signature to accept `GeoPoint sessionLocation`
- `EventService.java` - Check `event.geolocationRequired` before storing location
- `EventService.java` - Update `addUserToWaitingList()` to accept and conditionally store `joinLocation`

**Commit message:**
```
feat: Use session location when user joins event waitlist

- Update EventService.joinWaitingList() to accept session location parameter
- Store session location in WaitingListEntry.joinLocation
- Location captured at login is reused for all events joined in that session
```

---

#### **Commit 10: Remove location validation (CANCELLED)**

**What was planned (OLD):**
- Add location validation in EventService
- Check if event requires geolocation

**What will be done (NEW):**
- **CANCEL this commit** - Not needed with new requirements
- We always capture location, no validation needed

**Status:** ❌ CANCELLED

---

#### **Commit 11: Update EventDetailFragment to pass session location on join (CHANGED)**

**What was done (OLD):**
- Update `EventDetailFragment` to request location permission on join
- Capture location at join time

**What will be done (NEW):**
- Update `EventDetailFragment.handleJoinWaitlist()` to:
  - Get session location from `SessionLocationManager`
  - Pass session location to `EventService.joinWaitingList()`
  - Handle case where session location doesn't exist (shouldn't happen, but graceful fallback)

**Why this is important:**
- **Problem solved:** Connects session location to event join flow
- **Data flow:** Login → Session location stored → Join event → Session location used

**How it solves our overall problem:**
- **Complete flow:** User logs in → Location captured → User joins event → Location stored
- **Enables map feature:** Entrant locations are now stored in database

**Files to modify:**
- `EventDetailFragment.java` - Update `handleJoinWaitlist()` to get and pass session location

**Commit message:**
```
feat: Pass session location to EventService when joining waitlist

- Get session location from SessionLocationManager in EventDetailFragment
- Pass location to EventService.joinWaitingList()
- Handle graceful fallback if session location missing
- Completes location capture flow: login → session storage → event join
```

---

### **Phase 4: Event Creation with Location (Commits 12-14) - NO CHANGES**

- ✅ Commit 12: Add Google Places Autocomplete to CreateEventFragment
- ✅ Commit 13: Update CreateEventFragment to save event geolocation
- ✅ Commit 14: Show "Unknown" location for legacy events

---

### **Phase 5: Map Display Functionality (Commits 15-20) - NO CHANGES**

- ✅ Commit 15: Create EntrantsMapFragment
- ✅ Commit 16: Add map navigation to EntrantsFragment
- ✅ Commit 17: Implement map marker display logic
- ✅ Commit 18: Add map view to event detail page
- ✅ Commit 19: Add location chip/tag to event cards
- ✅ Commit 20: Update navigation graph for map fragments

---

## 📊 Updated Commit Summary

| Commit | Status | What Changed |
|--------|--------|---------------|
| 1-6 | ✅ COMPLETED | No changes needed |
| **7** | 🔄 **CHANGED** | **Remove dialog, auto-capture location on login** |
| **8** | 🆕 **NEW** | **Create SessionLocationManager utility** |
| **9** | 🔄 **CHANGED** | **Use session location instead of capturing at join** |
| **10** | ❌ **CANCELLED** | Not needed with new requirements |
| **11** | 🔄 **CHANGED** | **Pass session location to EventService** |
| 12-20 | 📋 **PLANNED** | No changes needed |

---

## 🔑 Key Differences from Original Plan

1. **No user choice dialog** - Location is automatically captured, no opt-in/opt-out
2. **Session-based location** - One location per login session, stored in SharedPreferences
3. **Location captured at login** - Not at event join time
4. **Same location for all events** - If user joins multiple events in same session, all use same location
5. **No location validation** - We always capture if permission granted, no need to validate
6. **Users.locationTrackingEnabled field** - Still exists for backward compatibility, but not used in new flow

---

## ✅ Understanding Confirmation

**I understand:**
- ✅ No location permission dialog at login
- ✅ Location automatically captured on login (if permission granted)
- ✅ Location stored in session (SharedPreferences)
- ✅ When user joins event, session location is used and stored in `WaitingListEntry.joinLocation`
- ✅ Same session location used for all events joined during that session
- ✅ All remaining user stories and features remain the same
- ✅ Commits 1-6, 12-20 remain unchanged
- ✅ Commits 7, 9, 11 are restructured
- ✅ Commit 8 is new (SessionLocationManager)
- ✅ Commit 10 is cancelled

**Ready to proceed with implementation when you give the go-ahead!**

