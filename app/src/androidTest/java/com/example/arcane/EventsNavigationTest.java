package com.example.arcane;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.arcane.model.Event;
import com.example.arcane.ui.events.EventCardAdapter;
import com.example.arcane.ui.events.OrganizerEventsFragment;
import com.example.arcane.ui.events.UserEventsFragment;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class EventsNavigationTest {

    @Test
    public void userEventsPrimaryButtonNavigatesToGlobal() {
        AtomicReference<TestNavHostController> navControllerRef = new AtomicReference<>();

        // Launch the user events fragment in isolation
        FragmentScenario<UserEventsFragment> scenario = FragmentScenario.launchInContainer(
                UserEventsFragment.class,
                null,
                R.style.Theme_Arcane,
                Lifecycle.State.RESUMED
        );

        // Attach a test NavController to intercept navigation actions
        scenario.onFragment(fragment -> {
            TestNavHostController navController = createNavController(fragment);
            navControllerRef.set(navController);
        });

        // Click the primary nav button (Go to Global Events)
        onView(withId(R.id.primary_nav_button)).perform(click());

        assertThat(navControllerRef.get().getCurrentDestination().getId(),
                equalTo(R.id.navigation_global_events));

        scenario.close();
    }

    @Test
    public void userEventCardNavigatesToDetail() {
        AtomicReference<TestNavHostController> navControllerRef = new AtomicReference<>();

        // Launch the user events fragment
        FragmentScenario<UserEventsFragment> scenario = FragmentScenario.launchInContainer(
                UserEventsFragment.class,
                null,
                R.style.Theme_Arcane,
                Lifecycle.State.RESUMED
        );

        // Attach NavController and seed the adapter with a fake event
        scenario.onFragment(fragment -> {
            TestNavHostController navController = createNavController(fragment);
            navControllerRef.set(navController);

            Event event = new Event();
            event.setEventId("event-123");
            event.setEventName("Test Event");

            EventCardAdapter adapter = getField(fragment, "adapter", EventCardAdapter.class);
            adapter.setItems(Collections.singletonList(event));

            RecyclerView recycler = fragment.requireView().findViewById(R.id.events_recycler_view);
            recycler.setItemAnimator(null);
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Click the first event card to trigger navigation
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        assertThat(navControllerRef.get().getCurrentDestination().getId(),
                equalTo(R.id.navigation_event_detail));
        assertThat(navControllerRef.get().getCurrentBackStackEntry().getArguments().getString("eventId"),
                equalTo("event-123"));

        scenario.close();
    }

    @Test
    public void organizerEventCardNavigatesToDetail() {
        AtomicReference<TestNavHostController> navControllerRef = new AtomicReference<>();

        // Launch the organizer events fragment
        FragmentScenario<OrganizerEventsFragment> scenario = FragmentScenario.launchInContainer(
                OrganizerEventsFragment.class,
                null,
                R.style.Theme_Arcane,
                Lifecycle.State.RESUMED
        );

        // Attach NavController and seed organizer-specific event data
        scenario.onFragment(fragment -> {
            TestNavHostController navController = createNavController(fragment);
            navControllerRef.set(navController);

            Event event = new Event();
            event.setEventId("organizer-event");
            event.setEventName("Organizer Event");

            EventCardAdapter adapter = getField(fragment, "adapter", EventCardAdapter.class);
            adapter.setItems(Collections.singletonList(event));

            RecyclerView recycler = fragment.requireView().findViewById(R.id.events_recycler_view);
            recycler.setItemAnimator(null);
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Click the first event card to verify detail navigation
        onView(withId(R.id.events_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        assertThat(navControllerRef.get().getCurrentDestination().getId(),
                equalTo(R.id.navigation_event_detail));
        assertThat(navControllerRef.get().getCurrentBackStackEntry().getArguments().getString("eventId"),
                equalTo("organizer-event"));

        scenario.close();
    }

    private TestNavHostController createNavController(Fragment fragment) {
        TestNavHostController navController = new TestNavHostController(
                InstrumentationRegistry.getInstrumentation().getTargetContext());
        navController.setGraph(R.navigation.mobile_navigation);
        navController.setCurrentDestination(R.id.navigation_home);
        ViewGroup content = fragment.requireActivity().findViewById(android.R.id.content);
        content.setId(R.id.nav_host_fragment_activity_main);
        Navigation.setViewNavController(content, navController);
        return navController;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

