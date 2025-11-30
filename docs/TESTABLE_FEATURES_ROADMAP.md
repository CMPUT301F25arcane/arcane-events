# Testable Features Roadmap - Geolocation Implementation

## 🎯 Quick Answer: When Can We Test?

### **First Testable Feature: Commit 7** ✅
**Feature:** Location Permission Dialog on Login  
**What you'll see:** When a user logs in, a popup appears asking if they want to enable location tracking  
**How to test:** Log in → See dialog → Accept/Decline → Preference saved

### **Second Major Testable Feature: Commit 12** ✅
**Feature:** Address Autocomplete for Event Creation  
**What you'll see:** When organizer types an event address, real-time suggestions appear  
**How to test:** Login as organizer → Create Event → Type address → See suggestions

---

## 📊 Feature Dependency Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    FOUNDATION (Commits 1-4)                     │
│  ✅ Permissions  ✅ Data Models  ✅ SDK Dependencies            │
│  (Not directly testable, but required for everything)           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              UTILITY CLASSES (Commits 5-6)                      │
│  ⚙️ LocationPermissionHelper  ⚙️ LocationService               │
│  (Backend tools, not visible to user)                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│         🎉 FIRST TESTABLE FEATURE: Commit 7                     │
│  ✅ Location Permission Dialog on Login                         │
│  User sees: Popup asking for location tracking consent          │
│  Test: Login → Dialog appears → Accept/Decline → Saved         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│              PROFILE CLEANUP (Commit 8)                        │
│  ✅ Remove geolocation toggle from organizer/admin profile      │
│  Test: Check profile page - toggle should be gone               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│      LOCATION CAPTURE ON JOIN (Commits 9-11)                    │
│  ✅ Capture location when user joins event waitlist             │
│  Test: Join event → Location captured → Stored in database      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│    🎉 SECOND MAJOR TESTABLE FEATURE: Commit 12                  │
│  ✅ Address Autocomplete for Event Creation                     │
│  User sees: Real-time address suggestions as they type          │
│  Test: Create event → Type address → See suggestions → Select   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│         EVENT LOCATION SAVING (Commits 13-14)                   │
│  ✅ Save event geolocation  ✅ Show "Unknown" for legacy events │
│  Test: Create event with address → Check event has location    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│         🎉 THIRD MAJOR TESTABLE FEATURE: Commits 15-17         │
│  ✅ Map View of Entrant Locations                              │
│  User sees: Interactive map showing where entrants joined from  │
│  Test: View entrants → Click "View Map" → See map with markers │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📋 Detailed Testable Features Timeline

### **Commit 7: Location Permission Dialog on Login** 🎯 FIRST TESTABLE

**What Gets Created:**
- Dialog UI that appears after successful login
- Logic to ask user: "Allow location tracking?"
- Saves user's choice to `Users.locationTrackingEnabled` field
- Updates Firestore with user preference

**What You Can Test:**
1. **Login Flow:**
   - Log in with any user account
   - After successful login, a dialog appears
   - Dialog asks: "Would you like to enable location tracking?"
   - Options: "Allow" or "Don't Allow"

2. **Accept Flow:**
   - Click "Allow"
   - Dialog closes
   - User preference saved to database
   - User proceeds to their events page

3. **Decline Flow:**
   - Click "Don't Allow"
   - Dialog closes
   - Preference saved as `false`
   - User proceeds to their events page

4. **Verify in Database:**
   - Check Firestore `Users` collection
   - Find your user document
   - Verify `locationTrackingEnabled` field is `true` or `false`

**What Makes This Work:**
- ✅ Commit 1: Permissions declared (required)
- ✅ Commit 3: `locationTrackingEnabled` field exists (required)
- ✅ Commit 5: `LocationPermissionHelper` utility (required)
- ✅ Commit 6: `LocationService` utility (required)
- ✅ Commit 7: Dialog UI and logic (this commit)

**Files Created/Modified:**
- `LoginFragment.java` - Adds dialog after login
- Uses `LocationPermissionHelper` (Commit 5)
- Uses `LocationService` (Commit 6)
- Updates `Users` model via `UserService`

---

### **Commit 12: Address Autocomplete for Event Creation** 🎯 SECOND MAJOR TESTABLE

**What Gets Created:**
- Google Places Autocomplete integrated into event creation form
- Real-time address suggestions as organizer types
- Automatic conversion of selected address to GPS coordinates
- Address field in `CreateEventFragment` now has autocomplete

**What You Can Test:**
1. **Autocomplete Flow:**
   - Login as organizer
   - Click "+" button to create event
   - Scroll to "Event Location" field
   - Start typing an address (e.g., "123 Main St")
   - See dropdown with suggestions appear
   - Select a suggestion
   - Address field auto-fills with full address

2. **Address Conversion:**
   - After selecting address, coordinates are automatically generated
   - Event location is saved as GeoPoint in Firestore
   - Verify in database that event has `geolocation` field

3. **User Experience:**
   - Faster event creation (no need to type full address)
   - No typos in addresses
   - Consistent address format

**What Makes This Work:**
- ✅ Commit 4: Google Places SDK dependency (required)
- ✅ Commit 12: Autocomplete integration (this commit)
- ✅ Commit 13: Save geolocation (next commit, but can test UI now)

**Files Created/Modified:**
- `CreateEventFragment.java` - Adds Places Autocomplete
- `fragment_create_event.xml` - Address input field enhanced
- Uses Google Places SDK (from Commit 4)

---

### **Commits 15-17: Map View of Entrant Locations** 🎯 THIRD MAJOR TESTABLE

**What Gets Created:**
- `EntrantsMapFragment` - New fragment showing map
- Interactive Google Map with zoom/pan
- Markers showing where each entrant joined from
- Event location marker
- "View Map" button in `EntrantsFragment` (currently hidden)

**What You Can Test:**
1. **Map Display:**
   - Login as organizer
   - Go to "My Events"
   - Click on an event
   - Click "Show Entrants"
   - Click "View Map" button
   - See interactive map with markers

2. **Marker Interaction:**
   - Each marker represents an entrant's join location
   - Click marker to see entrant name
   - Event location shown with different marker
   - Zoom in/out, pan around map

3. **Data Visualization:**
   - See geographic distribution of participants
   - Understand where your event audience is located
   - Identify popular join locations

**What Makes This Work:**
- ✅ Commit 2: `joinLocation` field in `WaitingListEntry` (required)
- ✅ Commit 4: Google Maps SDK (required)
- ✅ Commit 9: Location capture on join (required - need data)
- ✅ Commits 15-17: Map UI and logic (these commits)

**Files Created/Modified:**
- `EntrantsMapFragment.java` - New fragment (Commit 15)
- `fragment_entrants_map.xml` - Map layout (Commit 15)
- `EntrantsFragment.java` - Add "View Map" button (Commit 16)
- Map marker logic (Commit 17)

---

## 🔗 Dependency Chain Summary

### For Commit 7 (Location Dialog) to Work:
```
Commit 1 (Permissions) 
  → Commit 3 (Users.locationTrackingEnabled field)
    → Commit 5 (LocationPermissionHelper)
      → Commit 6 (LocationService)
        → Commit 7 (Dialog UI) ✅ TESTABLE
```

### For Commit 12 (Address Autocomplete) to Work:
```
Commit 4 (Places SDK)
  → Commit 12 (Autocomplete UI) ✅ TESTABLE
    → Commit 13 (Save geolocation) ✅ FULLY FUNCTIONAL
```

### For Commits 15-17 (Map View) to Work:
```
Commit 2 (joinLocation field)
  → Commit 4 (Maps SDK)
    → Commit 9 (Capture location on join)
      → Commits 15-17 (Map UI) ✅ TESTABLE
```

---

## 📝 Testing Checklist by Commit

### ✅ After Commit 7:
- [ ] Login as user → See location permission dialog
- [ ] Click "Allow" → Check Firestore: `locationTrackingEnabled = true`
- [ ] Click "Don't Allow" → Check Firestore: `locationTrackingEnabled = false`
- [ ] Login again → Dialog should NOT appear (already asked)
- [ ] Test with new user → Dialog appears

### ✅ After Commit 12:
- [ ] Login as organizer
- [ ] Create new event
- [ ] Type in address field → See suggestions dropdown
- [ ] Select suggestion → Address auto-fills
- [ ] Create event → Check Firestore: Event has `geolocation` field

### ✅ After Commits 15-17:
- [ ] Login as organizer
- [ ] Create event with location
- [ ] Have users join event (with location tracking enabled)
- [ ] View entrants → Click "View Map"
- [ ] See map with markers for each entrant
- [ ] See event location marker
- [ ] Test zoom/pan functionality

---

## 🎓 What Each Commit Enables

| Commit | What It Enables | Testable? | Depends On |
|--------|----------------|-----------|------------|
| 1-4 | Foundation (permissions, models, SDKs) | ❌ No | None |
| 5-6 | Utility classes (permission helper, location service) | ❌ No | 1, 3 |
| **7** | **Location permission dialog** | **✅ YES** | 1, 3, 5, 6 |
| 8 | Remove toggle from profile | ✅ Yes (minor) | None |
| 9-11 | Capture location on join | ✅ Yes (needs event) | 2, 7 |
| **12** | **Address autocomplete** | **✅ YES** | 4 |
| 13 | Save event geolocation | ✅ Yes | 12 |
| 14 | Show "Unknown" for legacy events | ✅ Yes | 13 |
| **15-17** | **Map view of entrants** | **✅ YES** | 2, 4, 9 |
| 18 | Map view on event detail | ✅ Yes | 15-17 |
| 19-20 | UI polish and navigation | ✅ Yes | 15-17 |

---

## 🚀 Quick Start Testing Guide

### Test Feature 1: Location Permission Dialog (After Commit 7)

1. **Build and run the app**
2. **Login with any account**
3. **Expected:** After login, you see a dialog asking about location tracking
4. **Test Accept:**
   - Click "Allow"
   - Check Firestore: User document should have `locationTrackingEnabled: true`
5. **Test Decline:**
   - Logout and login again (or use different account)
   - Click "Don't Allow"
   - Check Firestore: User document should have `locationTrackingEnabled: false`

### Test Feature 2: Address Autocomplete (After Commit 12)

1. **Build and run the app**
2. **Login as organizer**
3. **Create new event:**
   - Click "+" button
   - Fill in event details
   - Scroll to "Event Location" field
4. **Test autocomplete:**
   - Type "123 Main" in address field
   - See dropdown with suggestions
   - Select a suggestion
   - Address field fills automatically
5. **Verify:**
   - Create the event
   - Check Firestore: Event document should have `geolocation` field with coordinates

### Test Feature 3: Map View (After Commits 15-17)

1. **Prerequisites:**
   - Have at least one event created (with location)
   - Have at least one user join the event (with location tracking enabled)
2. **View map:**
   - Login as organizer
   - Go to "My Events"
   - Click on event
   - Click "Show Entrants"
   - Click "View Map" button
3. **Expected:**
   - See Google Map
   - See markers for each entrant's join location
   - See marker for event location
   - Can zoom and pan

---

## 📚 Files Created/Modified Reference

### Commit 7 (Location Dialog):
- `LoginFragment.java` - Adds dialog logic
- Uses: `LocationPermissionHelper.java` (Commit 5)
- Uses: `LocationService.java` (Commit 6)
- Updates: `Users` model via `UserService`

### Commit 12 (Address Autocomplete):
- `CreateEventFragment.java` - Adds Places Autocomplete
- `fragment_create_event.xml` - Address input field
- Uses: Google Places SDK (Commit 4)

### Commits 15-17 (Map View):
- `EntrantsMapFragment.java` - New fragment (Commit 15)
- `fragment_entrants_map.xml` - Map layout (Commit 15)
- `EntrantsFragment.java` - Shows "View Map" button (Commit 16)
- Map marker display logic (Commit 17)
- Uses: Google Maps SDK (Commit 4)
- Uses: `WaitingListEntry.joinLocation` (Commit 2)

---

## 💡 Key Insights

1. **Commit 7 is the first visible, testable feature** - Users will see and interact with it immediately
2. **Commit 12 is the second major testable feature** - Organizers will use it every time they create an event
3. **Commits 15-17 provide the full map visualization** - The complete feature organizers requested
4. **Each feature builds on previous commits** - The dependency chain ensures everything works together
5. **Testing can happen incrementally** - You don't need to wait for all 20 commits to test features

---

## 🎯 Summary

- **First Testable Feature:** Commit 7 (Location Permission Dialog)
- **Second Major Feature:** Commit 12 (Address Autocomplete)
- **Third Major Feature:** Commits 15-17 (Map View)

Each feature is independently testable once its dependencies are complete. The roadmap above shows exactly what needs to be done before each feature can be tested.

