# API Key Setup Instructions

## ✅ Current Status

Based on your Google Cloud Console screenshot, you have:
- ✅ **Two API keys created**: "API key 2" and "Maps Platform API Key"
- ✅ **Both have 31 APIs restricted** (good security practice)
- ⚠️ **No application restrictions yet** (needs to be added)

---

## 🔑 Which API Key to Use?

**Answer: You can use EITHER one, but I recommend using "Maps Platform API Key"** because:
- It has a descriptive name that makes it clear what it's for
- It's specifically created for Maps Platform APIs
- Easier to manage and identify later

---

## 📝 Step-by-Step: Add API Key to Your App

### Step 1: Get Your API Key Value

1. In Google Cloud Console, go to **"APIs & Services" → "Credentials"**
2. Find **"Maps Platform API Key"** (or "API key 2" if you prefer)
3. Click the **"Show key"** link (or the vertical ellipsis → "Show key")
4. **Copy the API key** (it starts with `AIza...`)
5. **IMPORTANT:** Keep this window open - you'll need to add restrictions next

### Step 2: Add API Key to AndroidManifest.xml

1. Open `app/src/main/AndroidManifest.xml` in Android Studio
2. Find this section (around line 25-30):
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_API_KEY" />
   ```
3. Replace `YOUR_API_KEY` with your actual API key:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="AIzaSyAQv7BMoVWByWNS_nlEi2zjdOyL_tKowtU" />
   ```
   (Use the key you copied from Step 1)
4. **Save the file**

### Step 3: Add Application Restrictions (IMPORTANT for Security)

**Why add restrictions?**
- Prevents unauthorized use of your API key
- Limits usage to your app only
- Protects against quota abuse

**How to add restrictions:**

1. In Google Cloud Console, click the **vertical ellipsis (⋮)** next to your API key
2. Select **"Edit"** (or click **"Show key"** → **"Edit"**)
3. Scroll down to **"Application restrictions"**
4. Select **"Android apps"**
5. Click **"+ Add an item"**
6. Fill in:
   - **Package name:** `com.example.arcane`
     - (Check your `build.gradle.kts` file if unsure - look for `applicationId`)
   - **SHA-1 certificate fingerprint:** Get this from Android Studio:
     - Open **Terminal** in Android Studio (bottom panel)
     - Run this command:
       ```bash
       keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
       ```
     - Look for **"SHA1:"** line
     - Copy the SHA-1 value (looks like: `AA:BB:CC:DD:EE:FF:...`)
     - Paste it into the fingerprint field
7. Scroll down to **"API restrictions"**
8. Select **"Restrict key"**
9. Check these APIs:
   - ✅ **Maps SDK for Android**
   - ✅ **Places API**
10. Click **"Save"** at the bottom

### Step 4: Rebuild Your App

1. In Android Studio, go to **Build → Rebuild Project**
2. Wait for build to complete
3. Run the app

---

## ⚠️ Important Notes About Restrictions

### For Development (Debug Builds)
- Use your **debug keystore SHA-1** (the one you just got)
- This works for testing on emulator and physical devices via USB

### For Production (Release Builds)
- You'll need to add **another SHA-1** from your release keystore
- Or create a **separate API key** for production
- Release keystore SHA-1 is different from debug keystore

### If You Don't Add Restrictions
- ⚠️ **Your API key will work, BUT:**
- Anyone who gets your API key can use it
- You'll be charged for their usage
- Your quota can be exhausted by unauthorized users
- **Highly recommended to add restrictions!**

---

## 🧪 Testing After Setup

1. **Test Places Autocomplete:**
   - Create a new event
   - Click on location input field
   - Should open Places Autocomplete (not crash)
   - Select an address
   - Should fill in the location field

2. **Test Location Capture:**
   - Log in to the app
   - Check Logcat for: `"DEBUG: SUCCESS: Location = ..."`
   - Should see location coordinates

3. **If Autocomplete Still Crashes:**
   - Check Logcat for error messages
   - Verify API key is correct in `AndroidManifest.xml`
   - Verify Maps SDK and Places API are enabled
   - Try typing location manually (should work even if autocomplete fails)

---

## 🔍 Troubleshooting

### "API key not valid" Error
- Check if API key is correct in `AndroidManifest.xml`
- Verify Maps SDK and Places API are enabled
- Check if restrictions are blocking your app (try temporarily removing restrictions to test)

### "This API key is not authorized" Error
- Verify Maps SDK and Places API are enabled in Google Cloud Console
- Check API restrictions on your key include these APIs

### Autocomplete Still Crashes
- Check Logcat for specific error
- Verify Places SDK is initialized (check logs)
- The app should now allow manual typing even if autocomplete fails

---

## 📋 Quick Checklist

- [ ] Copied API key from Google Cloud Console
- [ ] Added API key to `AndroidManifest.xml`
- [ ] Got SHA-1 fingerprint from debug keystore
- [ ] Added Android app restriction (package name + SHA-1)
- [ ] Added API restrictions (Maps SDK + Places API)
- [ ] Saved changes in Google Cloud Console
- [ ] Rebuilt project in Android Studio
- [ ] Tested Places Autocomplete
- [ ] Tested location capture at login

---

## 💡 Pro Tips

1. **Keep API keys secure:**
   - Don't commit API keys to public repositories
   - Use environment variables or secure storage for production

2. **Monitor usage:**
   - Check Google Cloud Console → "APIs & Services" → "Dashboard"
   - Set up billing alerts if needed

3. **Multiple environments:**
   - Consider separate API keys for:
     - Development (debug builds)
     - Staging
     - Production (release builds)

4. **If you need to test without restrictions:**
   - Temporarily set restrictions to "None"
   - Test your app
   - **Remember to add restrictions back!**

