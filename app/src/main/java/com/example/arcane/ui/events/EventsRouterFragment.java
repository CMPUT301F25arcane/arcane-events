package com.example.arcane.ui.events;

/**
 * This file defines the EventsRouterFragment class, which acts as a router/container
 * fragment that dynamically displays either OrganizerEventsFragment or UserEventsFragment
 * based on the user's role stored in SharedPreferences. It listens for role changes and
 * automatically switches between the appropriate child fragment.
 *
 * Design Pattern: Router/Container Pattern
 * - Acts as a container that routes to different child fragments based on user role
 * - Uses SharedPreferences listener to react to role changes dynamically
 * - Uses child fragment manager to manage nested fragments
 *
 * Outstanding Issues:
 * - None identified at this time
 */
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.arcane.R;

/**
 * Router fragment that dynamically displays either OrganizerEventsFragment or UserEventsFragment
 * based on the user's role. Listens for role changes and automatically switches child fragments.
 *
 * @version 1.0
 */
public class EventsRouterFragment extends Fragment {

    private static final String CHILD_TAG = "events_router_child";
    private SharedPreferences sharedPreferences;

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            (prefs, key) -> {
                if ("user_role".equals(key) && isAdded()) {
                    attachChildFragment();
                }
            };

    /**
     * Called when the fragment is attached to its context.
     * Initializes SharedPreferences and registers a listener for role changes.
     *
     * @param context The context that the fragment is attached to
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    /**
     * Called when the fragment is detached from its context.
     * Unregisters the SharedPreferences listener to prevent memory leaks.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
            sharedPreferences = null;
        }
    }

    /**
     * Creates and returns the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     * @return The root View for the fragment's layout
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events_router, container, false);
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored.
     * Attaches the appropriate child fragment based on the user's role.
     *
     * @param view The View returned by onCreateView
     * @param savedInstanceState If non-null, this fragment is being reconstructed from a previous saved state
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        attachChildFragment();
    }

    /**
     * Attaches the appropriate child fragment based on the user's role.
     * Shows OrganizerEventsFragment for organizers, UserEventsFragment for regular users.
     * Only replaces the fragment if the current fragment doesn't match the required type.
     */
    private void attachChildFragment() {
        if (sharedPreferences == null || getView() == null) {
            return;
        }

        String role = sharedPreferences.getString("user_role", null);
        boolean showOrganizer = isOrganizer(role);

        Fragment current = getChildFragmentManager().findFragmentByTag(CHILD_TAG);
        if (current != null) {
            if (showOrganizer && current instanceof OrganizerEventsFragment) {
                return;
            }
            if (!showOrganizer && current instanceof UserEventsFragment) {
                return;
            }
        }

        Fragment nextFragment = showOrganizer
                ? new OrganizerEventsFragment()
                : new UserEventsFragment();

        FragmentTransaction transaction = getChildFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.events_router_container, nextFragment, CHILD_TAG);

        if (getChildFragmentManager().isStateSaved()) {
            transaction.commitAllowingStateLoss();
        } else {
            transaction.commit();
        }
    }

    /**
     * Checks if the given role string represents an organizer role.
     *
     * @param role The role string to check (case-insensitive)
     * @return true if the role is "ORGANIZER" or "ORGANISER", false otherwise
     */
    private boolean isOrganizer(@Nullable String role) {
        if (role == null) {
            return false;
        }
        String roleUpper = role.toUpperCase();
        return "ORGANIZER".equals(roleUpper) || "ORGANISER".equals(roleUpper);
    }
}

