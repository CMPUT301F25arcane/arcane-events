/**
 * EventsRouterFragment.java
 * 
 * Purpose: Router fragment that dynamically displays either OrganizerEventsFragment
 * or UserEventsFragment based on the user's role stored in SharedPreferences.
 * 
 * Design Pattern: Router/Container pattern. Acts as a container that manages child fragments
 * based on user role, implementing dynamic fragment switching without navigation.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.events;

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
 * Router fragment for managing events display based on user role.
 *
 * <p>Dynamically displays either OrganizerEventsFragment or UserEventsFragment
 * based on the user's role. Listens to SharedPreferences changes to update
 * the displayed fragment when the role changes.</p>
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
            sharedPreferences = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events_router, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        attachChildFragment();
    }

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

    private boolean isOrganizer(@Nullable String role) {
        if (role == null) {
            return false;
        }
        String roleUpper = role.toUpperCase();
        return "ORGANIZER".equals(roleUpper) || "ORGANISER".equals(roleUpper);
    }
}

