package com.example.arcane.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.GeoPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Unit tests for SessionLocationManager.
 *
 * Tests session location storage and retrieval using SharedPreferences.
 * Uses Robolectric to provide Android Context for testing.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SessionLocationManagerTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.application;
        // Clear any existing session location before each test
        SessionLocationManager.clearSessionLocation(context);
    }

    @Test
    public void testSaveSessionLocation_validGeoPoint_savesCorrectly() {
        // Arrange
        GeoPoint edmontonLocation = new GeoPoint(53.5461, -113.4938);

        // Act
        SessionLocationManager.saveSessionLocation(context, edmontonLocation);
        GeoPoint retrieved = SessionLocationManager.getSessionLocation(context);

        // Assert
        assertNotNull("Retrieved location should not be null", retrieved);
        assertEquals("Latitude should match", 53.5461, retrieved.getLatitude(), 0.0001);
        assertEquals("Longitude should match", -113.4938, retrieved.getLongitude(), 0.0001);
    }

    @Test
    public void testGetSessionLocation_noLocationSaved_returnsNull() {
        // Act
        GeoPoint retrieved = SessionLocationManager.getSessionLocation(context);

        // Assert
        assertNull("Retrieved location should be null when nothing saved", retrieved);
    }

    @Test
    public void testHasSessionLocation_afterSave_returnsTrue() {
        // Arrange
        GeoPoint location = new GeoPoint(53.5461, -113.4938);
        SessionLocationManager.saveSessionLocation(context, location);

        // Act
        boolean hasLocation = SessionLocationManager.hasSessionLocation(context);

        // Assert
        assertTrue("hasSessionLocation should return true after saving", hasLocation);
    }

    @Test
    public void testHasSessionLocation_noLocationSaved_returnsFalse() {
        // Act
        boolean hasLocation = SessionLocationManager.hasSessionLocation(context);

        // Assert
        assertFalse("hasSessionLocation should return false when nothing saved", hasLocation);
    }

    @Test
    public void testClearSessionLocation_afterSave_removesLocation() {
        // Arrange
        GeoPoint location = new GeoPoint(53.5461, -113.4938);
        SessionLocationManager.saveSessionLocation(context, location);
        assertTrue("Location should exist after save", SessionLocationManager.hasSessionLocation(context));

        // Act
        SessionLocationManager.clearSessionLocation(context);

        // Assert
        assertFalse("hasSessionLocation should return false after clear", SessionLocationManager.hasSessionLocation(context));
        assertNull("getSessionLocation should return null after clear", SessionLocationManager.getSessionLocation(context));
    }

    @Test
    public void testSaveSessionLocation_overwritesPreviousLocation() {
        // Arrange
        GeoPoint firstLocation = new GeoPoint(10.0, 20.0);
        GeoPoint secondLocation = new GeoPoint(30.0, 40.0);

        // Act
        SessionLocationManager.saveSessionLocation(context, firstLocation);
        GeoPoint afterFirst = SessionLocationManager.getSessionLocation(context);

        SessionLocationManager.saveSessionLocation(context, secondLocation);
        GeoPoint afterSecond = SessionLocationManager.getSessionLocation(context);

        // Assert
        assertNotNull("First location should be saved", afterFirst);
        assertEquals("First save should have correct latitude", 10.0, afterFirst.getLatitude(), 0.0001);
        assertEquals("First save should have correct longitude", 20.0, afterFirst.getLongitude(), 0.0001);

        assertNotNull("Second location should overwrite first", afterSecond);
        assertEquals("Second save should have correct latitude", 30.0, afterSecond.getLatitude(), 0.0001);
        assertEquals("Second save should have correct longitude", 40.0, afterSecond.getLongitude(), 0.0001);
        assertNotEquals("New location should be different from first", afterFirst.getLatitude(), afterSecond.getLatitude(), 0.0001);
    }

    @Test
    public void testSaveSessionLocation_extremeCoordinates_savesCorrectly() {
        // Arrange - North Pole coordinates
        GeoPoint northPole = new GeoPoint(90.0, 0.0);

        // Act
        SessionLocationManager.saveSessionLocation(context, northPole);
        GeoPoint retrieved = SessionLocationManager.getSessionLocation(context);

        // Assert
        assertNotNull("Retrieved location should not be null", retrieved);
        assertEquals("North Pole latitude should match", 90.0, retrieved.getLatitude(), 0.0001);
        assertEquals("North Pole longitude should match", 0.0, retrieved.getLongitude(), 0.0001);
    }

    @Test
    public void testSaveSessionLocation_negativeCoordinates_savesCorrectly() {
        // Arrange - Sydney, Australia coordinates (southern/western hemisphere)
        GeoPoint sydneyLocation = new GeoPoint(-33.8688, 151.2093);

        // Act
        SessionLocationManager.saveSessionLocation(context, sydneyLocation);
        GeoPoint retrieved = SessionLocationManager.getSessionLocation(context);

        // Assert
        assertNotNull("Retrieved location should not be null", retrieved);
        assertEquals("Sydney latitude should match", -33.8688, retrieved.getLatitude(), 0.0001);
        assertEquals("Sydney longitude should match", 151.2093, retrieved.getLongitude(), 0.0001);
    }

    @Test
    public void testGetSessionLocation_corruptedData_handlesGracefully() {
        // Arrange - Manually corrupt SharedPreferences by setting only one coordinate
        SharedPreferences prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        // Set only latitude, leave longitude at sentinel value
        editor.putLong("session_location_lat", Double.doubleToLongBits(53.5461));
        // Deliberately don't set longitude (it will be Long.MIN_VALUE)
        editor.apply();

        // Act
        GeoPoint retrieved = SessionLocationManager.getSessionLocation(context);

        // Assert
        assertNull("Should return null when data is corrupted/incomplete", retrieved);
    }
}
