# Organizer Create Event Button - Implementation Logic

## Overview
This document explains how the Floating Action Button (FAB) for organizers works to create events.

## Flow

### 1. **FAB Visibility**
- **Location**: `fragment_events.xml` layout file
- **Default State**: Hidden (`android:visibility="gone"`)
- **Organizer View**: `OrganizerEventsFragment` sets FAB to `VISIBLE` in `onViewCreated()`
- **User Views**: `UserEventsFragment` and `GlobalEventsFragment` keep FAB hidden (`GONE`)

### 2. **FAB Positioning**
- **Layout**: Floating Action Button positioned at bottom-right
- **Constraints**: 
  - Bottom: Constrained to parent bottom
  - End: Constrained to parent end
  - Margin: 16dp from end, 80dp from bottom (to clear bottom navigation bar)

### 3. **Navigation Flow**
```
OrganizerEventsFragment (My Events Organizer)
    ↓
User clicks FAB (+ button)
    ↓
NavController.navigate(R.id.navigation_create_event)
    ↓
CreateEventFragment opens
    ↓
User fills form and clicks "Create Event"
    ↓
EventService.createEvent(event) saves to Firebase
    ↓
On success: Navigate back (navigateUp)
    ↓
OrganizerEventsFragment.onResume() triggers
    ↓
loadOrganizerEvents() refreshes the list
    ↓
New event appears in organizer's event list
```

### 4. **Database Save Logic**
- **Service**: `EventService.createEvent(Event event)`
- **Repository**: `EventRepository.createEvent(Event event)`
- **Firebase Collection**: `events/{eventId}`
- **Key Fields Saved**:
  - `organizerId`: Current user's UID (from FirebaseAuth)
  - `eventName`: From form input
  - `location`: From form input
  - `eventDate`: Start date from date picker
  - `registrationStartDate`: Current timestamp
  - `registrationEndDate`: Registration deadline from date picker
  - `status`: Set to "OPEN"
  - `cost`, `maxEntrants`, `numberOfWinners`: From form inputs
  - `geolocationRequired`: From checkbox

### 5. **List Refresh**
- When organizer returns to `OrganizerEventsFragment` after creating an event:
  - `onResume()` lifecycle method triggers
  - Calls `loadOrganizerEvents()` which queries Firebase
  - Query: `events` collection where `organizerId == currentUser.uid`
  - Adapter updates with new list including the created event

## Key Files

1. **Layout**: `app/src/main/res/layout/fragment_events.xml`
   - Contains FAB definition

2. **Fragment**: `app/src/main/java/com/example/arcane/ui/events/OrganizerEventsFragment.java`
   - Shows/hides FAB based on role
   - Handles FAB click navigation
   - Loads organizer's events from database

3. **Create Form**: `app/src/main/java/com/example/arcane/ui/createevent/CreateEventFragment.java`
   - Form validation
   - Event object creation
   - Database save via EventService

4. **Navigation**: `app/src/main/res/navigation/mobile_navigation.xml`
   - Defines route: `navigation_create_event` → `CreateEventFragment`

## Testing Checklist

- [ ] FAB visible on organizer login
- [ ] FAB hidden on user login
- [ ] FAB click opens create event form
- [ ] Form validation works
- [ ] Event saves to Firebase with correct organizerId
- [ ] After save, navigates back to organizer events
- [ ] New event appears in organizer's event list
- [ ] FAB positioned correctly above bottom navigation bar

