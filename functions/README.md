# Cloud Functions for Arcane Events

This directory contains Firebase Cloud Functions that use the Admin SDK to perform privileged operations.

## Setup Instructions

### 1. Install Dependencies

```bash
cd functions
npm install
```

### 2. Deploy Functions

```bash
# Make sure you're logged into Firebase
firebase login

# Deploy the function
firebase deploy --only functions:updateUserEmail
```

### 3. Security Notes

- The `updateUserEmail` function requires the user to be authenticated
- The function uses Firebase Admin SDK which has elevated privileges
- Email verification is bypassed, so ensure your app has proper security measures
- Consider adding rate limiting to prevent abuse

## Function: updateUserEmail

**Purpose:** Updates a user's email address in Firebase Auth without requiring email verification.

**Parameters:**
- `newEmail` (string): The new email address
- `password` (string): User's current password (for verification)

**Returns:**
- Success object with `success: true`, `oldEmail`, and `newEmail`

**Errors:**
- `unauthenticated`: User is not authenticated
- `invalid-argument`: Invalid email format or missing parameters
- `already-exists`: Email is already in use
- `not-found`: User not found
- `internal`: Server error

## Testing Locally

```bash
# Start Firebase emulators
npm run serve

# Or use Firebase CLI
firebase emulators:start --only functions
```

## Production Deployment

```bash
firebase deploy --only functions
```

