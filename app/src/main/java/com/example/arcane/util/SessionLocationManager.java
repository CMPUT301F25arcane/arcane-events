/**
 * SessionLocationManager.java
 * 
 * Purpose: Utility class for managing user's location during their login session.
 * 
 * Design Pattern: Utility class pattern. Provides static helper methods for session
 * location storage and retrieval using SharedPreferences.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.GeoPoint;

/**
 * Utility class for managing session location storage.
 *
 * <p>Stores the user's location captured at login in SharedPreferences.
 * This location is used when the user joins events during their session.
 * The location is cleared when the user logs out.</p>
 *
 * @version 1.0
 */
public final class SessionLocationManager {

    // SharedPreferences keys for session location
    private static final String PREFS_NAME = "session_prefs";
    private static final String KEY_SESSION_LOCATION_LAT = "session_location_lat";
    private static final String KEY_SESSION_LOCATION_LNG = "session_location_lng";

    private SessionLocationManager() {
        // Utility class - prevent instantiation
    }

    /**
     * Saves the session location to SharedPreferences.
     *
     * <p>Stores latitude and longitude as doubles in SharedPreferences.
     * This location will be used when the user joins events during this session.</p>
     *
     * @param context the application context
     * @param location the location to save as a GeoPoint
     */
    public static void saveSessionLocation(@NonNull Context context, @NonNull GeoPoint location) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_SESSION_LOCATION_LAT, Double.doubleToLongBits(location.getLatitude()));
        editor.putLong(KEY_SESSION_LOCATION_LNG, Double.doubleToLongBits(location.getLongitude()));
        editor.apply();
    }

    /**
     * Retrieves the session location from SharedPreferences.
     *
     * <p>Returns the location that was captured at login and stored in session.
     * Returns null if no location has been stored for this session.</p>
     *
     * @param context the application context
     * @return the session location as a GeoPoint, or null if not set
     */
    @Nullable
    public static GeoPoint getSessionLocation(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Check if location exists (using a default value that indicates "not set")
        // We use Long.MIN_VALUE as a sentinel value since valid coordinates can't be that
        long latBits = prefs.getLong(KEY_SESSION_LOCATION_LAT, Long.MIN_VALUE);
        long lngBits = prefs.getLong(KEY_SESSION_LOCATION_LNG, Long.MIN_VALUE);
        
        if (latBits == Long.MIN_VALUE || lngBits == Long.MIN_VALUE) {
            // Location not set
            return null;
        }
        
        // Convert back from long bits to double
        double latitude = Double.longBitsToDouble(latBits);
        double longitude = Double.longBitsToDouble(lngBits);
        
        return new GeoPoint(latitude, longitude);
    }

    /**
     * Checks if a session location exists.
     *
     * <p>Returns true if a location has been stored for this session, false otherwise.</p>
     *
     * @param context the application context
     * @return true if session location exists, false otherwise
     */
    public static boolean hasSessionLocation(@NonNull Context context) {
        return getSessionLocation(context) != null;
    }

    /**
     * Clears the session location from SharedPreferences.
     *
     * <p>Should be called when the user logs out to ensure location is not
     * persisted across sessions.</p>
     *
     * @param context the application context
     */
    public static void clearSessionLocation(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_SESSION_LOCATION_LAT);
        editor.remove(KEY_SESSION_LOCATION_LNG);
        editor.apply();
    }
}

