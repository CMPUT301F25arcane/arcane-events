/**
 * LocationService.java
 * 
 * Purpose: Utility class for getting the device's current location using Google Play Services.
 * 
 * Design Pattern: Service utility pattern. Provides a clean interface for location operations,
 * abstracting the complexity of Android's location services and converting to Firestore GeoPoint.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.util;

import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.GeoPoint;

/**
 * Utility class for getting device location.
 *
 * <p>Uses Google Play Services FusedLocationProviderClient to get
 * the device's current location and convert it to Firestore GeoPoint format.</p>
 *
 * @version 1.0
 */
public final class LocationService {

    private LocationService() {
        // Utility class - prevent instantiation
    }

    /**
     * Callback interface for location retrieval results.
     */
    public interface LocationCallback {
        /**
         * Called when location is successfully retrieved.
         *
         * @param geoPoint the location as a Firestore GeoPoint
         */
        void onLocationSuccess(@NonNull GeoPoint geoPoint);

        /**
         * Called when location retrieval fails.
         *
         * @param exception the exception that occurred
         */
        void onLocationFailure(@NonNull Exception exception);
    }

    /**
     * Gets the device's current location.
     *
     * <p>Uses FusedLocationProviderClient to get the most recent location.
     * The location is converted to a Firestore GeoPoint and passed to the callback.</p>
     *
     * <p><strong>Important:</strong> This method requires location permissions to be granted.
     * Use {@link LocationPermissionHelper#hasLocationPermission(Context)} to check before calling.</p>
     *
     * @param context the application context
     * @param callback the callback to receive the location result
     */
    public static void getCurrentLocation(@NonNull Context context, @NonNull LocationCallback callback) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        // Request high accuracy location (GPS)
        Task<Location> locationTask = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null // No cancellation token
        );

        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(@Nullable Location location) {
                if (location != null) {
                    // Convert Android Location to Firestore GeoPoint
                    GeoPoint geoPoint = new GeoPoint(
                            location.getLatitude(),
                            location.getLongitude()
                    );
                    callback.onLocationSuccess(geoPoint);
                } else {
                    // Location is null (location services may be disabled)
                    callback.onLocationFailure(
                            new Exception("Unable to get location. Please ensure location services are enabled.")
                    );
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.onLocationFailure(e);
            }
        });
    }

    /**
     * Gets the device's current location with balanced power/accuracy.
     *
     * <p>Similar to {@link #getCurrentLocation(Context, LocationCallback)} but uses
     * balanced priority, which may use less battery but might be slightly less accurate.</p>
     *
     * @param context the application context
     * @param callback the callback to receive the location result
     */
    public static void getCurrentLocationBalanced(@NonNull Context context, @NonNull LocationCallback callback) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        // Request balanced location (good accuracy, less battery usage)
        Task<Location> locationTask = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null // No cancellation token
        );

        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(@Nullable Location location) {
                if (location != null) {
                    // Convert Android Location to Firestore GeoPoint
                    GeoPoint geoPoint = new GeoPoint(
                            location.getLatitude(),
                            location.getLongitude()
                    );
                    callback.onLocationSuccess(geoPoint);
                } else {
                    // Location is null (location services may be disabled)
                    callback.onLocationFailure(
                            new Exception("Unable to get location. Please ensure location services are enabled.")
                    );
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.onLocationFailure(e);
            }
        });
    }

    /**
     * Converts an Android Location object to a Firestore GeoPoint.
     *
     * @param location the Android Location object
     * @return a Firestore GeoPoint, or null if location is null
     */
    @Nullable
    public static GeoPoint locationToGeoPoint(@Nullable Location location) {
        if (location == null) {
            return null;
        }
        return new GeoPoint(location.getLatitude(), location.getLongitude());
    }

    /**
     * Converts a Firestore GeoPoint to a string representation.
     *
     * <p>Useful for logging or display purposes.</p>
     *
     * @param geoPoint the GeoPoint to convert
     * @return a string in the format "latitude,longitude" or "null" if geoPoint is null
     */
    @NonNull
    public static String geoPointToString(@Nullable GeoPoint geoPoint) {
        if (geoPoint == null) {
            return "null";
        }
        return geoPoint.getLatitude() + "," + geoPoint.getLongitude();
    }
}

