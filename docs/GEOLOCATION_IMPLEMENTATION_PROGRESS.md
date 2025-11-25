# Geolocation and Map Feature - Implementation Progress

## Overview
This document tracks the implementation of geolocation and map features for the Arcane Events app, including address autocomplete for event creation and map visualization of entrant locations.

## ⚠️ Requirements Change Notice

**IMPORTANT:** Requirements have changed. See [GEOLOCATION_REQUIREMENTS_CHANGE.md](./GEOLOCATION_REQUIREMENTS_CHANGE.md) for complete details.

**Key Changes:**
- ❌ **REMOVED:** Location permission dialog at login (no user choice)
- ✅ **NEW:** Automatic location capture on login (stored in session)
- ✅ **NEW:** Session-based location (one location per login, used for all events)
- ✅ **NEW:** Location captured at login, not at event join time

**Commit 7 Status:** 🔄 **NEEDS RESTRUCTURING** - See requirements change document for new approach.

---

## ✅ Commit 1: Add Location Permissions to AndroidManifest

**What was done:**
- Added `ACCESS_FINE_LOCATION` permission (for precise GPS location)
- Added `ACCESS_COARSE_LOCATION` permission (for approximate location, required for Android 12+)

**Why this is important:**
- **Problem solved:** Without these permissions declared in the manifest, Android will not allow our app to access the device's location services at all. This is like asking for permission to enter a building - we need to declare our intent first before we can even ask the user.

**How it solves our overall problem:**
- This is the foundation that enables all location features:
  - Users can share their location when joining events
  - Organizers can see where entrants joined from on a map
  - Event locations can be geocoded and displayed on maps
  - Address autocomplete will work for event creation

**Files modified:**
- `app/src/main/AndroidManifest.xml`

**Status:** ✅ COMPLETED

---

## ✅ Commit 2: Add Geolocation Field to WaitingListEntry Model

**What was done:**
- Added `joinLocation` field (GeoPoint type) to store where a user was when they joined an event's waitlist
- Updated constructor to accept `joinLocation` parameter
- Added getter and setter methods for the new field
- Updated class documentation to reflect the new field

**Why this is important:**
- **Problem solved:** Previously, we could only track WHEN someone joined (timestamp) but not WHERE they joined. This field stores the GPS coordinates of where the user was when they clicked "Join Waitlist".

**How it solves our overall problem:**
- This is the data storage layer that enables the map feature:
  - When a user joins an event, we can capture their location and store it here
  - Organizers can later retrieve all these locations to display on a map
  - Shows geographic distribution of event participants (e.g., "Most people joined from downtown")
  - Helps organizers understand where their event audience is located

**Files modified:**
- `app/src/main/java/com/example/arcane/model/WaitingListEntry.java`

**Status:** ✅ COMPLETED

---

## ✅ Commit 3: Add Location Tracking Preference to Users Model

**What was done:**
- Added `locationTrackingEnabled` field (Boolean type) to track if user has opted-in to location tracking
- Set default value to `false` in constructor (privacy-first approach)
- Added getter and setter methods with null-safety handling
- Updated class to store user's location tracking preference

**Why this is important:**
- **Problem solved:** We need to know if a user has given permission to track their location. Without this field, we can't distinguish between "user hasn't been asked yet" vs "user said no" vs "user said yes". This is like a checkbox that remembers the user's choice.

**How it solves our overall problem:**
- **Privacy control:** Users must explicitly opt-in (default is false) - we never track location without permission
- **Conditional logic:** We can check this field before capturing location:
  - If `true`: Capture and store location when joining events
  - If `false`: Don't capture location, show message if event requires it
- **Organizer visibility:** Only show entrant locations on map if they enabled tracking
- **Compliance:** Ensures we respect user privacy preferences

**Files modified:**
- `app/src/main/java/com/example/arcane/model/Users.java`

**Status:** ✅ COMPLETED

---

## ✅ Commit 4: Add Google Maps SDK Dependency

**What was done:**
- Added Google Maps SDK dependency (`com.google.android.gms:play-services-maps:18.2.0`) to `build.gradle.kts`
- Added Google Places SDK dependency (`com.google.android.libraries.places:places:3.3.0`) to `build.gradle.kts`
- Added Maps API key placeholder in `AndroidManifest.xml` with instructions for developers

**Why this is important:**
- **Problem solved:** Without these SDK libraries, our app cannot display maps or use address autocomplete features. Think of it like installing a GPS navigation app on your phone - you need the app installed before you can use it. Similarly, we need these libraries "installed" in our project before we can write code that uses maps and places.

**How it solves our overall problem:**
- **Maps SDK enables:**
  - Displaying interactive maps with zoom, pan, and marker functionality (for showing entrant locations)
  - Showing event locations on maps
  - Creating custom map views for organizers to see where participants joined from
- **Places SDK enables:**
  - Address autocomplete when organizers type event addresses (real-time suggestions as they type)
  - Converting addresses to GPS coordinates automatically
  - Making event creation faster and more accurate (no typos in addresses)
- **Foundation for future commits:**
  - Commit 12 will use Places SDK for address autocomplete in event creation
  - Commits 15-18 will use Maps SDK to display entrant locations and event maps
  - Without this commit, none of the map features would work

**Files modified:**
- `app/build.gradle.kts` (added dependencies)
- `app/src/main/AndroidManifest.xml` (added API key placeholder)

**Note for developers:**
- Before running the app with map features, you must:
  1. Get a Google Maps API key from [Google Cloud Console](https://console.cloud.google.com/google/maps-apis)
     - Note: Your Firebase project IS a Google Cloud project (they share the same project ID)
     - You can access it via Firebase Console → Project Settings → Google Cloud Platform → Open Google Cloud Console
  2. Replace `YOUR_API_KEY` in `AndroidManifest.xml` with your actual API key
  3. Enable "Maps SDK for Android" and "Places API" in your Firebase/Google Cloud project:
     - Go to [Google Cloud Console APIs Library](https://console.cloud.google.com/apis/library)
     - Select your Firebase project
     - Search for and enable "Maps SDK for Android"
     - Search for and enable "Places API"

**Status:** ✅ COMPLETED

---

## ✅ Commit 5: Create LocationPermissionHelper Utility Class

**What was done:**
- Created `LocationPermissionHelper.java` utility class in `util` package
- Added methods to check if location permissions are granted
- Added methods to request location permissions from Fragment or Activity
- Added helper methods to verify permission results
- Defined constant for permission request code

**Why this is important:**
- **Problem solved:** Android's permission system is complex - you need to check permissions, request them, and handle results. Without a helper class, we'd have to write the same permission-checking code in multiple places (login dialog, event join, etc.), which leads to code duplication and bugs. This is like having a toolbox - instead of carrying around individual tools, we have one organized box with everything we need.

**How it solves our overall problem:**
- **Code reusability:** One place to handle all location permission logic - any fragment or activity can use these methods
- **Consistency:** All permission requests work the same way across the app
- **Simplifies future commits:**
  - Commit 7 (location dialog) will use `requestLocationPermission()` to ask for permission
  - Commit 11 (join waitlist) will use `hasLocationPermission()` to check before capturing location
  - All map features will use these helpers to ensure permissions are granted
- **Error prevention:** Centralized logic means fewer bugs - if we fix a permission issue, it's fixed everywhere

**Key methods:**
- `hasLocationPermission()` - Check if app has location permission
- `hasFineLocationPermission()` - Check for precise GPS permission
- `requestLocationPermission()` - Request permissions from Fragment or Activity
- `isLocationPermissionGranted()` - Verify permission results
- `isLocationPermissionRequest()` - Check if callback is for location permissions

**Files created:**
- `app/src/main/java/com/example/arcane/util/LocationPermissionHelper.java`

**Status:** ✅ COMPLETED

---

## ✅ Commit 6: Create LocationService Utility Class

**What was done:**
- Created `LocationService.java` utility class in `util` package
- Added Google Location Services dependency (`play-services-location:21.0.1`) to `build.gradle.kts`
- Implemented methods to get device's current location using FusedLocationProviderClient
- Added callback interface for handling location results
- Added helper methods to convert between Android Location and Firestore GeoPoint
- Provided both high-accuracy and balanced power/accuracy location retrieval options

**Why this is important:**
- **Problem solved:** Getting GPS coordinates from a device is complex - you need to use Google Play Services, handle callbacks, convert data formats, and manage errors. Without a service class, every place that needs location would have to write this complex code, leading to bugs and inconsistency. This is like having a GPS device - instead of building your own GPS from scratch, you use a ready-made one that works reliably.

**How it solves our overall problem:**
- **Location retrieval:** Provides a simple way to get the device's current GPS coordinates
- **Data conversion:** Automatically converts Android Location to Firestore GeoPoint (the format we store in database)
- **Error handling:** Centralized error handling means consistent behavior across the app
- **Power efficiency:** Offers both high-accuracy and balanced modes (balanced uses less battery)
- **Enables future commits:**
  - Commit 9 (capture location on join) will use `getCurrentLocation()` when user joins waitlist
  - Commit 11 (request location on join) will use this service to get coordinates
  - All location capture features will use this service consistently

**Key methods:**
- `getCurrentLocation()` - Get high-accuracy GPS location
- `getCurrentLocationBalanced()` - Get location with balanced power/accuracy
- `locationToGeoPoint()` - Convert Android Location to Firestore GeoPoint
- `geoPointToString()` - Convert GeoPoint to string for logging/display
- `LocationCallback` interface - Handle location results (success/failure)

**Files created:**
- `app/src/main/java/com/example/arcane/util/LocationService.java`

**Files modified:**
- `app/build.gradle.kts` (added location services dependency)

**Status:** ✅ COMPLETED

---

## ✅ Commit 7: Auto-capture Location on Login

**What was done:**
- **Removed** location permission dialog (no user choice needed)
- **Automatically** requests location permission (system dialog) after successful login
- **Automatically** captures location after permission is granted
- **Stores** location in session using `SessionLocationManager` (SharedPreferences)
- **Clears** session location on logout in `NotificationsFragment`

**Why this is important:**
- **Problem solved:** Location is automatically captured for the session - no user choice needed. Without this, we can't capture location at login, and users would need to manually enable it each time.
- **Simpler UX:** No dialog interruption, seamless experience - users just log in and location is captured automatically
- **Session-based:** One location per login session, used for all events joined during that session

**How it solves our overall problem:**
- **Foundation for location capture:** Every user who logs in gets their location captured automatically
- **Enables event join:** When user joins event (Commit 11), we use this session location
- **No user friction:** No dialogs, no choices, just works - location is captured in the background
- **Privacy-aware:** Location is only stored in session (cleared on logout), not persisted permanently

**Key features:**
- Checks if location permission is already granted
- If yes: Captures location immediately and stores in session
- If no: Requests permission, then captures location after permission is granted
- Handles permission denial gracefully (continues without location)
- Location capture is optional - app works even if location can't be captured

**Files modified:**
- `LoginFragment.java` - Removed dialog code, added auto-capture logic using `LocationService` and `SessionLocationManager`
- `NotificationsFragment.java` - Added `SessionLocationManager.clearSessionLocation()` call in `clearCachedUserRole()` method

**Status:** ✅ COMPLETED

---

## ✅ Commit 8: Create SessionLocationManager Utility Class

**What was done:**
- Created `SessionLocationManager.java` utility class in `util` package
- Added methods to save, retrieve, check, and clear session location
- Uses SharedPreferences to store location (latitude/longitude as doubles)
- Provides clean abstraction for session location management

**Why this is important:**
- **Problem solved:** Centralized location for session location management. Without this utility, every fragment would need to know SharedPreferences keys and handle location storage/retrieval manually, leading to code duplication and bugs.
- **Code reusability:** One place to handle all session location operations - any fragment can use these simple methods
- **Consistency:** All fragments use the same methods, ensuring consistent behavior

**How it solves our overall problem:**
- **Foundation for Commit 7:** `LoginFragment` will use `saveSessionLocation()` to store location after capture
- **Foundation for Commit 11:** `EventDetailFragment` will use `getSessionLocation()` to retrieve location when user joins event
- **Clean abstraction:** Fragments don't need to know about SharedPreferences keys or storage details
- **Maintainable:** If we change storage mechanism (e.g., switch to database), only one file needs updating

**Key methods:**
- `saveSessionLocation(Context, GeoPoint)` - Store location in SharedPreferences
- `getSessionLocation(Context)` - Retrieve location from SharedPreferences (returns GeoPoint or null)
- `hasSessionLocation(Context)` - Check if location exists
- `clearSessionLocation(Context)` - Clear location (called on logout)

**Files created:**
- `app/src/main/java/com/example/arcane/util/SessionLocationManager.java`

**Status:** ✅ COMPLETED

---

## ✅ Commit 9: Update EventService to Use Session Location on Join

**What was done:**
- Updated `EventService.joinWaitingList()` to accept `GeoPoint sessionLocation` parameter
- Added logic to check `event.geolocationRequired` before storing location:
  - If `true` and `sessionLocation != null`: Store session location in `WaitingListEntry.joinLocation`
  - If `false` or `sessionLocation == null`: Set `joinLocation = null` (don't store)
- Updated `addUserToWaitingList()` to accept `GeoPoint joinLocation` parameter
- Set `entry.setJoinLocation(joinLocation)` when creating waiting list entry

**Why this is important:**
- **Problem solved:** Connects session location (captured at login) to event join flow. Without this, the location captured at login would never be used when users join events.
- **Respects organizer preference:** Only stores location if event requires it (`geolocationRequired = true`)
- **US 02.02.03 implementation:** Implements organizer's ability to enable/disable geolocation requirement per event

**How it solves our overall problem:**
- **Data storage:** Entrant join locations are stored in database (only for events that require it)
- **Enables map feature:** Organizers can see where entrants joined from (for events with location enabled)
- **Session-based:** Location represents where user was when they logged in
- **Privacy-aware:** Organizers can disable location tracking for specific events

**Key logic:**
```java
// Check if event requires geolocation
if (event.geolocationRequired == true && sessionLocation != null) {
    joinLocation = sessionLocation; // Store location
} else {
    joinLocation = null; // Don't store location
}
```

**Files modified:**
- `app/src/main/java/com/example/arcane/service/EventService.java`
  - Updated `joinWaitingList()` signature to accept `GeoPoint sessionLocation`
  - Added logic to check `event.geolocationRequired` and conditionally set location
  - Updated `addUserToWaitingList()` signature to accept `GeoPoint joinLocation`
  - Set `entry.setJoinLocation(joinLocation)` when creating entry

**Status:** ✅ COMPLETED

---

## ✅ Commit 11: Update EventDetailFragment to Pass Session Location on Join

**What was done:**
- Added `SessionLocationManager` import to `EventDetailFragment`
- Updated `handleJoinWaitlist()` to get session location using `SessionLocationManager.getSessionLocation()`
- Passed session location to `EventService.joinWaitingList()` as the third parameter

**Why this is important:**
- **Problem solved:** Connects session location (captured at login) to event join flow. Without this, the location captured at login would never be passed to EventService when users join events.
- **Completes the flow:** Login → Location captured → Stored in session → Retrieved when joining event → Passed to EventService → Stored in database (if event requires it)

**How it solves our overall problem:**
- **Complete location flow:** User logs in → Location captured → User joins event → Location stored (if event requires it)
- **Enables map feature:** Organizers can see where entrants joined from (for events with location enabled)
- **US 02.02.02 foundation:** Location data is now stored in database, ready for map visualization

**Key logic:**
```java
// Get session location (captured at login)
GeoPoint sessionLocation = SessionLocationManager.getSessionLocation(requireContext());

// Pass to EventService (will check geolocationRequired and store if needed)
eventService.joinWaitingList(eventId, userId, sessionLocation)
```

**Files modified:**
- `app/src/main/java/com/example/arcane/ui/events/EventDetailFragment.java`
  - Added `SessionLocationManager` import
  - Updated `handleJoinWaitlist()` to get and pass session location

**Feature:** Session-based location capture (foundation for US 02.02.02 - Organizer sees entrants on a map)

**Status:** ✅ COMPLETED

---

## ✅ Commit 12: Add Google Places Autocomplete to CreateEventFragment

**What was done:**
- Added Google Places SDK imports to `CreateEventFragment`
- Initialized Places SDK in `onViewCreated()` (reads API key from AndroidManifest)
- Created `ActivityResultLauncher` for Places Autocomplete intent
- Added `setupLocationAutocomplete()` method to handle location input clicks
- Added `openPlacesAutocomplete()` method to launch Places Autocomplete activity
- Added `handlePlaceSelection()` method to process selected place:
  - Sets address in location input field
  - Stores coordinates in `selectedLocationGeoPoint` (for Commit 13)
- Made location input field clickable to launch autocomplete

**Why this is important:**
- **Problem solved:** Organizers can now get address suggestions as they type, making event creation faster and more accurate. Without this, organizers have to manually type full addresses, leading to typos and inconsistent formats.
- **Better UX:** Real-time address suggestions from Google Places database
- **Accurate addresses:** No typos, consistent format, validated addresses

**How it solves our overall problem:**
- **Foundation for Commit 13:** Address is selected and coordinates are stored in `selectedLocationGeoPoint`
- **Enables geocoding:** Selected place includes coordinates that will be saved to `event.geolocation` in Commit 13
- **Better data quality:** All event addresses are validated and properly formatted

**Key features:**
- Full-screen Places Autocomplete activity (simple, clean UI)
- Extracts address and coordinates from selected place
- Stores coordinates for later use (Commit 13 will save to event)
- Handles errors gracefully (shows toast if autocomplete fails)

**Files modified:**
- `app/src/main/java/com/example/arcane/ui/createevent/CreateEventFragment.java`
  - Added Places SDK imports
  - Added Places initialization
  - Added autocomplete launcher and handlers
  - Added `selectedLocationGeoPoint` field to store coordinates

**Feature:** Address autocomplete suggestions (foundation for US 02.02.03 - enable/disable geolocation requirement)

**Note:** Requires Google Places API key in AndroidManifest. Will work once API key is configured.

**Status:** ✅ COMPLETED

---

## ✅ Commit 13: Update CreateEventFragment to Save Event Geolocation

**What was done:**
- Updated `createEvent()` method to save `selectedLocationGeoPoint` to `event.geolocation`
- Changed from `event.setGeolocation(null)` to `event.setGeolocation(selectedLocationGeoPoint)`
- Coordinates from Places Autocomplete (Commit 12) are now saved to the event
- The existing `enableGeolocationCheckbox` already sets `event.geolocationRequired` (US 02.02.03)

**Why this is important:**
- **Problem solved:** Event geolocation coordinates are now saved to the database. Without this, the coordinates selected via Places Autocomplete would be lost and not stored with the event.
- **Enables map features:** Event location can now be displayed on maps (Commit 18)
- **US 02.02.03 complete:** Organizer can enable/disable geolocation requirement via checkbox, and coordinates are saved when address is selected

**How it solves our overall problem:**
- **Data storage:** Event geolocation coordinates are stored in Firestore
- **Enables map display:** Event location can be shown on maps (for organizers and users)
- **Complete US 02.02.03:** Organizer can toggle geolocation requirement, and coordinates are saved
- **Foundation for Commit 18:** Event location will be displayed on event detail page map

**Key logic:**
```java
// If organizer selected address via Places Autocomplete:
event.setGeolocation(selectedLocationGeoPoint); // Coordinates saved

// If organizer didn't select address (typed manually):
event.setGeolocation(null); // No coordinates, will show "Unknown" (Commit 14)
```

**Files modified:**
- `app/src/main/java/com/example/arcane/ui/createevent/CreateEventFragment.java`
  - Updated `createEvent()` to save `selectedLocationGeoPoint` to `event.geolocation`

**Feature:** US 02.02.03 - Organizer enables/disables geolocation requirement (complete with coordinate saving)

**Status:** ✅ COMPLETED

---

## ✅ Commit 14: Show "Unknown" Location for Legacy Events

**What was done:**
- Updated `EventCardAdapter.onBindViewHolder()` to show "Unknown" when `event.getLocation()` is null or empty
- Updated `EventDetailFragment.populateEventDetails()` to show "Unknown" when `event.getLocation()` is null or empty
- Added null-safety checks for location display in both event cards and event detail page

**Why this is important:**
- **Problem solved:** Legacy events (created before geolocation feature) don't have location data. Without this fix, these events would show blank location fields, which looks broken and confusing to users.
- **User experience:** Users can now clearly see when an event doesn't have location information, rather than seeing an empty field
- **Backward compatibility:** Ensures the app gracefully handles events created before the geolocation feature was added

**How it solves our overall problem:**
- **Graceful degradation:** Legacy events display "Unknown" instead of blank fields
- **Consistent UI:** All events show location information (either the actual location or "Unknown")
- **Foundation for map features:** When we add map views (Commit 18), we can check for null geolocation and show "Location: Unknown" instead of trying to display a map
- **Data integrity:** Makes it clear which events have location data and which don't

**Key logic:**
```java
// In EventCardAdapter and EventDetailFragment:
String location = event.getLocation();
if (location != null && !location.isEmpty()) {
    // Show actual location
    locationView.setText(location);
} else {
    // Show "Unknown" for legacy events
    locationView.setText("Unknown");
}
```

**Files modified:**
- `app/src/main/java/com/example/arcane/ui/events/EventCardAdapter.java`
  - Updated `onBindViewHolder()` to handle null/empty location
- `app/src/main/java/com/example/arcane/ui/events/EventDetailFragment.java`
  - Updated `populateEventDetails()` to show "Unknown" for null/empty location

**Commit message:**
```
feat: Show "Unknown" location for legacy events without location data

- Add null-safety checks in EventCardAdapter for location display
- Add null-safety checks in EventDetailFragment for location display
- Show "Unknown" instead of blank fields for events without location
- Ensures backward compatibility with events created before geolocation feature
```

**Status:** ✅ COMPLETED

---

## 📋 Remaining Commits

### Phase 1: Foundation and Data Model
- [x] Commit 2: Add geolocation field to WaitingListEntry model ✅
- [x] Commit 3: Add location tracking preference to Users model ✅
- [x] Commit 4: Add Google Maps SDK dependency ✅

### Phase 2: Session Location Management
- [x] Commit 5: Create LocationPermissionHelper utility class ✅
- [x] Commit 6: Create LocationService utility class ✅
- [x] Commit 7: Auto-capture location on login ✅
- [x] Commit 8: Create SessionLocationManager utility class ✅

### Phase 3: Use Session Location on Join Waitlist
- [x] Commit 9: Update EventService to use session location on join ✅
- [ ] Commit 10: ~~Add location validation in EventService~~ (CANCELLED) ❌
- [x] Commit 11: Update EventDetailFragment to pass session location on join ✅

### Phase 4: Event Creation with Location
- [x] Commit 12: Add Google Places Autocomplete to CreateEventFragment ✅
- [x] Commit 13: Update CreateEventFragment to save event geolocation ✅
- [x] Commit 14: Show "Unknown" location for legacy events ✅

### Phase 5: Map Display Functionality
- [ ] Commit 15: Create EntrantsMapFragment
- [ ] Commit 16: Add map navigation to EntrantsFragment
- [ ] Commit 17: Implement map marker display logic
- [ ] Commit 18: Add map view to event detail page

### Phase 6: UI Polish and Event Cards
- [ ] Commit 19: Add location chip/tag to event cards
- [ ] Commit 20: Update navigation graph for map fragments

---

## Key Features Being Implemented

1. **Address Autocomplete for Organizers** (Commit 12)
   - When organizer types event address, Google Places API provides real-time suggestions
   - Automatically converts selected address to GPS coordinates (GeoPoint)
   - Makes event creation faster and more accurate

2. **Location Tracking for Users**
   - Location is automatically captured on login (no user choice)
   - Location stored in session (SharedPreferences)
   - Session location used when user joins event waitlists
   - Same location used for all events joined during that session

3. **Map Visualization for Organizers**
   - Organizers can see all entrant join locations on an interactive map
   - Helps understand geographic distribution of participants
   - Event location is also displayed on the map

4. **Location Enforcement**
   - If event requires geolocation, users must enable location tracking to join
   - Ensures data quality for events that need location verification

---

## Technical Notes

- **Permissions:** Android requires both FINE and COARSE location permissions for compatibility across Android versions
- **Session-based:** Location captured once per login session, stored in SharedPreferences
- **Data Storage:** Locations stored as Firestore GeoPoint objects for efficient querying
- **Backward Compatibility:** Events created before this feature show "Unknown" location
- **⚠️ Requirements Changed:** See [GEOLOCATION_REQUIREMENTS_CHANGE.md](./GEOLOCATION_REQUIREMENTS_CHANGE.md) for details

---

## 🎯 Testable Features Timeline

**See [TESTABLE_FEATURES_ROADMAP.md](./TESTABLE_FEATURES_ROADMAP.md) for detailed testing guide.**

### Quick Summary:
- **Commit 7:** First testable feature - Auto-capture location on login (NEEDS RESTRUCTURING) 🔄
- **Commit 12:** Second major feature - Address autocomplete for event creation ✅
- **Commits 15-17:** Third major feature - Map view of entrant locations ✅

**⚠️ Note:** Commit 7 needs restructuring due to requirements change. See [GEOLOCATION_REQUIREMENTS_CHANGE.md](./GEOLOCATION_REQUIREMENTS_CHANGE.md).

Each feature is independently testable once its dependencies are complete.

