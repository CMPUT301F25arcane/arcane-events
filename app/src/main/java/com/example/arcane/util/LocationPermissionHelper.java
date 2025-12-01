/**
 * LocationPermissionHelper.java
 * 
 * Purpose: Utility class for handling Android location permission requests and checks.
 * 
 * Design Pattern: Utility class pattern. Provides static helper methods for location
 * permission operations, abstracting Android's permission system complexity.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Utility class for managing location permissions.
 *
 * <p>Provides helper methods to check and request location permissions
 * in a consistent way across the app.</p>
 *
 * @version 1.0
 */
public final class LocationPermissionHelper {

    // Permission request code constant
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private LocationPermissionHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if the app has been granted location permissions.
     *
     * <p>Checks both FINE and COARSE location permissions. Returns true
     * if at least one is granted (COARSE is sufficient for basic location).</p>
     *
     * @param context the application context
     * @return true if location permission is granted, false otherwise
     */
    public static boolean hasLocationPermission(@NonNull Context context) {
        int fineLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        );
        int coarseLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
        );

        // Return true if either permission is granted
        return fineLocation == PackageManager.PERMISSION_GRANTED ||
               coarseLocation == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks if fine location permission is specifically granted.
     *
     * <p>Fine location provides more precise GPS coordinates.
     * Use this when you need accurate location data.</p>
     *
     * @param context the application context
     * @return true if fine location permission is granted, false otherwise
     */
    public static boolean hasFineLocationPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests location permissions from a Fragment.
     *
     * <p>Shows the system permission dialog to the user. The result
     * will be delivered to the fragment's onRequestPermissionsResult callback.</p>
     *
     * @param fragment the fragment requesting permissions
     */
    public static void requestLocationPermission(@NonNull Fragment fragment) {
        fragment.requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Requests location permissions from a FragmentActivity.
     *
     * <p>Shows the system permission dialog to the user. The result
     * will be delivered to the activity's onRequestPermissionsResult callback.</p>
     *
     * @param activity the activity requesting permissions
     */
    public static void requestLocationPermission(@NonNull FragmentActivity activity) {
        activity.requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    /**
     * Checks if a permission result indicates that location permission was granted.
     *
     * <p>Helper method to verify permission results in onRequestPermissionsResult callback.</p>
     *
     * @param grantResults the permission grant results array
     * @return true if location permission was granted, false otherwise
     */
    public static boolean isLocationPermissionGranted(@NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return false;
        }

        // Check if at least one location permission was granted
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the permission request code matches the location permission request.
     *
     * <p>Helper method to verify that a permission result callback is for location permissions.</p>
     *
     * @param requestCode the request code from onRequestPermissionsResult
     * @return true if this is a location permission request, false otherwise
     */
    public static boolean isLocationPermissionRequest(int requestCode) {
        return requestCode == LOCATION_PERMISSION_REQUEST_CODE;
    }
}

