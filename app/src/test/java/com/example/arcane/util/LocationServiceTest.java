package com.example.arcane.util;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.GeoPoint;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocationService.
 *
 * Tests location capture and conversion utilities.
 * Uses Robolectric for Android framework support and Mockito for mocking.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class LocationServiceTest {

    @Mock
    private Context mockContext;

    @Mock
    private FusedLocationProviderClient mockLocationClient;

    @Mock
    private Task<Location> mockLocationTask;

    @Mock
    private LocationService.LocationCallback mockCallback;

    private MockedStatic<LocationServices> mockedLocationServices;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() {
        // Clean up static mocks
        if (mockedLocationServices != null) {
            mockedLocationServices.close();
        }
    }

    // ==================== Helper Method Tests ====================

    @Test
    public void testLocationToGeoPoint_validLocation_convertsCorrectly() {
        // Arrange
        Location location = new Location("mock");
        location.setLatitude(53.5461);
        location.setLongitude(-113.4938);

        // Act
        GeoPoint geoPoint = LocationService.locationToGeoPoint(location);

        // Assert
        assertNotNull("GeoPoint should not be null", geoPoint);
        assertEquals("Latitude should match", 53.5461, geoPoint.getLatitude(), 0.0001);
        assertEquals("Longitude should match", -113.4938, geoPoint.getLongitude(), 0.0001);
    }

    @Test
    public void testLocationToGeoPoint_nullLocation_returnsNull() {
        // Act
        GeoPoint geoPoint = LocationService.locationToGeoPoint(null);

        // Assert
        assertNull("Should return null for null location", geoPoint);
    }

    @Test
    public void testGeoPointToString_validGeoPoint_formatsCorrectly() {
        // Arrange
        GeoPoint geoPoint = new GeoPoint(53.5461, -113.4938);

        // Act
        String result = LocationService.geoPointToString(geoPoint);

        // Assert
        assertEquals("Should format as 'lat,lng'", "53.5461,-113.4938", result);
    }

    @Test
    public void testGeoPointToString_nullGeoPoint_returnsNull() {
        // Act
        String result = LocationService.geoPointToString(null);

        // Assert
        assertEquals("Should return 'null' for null GeoPoint", "null", result);
    }

    @Test
    public void testGeoPointToString_zeroCoordinates_formatsCorrectly() {
        // Arrange - Equator and Prime Meridian intersection
        GeoPoint geoPoint = new GeoPoint(0.0, 0.0);

        // Act
        String result = LocationService.geoPointToString(geoPoint);

        // Assert
        assertEquals("Should format zero coordinates correctly", "0.0,0.0", result);
    }

    @Test
    public void testGeoPointToString_negativeCoordinates_formatsCorrectly() {
        // Arrange - Sydney, Australia (southern/eastern hemisphere)
        GeoPoint geoPoint = new GeoPoint(-33.8688, 151.2093);

        // Act
        String result = LocationService.geoPointToString(geoPoint);

        // Assert
        assertEquals("Should include negative sign for southern hemisphere", "-33.8688,151.2093", result);
    }

    @Test
    public void testLocationToGeoPoint_extremeCoordinates_convertsCorrectly() {
        // Arrange - North Pole
        Location location = new Location("mock");
        location.setLatitude(90.0);
        location.setLongitude(0.0);

        // Act
        GeoPoint geoPoint = LocationService.locationToGeoPoint(location);

        // Assert
        assertNotNull("GeoPoint should not be null", geoPoint);
        assertEquals("North Pole latitude should match", 90.0, geoPoint.getLatitude(), 0.0001);
        assertEquals("Longitude should match", 0.0, geoPoint.getLongitude(), 0.0001);
    }


    @Test
    public void testGetCurrentLocation_success_invokesSuccessCallback() {
        // Arrange
        mockedLocationServices = mockStatic(LocationServices.class);
        mockedLocationServices.when(() -> LocationServices.getFusedLocationProviderClient(any(Context.class)))
                              .thenReturn(mockLocationClient);

        Location mockLocation = new Location("mock");
        mockLocation.setLatitude(53.5461);
        mockLocation.setLongitude(-113.4938);

        when(mockLocationClient.getCurrentLocation(anyInt(), isNull())).thenReturn(mockLocationTask);

        // Capture the success listener
        ArgumentCaptor<OnSuccessListener<Location>> successCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mockLocationTask.addOnSuccessListener(successCaptor.capture())).thenReturn(mockLocationTask);
        when(mockLocationTask.addOnFailureListener(any())).thenReturn(mockLocationTask);

        // Act
        LocationService.getCurrentLocation(mockContext, mockCallback);

        // Trigger the success callback
        successCaptor.getValue().onSuccess(mockLocation);

        // Assert
        verify(mockLocationClient).getCurrentLocation(eq(Priority.PRIORITY_HIGH_ACCURACY), isNull());

        ArgumentCaptor<GeoPoint> geoPointCaptor = ArgumentCaptor.forClass(GeoPoint.class);
        verify(mockCallback).onLocationSuccess(geoPointCaptor.capture());

        GeoPoint result = geoPointCaptor.getValue();
        assertEquals("Latitude should match", 53.5461, result.getLatitude(), 0.0001);
        assertEquals("Longitude should match", -113.4938, result.getLongitude(), 0.0001);
        verify(mockCallback, never()).onLocationFailure(any());
    }

    @Test
    public void testGetCurrentLocation_nullLocation_invokesFailureCallback() {
        // Arrange
        mockedLocationServices = mockStatic(LocationServices.class);
        mockedLocationServices.when(() -> LocationServices.getFusedLocationProviderClient(any(Context.class)))
                              .thenReturn(mockLocationClient);

        when(mockLocationClient.getCurrentLocation(anyInt(), isNull())).thenReturn(mockLocationTask);

        // Capture the success listener
        ArgumentCaptor<OnSuccessListener<Location>> successCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mockLocationTask.addOnSuccessListener(successCaptor.capture())).thenReturn(mockLocationTask);
        when(mockLocationTask.addOnFailureListener(any())).thenReturn(mockLocationTask);

        // Act
        LocationService.getCurrentLocation(mockContext, mockCallback);

        // Trigger success callback with null location
        successCaptor.getValue().onSuccess(null);

        // Assert
        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(mockCallback).onLocationFailure(exceptionCaptor.capture());

        assertTrue("Exception message should mention location services",
                   exceptionCaptor.getValue().getMessage().contains("location services"));
        verify(mockCallback, never()).onLocationSuccess(any());
    }

    @Test
    public void testGetCurrentLocation_failure_invokesFailureCallback() {
        // Arrange
        mockedLocationServices = mockStatic(LocationServices.class);
        mockedLocationServices.when(() -> LocationServices.getFusedLocationProviderClient(any(Context.class)))
                              .thenReturn(mockLocationClient);

        when(mockLocationClient.getCurrentLocation(anyInt(), isNull())).thenReturn(mockLocationTask);

        // Capture the failure listener
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        when(mockLocationTask.addOnSuccessListener(any())).thenReturn(mockLocationTask);
        when(mockLocationTask.addOnFailureListener(failureCaptor.capture())).thenReturn(mockLocationTask);

        Exception testException = new Exception("Permission denied");

        // Act
        LocationService.getCurrentLocation(mockContext, mockCallback);

        // Trigger failure callback
        failureCaptor.getValue().onFailure(testException);

        // Assert
        verify(mockCallback).onLocationFailure(testException);
        verify(mockCallback, never()).onLocationSuccess(any());
    }

    @Test
    public void testGetCurrentLocationBalanced_success_invokesSuccessCallback() {
        // Arrange
        mockedLocationServices = mockStatic(LocationServices.class);
        mockedLocationServices.when(() -> LocationServices.getFusedLocationProviderClient(any(Context.class)))
                              .thenReturn(mockLocationClient);

        Location mockLocation = new Location("mock");
        mockLocation.setLatitude(53.5461);
        mockLocation.setLongitude(-113.4938);

        when(mockLocationClient.getCurrentLocation(anyInt(), isNull())).thenReturn(mockLocationTask);

        // Capture the success listener
        ArgumentCaptor<OnSuccessListener<Location>> successCaptor = ArgumentCaptor.forClass(OnSuccessListener.class);
        when(mockLocationTask.addOnSuccessListener(successCaptor.capture())).thenReturn(mockLocationTask);
        when(mockLocationTask.addOnFailureListener(any())).thenReturn(mockLocationTask);

        // Act
        LocationService.getCurrentLocationBalanced(mockContext, mockCallback);

        // Trigger the success callback
        successCaptor.getValue().onSuccess(mockLocation);

        // Assert
        verify(mockLocationClient).getCurrentLocation(eq(Priority.PRIORITY_BALANCED_POWER_ACCURACY), isNull());

        ArgumentCaptor<GeoPoint> geoPointCaptor = ArgumentCaptor.forClass(GeoPoint.class);
        verify(mockCallback).onLocationSuccess(geoPointCaptor.capture());

        GeoPoint result = geoPointCaptor.getValue();
        assertEquals("Latitude should match", 53.5461, result.getLatitude(), 0.0001);
        assertEquals("Longitude should match", -113.4938, result.getLongitude(), 0.0001);
        verify(mockCallback, never()).onLocationFailure(any());
    }
}
