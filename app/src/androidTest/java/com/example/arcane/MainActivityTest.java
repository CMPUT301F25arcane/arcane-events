package com.example.arcane;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.matcher.ViewMatchers.Visibility;

/**
 * Instrumentation tests for {@link MainActivity} verifying navigation-driven UI behaviors.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @BeforeClass
    public static void setUpClass() {
        // Sign out user before any activity is created
        FirebaseAuth.getInstance().signOut();
        // Clear shared preferences
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        // Ensure user is signed out
        FirebaseAuth.getInstance().signOut();
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().clear().commit();
        
        // Wait for activity to fully initialize
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        
        // Give time for MainActivity's post() callback to complete
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    @Test
    public void navViewHiddenOnWelcomeDestination() {
        // Wait for activity to be fully ready
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        
        // Give time for MainActivity's post() callback and fragment initialization
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Verify we're on welcome screen and nav view is hidden
        activityRule.getScenario().onActivity(activity -> {
            NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_activity_main);
            androidx.navigation.NavDestination currentDest = navController.getCurrentDestination();
            assertNotNull("Current destination should not be null", currentDest);
            assertEquals("Should be on welcome screen", R.id.navigation_welcome, currentDest.getId());
            
            BottomNavigationView navView = activity.findViewById(R.id.nav_view);
            assertNotNull("Bottom navigation view should exist", navView);
            
            // Force a layout pass to ensure visibility is applied
            navView.requestLayout();
            navView.invalidate();
            
            int visibility = navView.getVisibility();
            assertEquals("Bottom nav should be gone on welcome destination. Current visibility: " + visibility + 
                    " (0=VISIBLE, 4=INVISIBLE, 8=GONE)", View.GONE, visibility);
        });
    }

    @Test
    public void navViewVisibleOnHomeDestination() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        
        // Navigate to home
        activityRule.getScenario().onActivity(activity -> {
            NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_activity_main);
            try {
                navController.navigate(R.id.navigation_home);
            } catch (Exception e) {
                // If navigation fails, we can't test this scenario
                throw new AssertionError("Failed to navigate to home: " + e.getMessage(), e);
            }
        });
        
        // Wait for navigation and UI updates
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        
        // Give time for destination changed listener to fire and UI to update
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Verify we're on home and nav view is visible
        activityRule.getScenario().onActivity(activity -> {
            NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_activity_main);
            androidx.navigation.NavDestination currentDest = navController.getCurrentDestination();
            
            assertNotNull("Current destination should not be null", currentDest);
            assertEquals("Should be on home screen after navigation", R.id.navigation_home, currentDest.getId());
            
            BottomNavigationView navView = activity.findViewById(R.id.nav_view);
            assertNotNull("Bottom navigation view should exist", navView);
            
            // Force a layout pass to ensure visibility is applied
            navView.requestLayout();
            navView.invalidate();
            
            int visibility = navView.getVisibility();
            assertEquals("Bottom nav should be visible on home destination. Current visibility: " + visibility + 
                    " (0=VISIBLE, 4=INVISIBLE, 8=GONE)", View.VISIBLE, visibility);
        });
    }

    @Test
    public void navViewHiddenAndActionBarHiddenOnCreateEvent() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        
        // Navigate to create event
        activityRule.getScenario().onActivity(activity -> {
            NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_activity_main);
            try {
                navController.navigate(R.id.navigation_create_event);
            } catch (Exception e) {
                // If navigation fails, we can't test this scenario
                throw new AssertionError("Failed to navigate to create event: " + e.getMessage(), e);
            }
        });
        
        // Wait for navigation and UI updates
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        
        // Give time for destination changed listener to fire and UI to update
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Verify we're on create event and nav view is hidden
        activityRule.getScenario().onActivity(activity -> {
            NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_activity_main);
            androidx.navigation.NavDestination currentDest = navController.getCurrentDestination();
            
            assertNotNull("Current destination should not be null", currentDest);
            assertEquals("Should be on create event screen after navigation", R.id.navigation_create_event, currentDest.getId());
            
            BottomNavigationView navView = activity.findViewById(R.id.nav_view);
            assertNotNull("Bottom navigation view should exist", navView);
            
            // Force a layout pass to ensure visibility is applied
            navView.requestLayout();
            navView.invalidate();
            
            int visibility = navView.getVisibility();
            assertEquals("Bottom nav should be gone on create event destination. Current visibility: " + visibility + 
                    " (0=VISIBLE, 4=INVISIBLE, 8=GONE)", View.GONE, visibility);
            
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                assertFalse("ActionBar should be hidden on create event destination", actionBar.isShowing());
            }
        });
    }
}

