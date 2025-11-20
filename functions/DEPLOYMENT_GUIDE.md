# Cloud Functions Deployment Guide

## Prerequisites

1. **Node.js 18+** installed
2. **Firebase CLI** installed: `npm install -g firebase-tools`
3. **Firebase project** set up in Firebase Console

## Setup Steps

### 1. Install Dependencies

```bash
cd functions
npm install
```

### 2. Login to Firebase

```bash
firebase login
```

### 3. Initialize Firebase Functions (if not already done)

```bash
firebase init functions
```

When prompted:
- Select your Firebase project
- Choose JavaScript
- Install dependencies: Yes

### 4. Deploy the Function

```bash
# Deploy only the email update function
firebase deploy --only functions:updateUserEmail

# Or deploy all functions
firebase deploy --only functions
```

### 5. Verify Deployment

After deployment, you'll see a URL like:
```
https://[region]-[project-id].cloudfunctions.net/updateUserEmail
```

## Testing Locally (Optional)

### 1. Start Firebase Emulators

```bash
firebase emulators:start --only functions
```

### 2. Update Android App for Local Testing

In `EmailUpdateService.java`, uncomment:
```java
functions.useEmulator("10.0.2.2", 5001);
```

Note: `10.0.2.2` is the Android emulator's alias for `localhost`.

## Security Considerations

1. **Authentication**: The function requires the user to be authenticated (verified via ID token)
2. **Password Verification**: The function accepts the password, but you may want to add additional server-side verification
3. **Rate Limiting**: Consider adding rate limiting to prevent abuse
4. **Logging**: Monitor function logs in Firebase Console for suspicious activity

## Monitoring

View function logs:
```bash
firebase functions:log
```

Or in Firebase Console:
- Go to Functions → Logs

## Troubleshooting

### Function Not Found
- Ensure the function is deployed: `firebase deploy --only functions:updateUserEmail`
- Check the function name matches exactly: `updateUserEmail`

### Authentication Errors
- Ensure the user is logged in before calling the function
- Check that Firebase Auth is properly configured in your Android app

### Permission Denied
- Check Firebase Console → Functions → Permissions
- Ensure the function has proper IAM roles

## Cost Considerations

Cloud Functions have a free tier:
- 2 million invocations/month (free)
- 400,000 GB-seconds/month (free)
- 200,000 CPU-seconds/month (free)

Beyond that, you pay per invocation and compute time.

