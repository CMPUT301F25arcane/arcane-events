# Firebase Setup Guide

## Do You Need to Initialize Collections?

**NO!** Firebase Firestore automatically creates collections when you write the first document. You don't need to manually create collections.

**However**, you DO need to:
1. Set up Firebase project in Firebase Console
2. Add Firebase configuration to your app
3. Create indexes for collection group queries (optional but recommended)

---

## Step-by-Step Firebase Setup

### Step 1: Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" or select existing project
3. Follow the wizard:
   - Enter project name: "Arcane Events" (or your choice)
   - Enable Google Analytics (optional)
   - Click "Create project"

### Step 2: Add Android App to Firebase
1. In Firebase Console, click the Android icon (or "Add app")
2. Enter package name: `com.example.arcane` (from your `build.gradle.kts`)
3. Download `google-services.json`
4. Place it in: `arcane-events/app/google-services.json`

### Step 3: Enable Firestore Database
1. In Firebase Console, go to "Build" → "Firestore Database"
2. Click "Create database"
3. Choose location (e.g., `us-central1`)
4. Start in **Test mode** (for development) - allows read/write for 30 days
5. Click "Enable"

### Step 4: Create Firestore Indexes (Required for Collection Group Queries)
1. Go to Firestore Database → "Indexes" tab
2. Click "Create Index"
3. Create these indexes:

**Index 1:**
- Collection ID: `waitingList` (collection group)
- Fields: `entrantId` (Ascending)
- Click "Create"

**Index 2:**
- Collection ID: `decisions` (collection group)
- Fields: `entrantId` (Ascending)
- Click "Create"

**Note:** These indexes allow querying across all events (e.g., "get all events user registered for")

---

## Collections Will Be Created Automatically

When you first write data, Firestore creates these collections automatically:

```
/users/{userId}                          ← Created when UserService.createUser() runs
/events/{eventId}                        ← Created when EventService.createEvent() runs
/events/{eventId}/waitingList/{entryId}  ← Created when EventService.joinWaitingList() runs
/events/{eventId}/decisions/{decisionId} ← Created when EventService.joinWaitingList() runs
```

**You don't need to create them manually!**

---

## Test Mode Security Rules (Development)

For development, your Firestore rules should look like:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.time < timestamp.date(2025, 12, 31);
    }
  }
}
```

**⚠️ Important:** Change these rules before production! Use proper authentication-based rules.

---

## Verify Setup

1. Run your app
2. Create a user (sign up)
3. Check Firebase Console → Firestore Database
4. You should see `/users/{userId}` collection created automatically!

---

## Next Steps

1. ✅ Firebase project created
2. ✅ `google-services.json` added to app
3. ✅ Firestore enabled
4. ✅ Indexes created (for collection group queries)
5. ✅ Ready to use the code!

