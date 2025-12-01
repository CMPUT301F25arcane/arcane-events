# Debugging and Setup Guide - Geolocation Features

## 🔍 Issue Summary

This document addresses multiple issues with geolocation features:
1. Location not being stored at login
2. Places Autocomplete crashing
3. Google API setup requirements
4. SharedPreferences storage location

---

## 1. 📝 Added Debug Logging

### Login Location Capture Logging

**Location:** `LoginFragment.java`

**Added Logs:**
- `"DEBUG: Attempting to capture location..."` - When location capture starts
- `"DEBUG: Location permission status: GRANTED/DENIED"` - Permission check result
- `"DEBUG: SUCCESS: Location = lat, lng"` - When location is successfully captured
- `"DEBUG: FAILURE: [error message]"` - When location capture fails
- `"DEBUG: Session location saved: lat, lng"` - When location is saved to SharedPreferences

### Event Join Location Logging

**Location:** `EventDetailFragment.java`

**Added Logs:**
- `"DEBUG: Session location: EXISTS/NULL"` - When checking session location before joining
- `"DEBUG: Event geolocationRequired: true/false/null"` - Event's geolocation requirement
- `"DEBUG: Join location will be: SET/NULL"` - Final join location decision

### How to View Logs

1. Open Android Studio
2. Go to **View → Tool Windows → Logcat**
3. Filter by tag: `LoginFragment` or `EventDetailFragment`
4. Or filter by text: `DEBUG`

---

## 2. 🗺️ Places Autocomplete Crash Fix

### Problem
App crashes when clicking on location input field.

### Root Causes
1. **Google Places API key not configured** - API key is still `YOUR_API_KEY`
2. **Places SDK not initialized** - Initialization fails silently
3. **Missing error handling** - Crash when trying to open autocomplete without proper setup

### Fix Applied
- Added try-catch around Places initialization
- Added null checks before opening autocomplete
- Added error toast messages
- Made location input optional (can type manually)

### Testing
1. If API key is not set: App shows toast "Places API not configured" and allows manual typing
2. If API key is set but invalid: App shows error toast and allows manual typing
3. If API key is valid: Autocomplete opens normally

---

## 3. 🔑 Google API Setup - Step by Step

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click **"Select a project"** → **"New Project"**
3. Enter project name: `Arcane Events` (or your choice)
4. Click **"Create"**
5. Wait for project creation (30 seconds)

### Step 2: Enable Required APIs

1. In Google Cloud Console, go to **"APIs & Services" → "Library"**
2. Search for and enable these APIs:
   - **"Maps SDK for Android"** - Click → Enable
   - **"Places API"** - Click → Enable
   - **"Geocoding API"** (optional, for reverse geocoding)

### Step 3: Create API Key

1. Go to **"APIs & Services" → "Credentials"**
2. Click **"+ CREATE CREDENTIALS"** → **"API Key"**
3. Copy the API key (starts with `AIza...`)
4. **IMPORTANT:** Click **"Restrict Key"** to secure it:
   - **Application restrictions:** Select **"Android apps"**
   - Click **"Add an item"**
   - **Package name:** `com.example.arcane` (check your `build.gradle.kts` for actual package)
   - **SHA-1 certificate fingerprint:** Get from Android Studio:
     - Open Terminal in Android Studio
     - Run: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
     - Copy the SHA-1 value (looks like: `AA:BB:CC:DD:...`)
   - **API restrictions:** Select **"Restrict key"**
   - Check: **"Maps SDK for Android"** and **"Places API"**
   - Click **"Save"**

### Step 4: Add API Key to Android App

1. Open `app/src/main/AndroidManifest.xml`
2. Find this line:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_API_KEY" />
   ```
3. Replace `YOUR_API_KEY` with your actual API key:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="AIzaSyC_your_actual_key_here" />
   ```
4. Save the file
5. **Rebuild the app** (Build → Rebuild Project)

### Step 5: Test

1. Run the app
2. Try to create an event
3. Click on location input field
4. Places Autocomplete should open (not crash)

---

## 4. 🔥 Firebase Geolocation - What is it?

### What is "Geolocation in Firebase"?

**Answer:** There is NO separate "geolocation" setting in Firebase. What we're using is:

1. **Firestore GeoPoint** - A data type in Firestore that stores latitude/longitude
   - Used in: `Event.geolocation`, `WaitingListEntry.joinLocation`
   - Not a Firebase setting, just a data type

2. **Google Maps SDK** - For displaying maps (requires API key)
   - Not a Firebase feature, it's Google Maps

3. **Google Places API** - For address autocomplete (requires API key)
   - Not a Firebase feature, it's Google Places

### What You Need to Enable

✅ **In Firebase Console:** Nothing! Firestore already supports GeoPoint natively.

✅ **In Google Cloud Console:** 
- Maps SDK for Android
- Places API

### Summary
- **Firebase/Firestore:** Already supports GeoPoint (no setup needed)
- **Google Cloud:** Need to enable Maps SDK and Places API (see Step 3 above)

---

## 5. 📱 Running on Android Studio - Location Storage

### Will Location Be Stored When Running on Android Studio?

**Answer:** **YES**, if:
1. ✅ Location permission is granted
2. ✅ Location services are enabled on device/emulator
3. ✅ Device/emulator has GPS signal (or mock location set)

### Testing Location on Emulator

1. **Set Mock Location:**
   - Open Android Studio
   - Go to **Tools → Device Manager**
   - Click **"..."** next to your emulator → **"Extended Controls"**
   - Go to **"Location"** tab
   - Enter coordinates (e.g., `37.7749, -122.4194` for San Francisco)
   - Click **"Set Location"**

2. **Grant Location Permission:**
   - Run the app
   - When permission dialog appears, click **"Allow"**
   - Or manually: Settings → Apps → Arcane Events → Permissions → Location → Allow

3. **Check Logs:**
   - Open Logcat
   - Filter by `LoginFragment`
   - Look for: `"DEBUG: SUCCESS: Location = ..."`

### Testing Location on Physical Device

1. **Enable Location Services:**
   - Settings → Location → ON

2. **Grant Permission:**
   - When app requests permission, click **"Allow"**

3. **Check Logs:**
   - Connect device via USB
   - Enable USB debugging
   - View logs in Android Studio Logcat

---

## 6. 💾 SharedPreferences Storage - Location and Significance

### Where is SharedPreferences Stored?

**Physical Location:**
```
/data/data/com.example.arcane/shared_prefs/
```

**Files:**
- `user_prefs.xml` - Stores user role
- `session_prefs.xml` - Stores session location (lat/lng)

### How to View SharedPreferences

**Method 1: Android Studio Device File Explorer**
1. Run app on device/emulator
2. Go to **View → Tool Windows → Device File Explorer**
3. Navigate to: `/data/data/com.example.arcane/shared_prefs/`
4. Download `session_prefs.xml` to view contents

**Method 2: ADB Command**
```bash
adb shell run-as com.example.arcane cat shared_prefs/session_prefs.xml
```

**Method 3: Add Debug Code (Temporary)**
```java
SharedPreferences prefs = getSharedPreferences("session_prefs", Context.MODE_PRIVATE);
Map<String, ?> all = prefs.getAll();
Log.d("DEBUG", "Session prefs: " + all.toString());
```

### What is Stored in Each File?

#### `user_prefs.xml`
```xml
<map>
    <string name="user_role">ORGANIZER</string>
</map>
```
**Purpose:** Caches user role for bottom navigation routing
**Cleared:** On logout

#### `session_prefs.xml`
```xml
<map>
    <long name="session_location_lat">4607182418800017408</long>
    <long name="session_location_lng">-4617315517961601024</long>
</map>
```
**Purpose:** Stores user's location captured at login (for this session only)
**Cleared:** On logout
**Note:** Values are stored as `long` (bits of double) for precision

### Significance of SharedPreferences

1. **Session-Based Storage:**
   - Location is stored per login session
   - Cleared on logout (privacy)
   - Not persisted across app restarts if user logs out

2. **Performance:**
   - Fast access (no network call)
   - Available immediately when user joins event

3. **Privacy:**
   - Location not stored in Firestore Users document
   - Only stored in session (temporary)
   - Only stored in WaitingListEntry when user joins event (if event requires it)

4. **Why Not Firestore Users Document?**
   - Location changes frequently (user moves)
   - Would require constant updates
   - Privacy concern (permanent storage)
   - Session-based is sufficient for our use case

---

## 7. 🐛 Troubleshooting Checklist

### Location Not Stored at Login

- [ ] Check Logcat for `"DEBUG: SUCCESS: Location = ..."` (should see this)
- [ ] Check if permission is granted: Settings → Apps → Permissions → Location
- [ ] Check if location services are enabled: Settings → Location → ON
- [ ] Check if device has GPS signal (or mock location set on emulator)
- [ ] Check `session_prefs.xml` for `session_location_lat` and `session_location_lng`

### Places Autocomplete Crashes

- [ ] Check if API key is set in `AndroidManifest.xml` (not `YOUR_API_KEY`)
- [ ] Check if Maps SDK and Places API are enabled in Google Cloud Console
- [ ] Check if API key restrictions allow your app (package name + SHA-1)
- [ ] Check Logcat for error messages
- [ ] Try typing location manually (should work even if autocomplete fails)

### Join Location is NULL

- [ ] Check Logcat: `"DEBUG: Session location: EXISTS/NULL"`
- [ ] Check Logcat: `"DEBUG: Event geolocationRequired: true/false/null"`
- [ ] Verify event was created with "Enable Geolocation" checkbox checked
- [ ] Check Firestore event document: `geolocationRequired` field should be `true`
- [ ] If session location is NULL: Location wasn't captured at login (see above)
- [ ] If geolocationRequired is false: This is expected - location only stored if event requires it

---

## 8. 📋 Quick Reference

### Log Tags to Filter
- `LoginFragment` - Login and location capture
- `EventDetailFragment` - Event join and location usage
- `CreateEventFragment` - Event creation and Places Autocomplete
- `DEBUG` - All debug logs

### Key Files
- `LoginFragment.java` - Location capture at login
- `EventDetailFragment.java` - Location usage when joining event
- `EventService.java` - Location storage logic
- `SessionLocationManager.java` - SharedPreferences management
- `LocationService.java` - Location retrieval from device
- `AndroidManifest.xml` - API key configuration

### Important Constants
- SharedPreferences file: `session_prefs`
- Keys: `session_location_lat`, `session_location_lng`
- Permission request code: `1001` (LocationPermissionHelper)

---

## 9. ✅ Next Steps

1. **Add API key** to `AndroidManifest.xml` (see Step 4 above)
2. **Enable APIs** in Google Cloud Console (see Step 2 above)
3. **Test location capture** at login (check Logcat)
4. **Test Places Autocomplete** (should not crash)
5. **Create event with geolocation enabled** (check checkbox)
6. **Join event** and verify location is stored (check Firestore)

---

## 10. 📞 Still Having Issues?

Check these in order:
1. **Logcat** - Look for error messages
2. **API Key** - Verify it's correct and not restricted incorrectly
3. **Permissions** - Verify location permission is granted
4. **Location Services** - Verify GPS is enabled
5. **Event Settings** - Verify `geolocationRequired` is `true` in Firestore

