package com.example.arcane.util;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

/**
 * Unit tests for LocationPermissionHelper.
 *
 * Tests permission checking and requesting logic.
 * Uses Robolectric to simulate Android permission system.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class LocationPermissionHelperTest {

    private Context context;
    private ShadowApplication shadowApp;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        shadowApp = shadowOf((Application) context);

        // Start with no permissions granted
        shadowApp.denyPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        );
    }

    @Test
    public void testHasLocationPermission_finePermissionGranted_returnsTrue() {
        // Arrange
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        // Act
        boolean hasPermission = LocationPermissionHelper.hasLocationPermission(context);

        // Assert
        assertTrue("Should return true when FINE location is granted", hasPermission);
    }

    @Test
    public void testHasLocationPermission_coarsePermissionGranted_returnsTrue() {
        // Arrange
        shadowApp.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION);

        // Act
        boolean hasPermission = LocationPermissionHelper.hasLocationPermission(context);

        // Assert
        assertTrue("Should return true when COARSE location is granted", hasPermission);
    }

    @Test
    public void testHasLocationPermission_bothPermissionsGranted_returnsTrue() {
        // Arrange
        shadowApp.grantPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        );

        // Act
        boolean hasPermission = LocationPermissionHelper.hasLocationPermission(context);

        // Assert
        assertTrue("Should return true when both permissions are granted", hasPermission);
    }

    @Test
    public void testHasLocationPermission_noPermissionsGranted_returnsFalse() {
        // No permissions granted in setUp()

        // Act
        boolean hasPermission = LocationPermissionHelper.hasLocationPermission(context);

        // Assert
        assertFalse("Should return false when no permissions are granted", hasPermission);
    }

    @Test
    public void testHasFineLocationPermission_fineGranted_returnsTrue() {
        // Arrange
        shadowApp.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        // Act
        boolean hasFinePermission = LocationPermissionHelper.hasFineLocationPermission(context);

        // Assert
        assertTrue("Should return true when FINE location is granted", hasFinePermission);
    }

    @Test
    public void testHasFineLocationPermission_onlyCoarseGranted_returnsFalse() {
        // Arrange
        shadowApp.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION);
        shadowApp.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION);

        // Act
        boolean hasFinePermission = LocationPermissionHelper.hasFineLocationPermission(context);

        // Assert
        assertFalse("Should return false when only COARSE location is granted", hasFinePermission);
    }


    @Test
    public void testIsLocationPermissionGranted_bothGranted_returnsTrue() {
        // Arrange
        int[] grantResults = {
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_GRANTED
        };

        // Act
        boolean isGranted = LocationPermissionHelper.isLocationPermissionGranted(grantResults);

        // Assert
        assertTrue("Should return true when both permissions are granted", isGranted);
    }

    @Test
    public void testIsLocationPermissionGranted_oneGranted_returnsTrue() {
        // Arrange - First granted, second denied
        int[] grantResults = {
                PackageManager.PERMISSION_GRANTED,
                PackageManager.PERMISSION_DENIED
        };

        // Act
        boolean isGranted = LocationPermissionHelper.isLocationPermissionGranted(grantResults);

        // Assert
        assertTrue("Should return true when at least one permission is granted", isGranted);
    }

    @Test
    public void testIsLocationPermissionGranted_bothDenied_returnsFalse() {
        // Arrange
        int[] grantResults = {
                PackageManager.PERMISSION_DENIED,
                PackageManager.PERMISSION_DENIED
        };

        // Act
        boolean isGranted = LocationPermissionHelper.isLocationPermissionGranted(grantResults);

        // Assert
        assertFalse("Should return false when both permissions are denied", isGranted);
    }

    @Test
    public void testIsLocationPermissionGranted_emptyArray_returnsFalse() {
        // Arrange
        int[] grantResults = {};

        // Act
        boolean isGranted = LocationPermissionHelper.isLocationPermissionGranted(grantResults);

        // Assert
        assertFalse("Should return false for empty grant results array", isGranted);
    }

    @Test
    public void testIsLocationPermissionRequest_matchingCode_returnsTrue() {
        // Act
        boolean isLocationRequest = LocationPermissionHelper.isLocationPermissionRequest(
                LocationPermissionHelper.LOCATION_PERMISSION_REQUEST_CODE
        );

        // Assert
        assertTrue("Should return true for matching request code", isLocationRequest);
    }

    @Test
    public void testIsLocationPermissionRequest_differentCode_returnsFalse() {
        // Act
        boolean isLocationRequest = LocationPermissionHelper.isLocationPermissionRequest(9999);

        // Assert
        assertFalse("Should return false for different request code", isLocationRequest);
    }

    @Test
    public void testLocationPermissionRequestCode_isCorrectValue() {
        // Assert
        assertEquals("Request code should be 1001",
                     1001,
                     LocationPermissionHelper.LOCATION_PERMISSION_REQUEST_CODE);
    }
}
