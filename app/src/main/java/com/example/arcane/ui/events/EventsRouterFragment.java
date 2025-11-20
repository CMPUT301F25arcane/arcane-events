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
import com.example.arcane.model.UserProfile;
import com.example.arcane.model.Users;
import com.example.arcane.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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

    @Override
    public void onResume() {
        super.onResume();
        // Refresh role check when fragment becomes visible (in case role changed)
        attachChildFragment();
    }

    private void attachChildFragment() {
        if (sharedPreferences == null || getView() == null) {
            return;
        }

        // Check Firebase for actual user role (more reliable than SharedPreferences)
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository userRepository = new UserRepository();
            userRepository.getUserById(currentUser.getUid())
                    .addOnSuccessListener(snapshot -> {
                        String role = null;
                        if (snapshot.exists()) {
                            // Try UserProfile first
                            UserProfile profile = snapshot.toObject(UserProfile.class);
                            if (profile != null && profile.getRole() != null) {
                                role = profile.getRole();
                            } else {
                                // Fallback to Users model
                                Users user = snapshot.toObject(Users.class);
                                if (user != null && user.getRole() != null) {
                                    role = user.getRole();
                                }
                            }
                        }
                        // Update SharedPreferences with role from Firebase (keeps cache in sync)
                        if (role != null && sharedPreferences != null) {
                            String cachedRole = sharedPreferences.getString("user_role", null);
                            if (!role.equals(cachedRole)) {
                                sharedPreferences.edit().putString("user_role", role).apply();
                            }
                        }
                        // Fallback to SharedPreferences if Firebase doesn't have role
                        if (role == null) {
                            role = sharedPreferences.getString("user_role", null);
                        }
                        routeToFragment(role);
                        // Notify MainActivity to update title after role is loaded
                        updateMainActivityTitle();
                    })
                    .addOnFailureListener(e -> {
                        // On failure, use SharedPreferences
                        String role = sharedPreferences.getString("user_role", null);
                        routeToFragment(role);
                        // Notify MainActivity to update title
                        updateMainActivityTitle();
                    });
        } else {
            // No user logged in, use SharedPreferences
            String role = sharedPreferences.getString("user_role", null);
            routeToFragment(role);
            // Notify MainActivity to update title
            updateMainActivityTitle();
        }
    }

    private void routeToFragment(@Nullable String role) {
        if (getView() == null) {
            return;
        }

        // Debug: Log the role being checked
        android.util.Log.d("EventsRouter", "Routing with role: " + role);

        boolean isAdmin = isAdmin(role);
        boolean showOrganizer = isOrganizer(role);
        
        // Debug: Log the routing decision
        android.util.Log.d("EventsRouter", "isAdmin: " + isAdmin + ", showOrganizer: " + showOrganizer);

        Fragment current = getChildFragmentManager().findFragmentByTag(CHILD_TAG);
        if (current != null) {
            android.util.Log.d("EventsRouter", "Current fragment: " + current.getClass().getSimpleName());
            // Only skip replacement if the current fragment matches the desired fragment
            if (isAdmin && current instanceof AdminEventsFragment) {
                android.util.Log.d("EventsRouter", "Already showing AdminEventsFragment, skipping");
                return;
            }
            if (showOrganizer && current instanceof OrganizerEventsFragment) {
                android.util.Log.d("EventsRouter", "Already showing OrganizerEventsFragment, skipping");
                return;
            }
            if (!isAdmin && !showOrganizer && current instanceof UserEventsFragment) {
                android.util.Log.d("EventsRouter", "Already showing UserEventsFragment, skipping");
                return;
            }
            // If we get here, we need to replace the fragment
            android.util.Log.d("EventsRouter", "Fragment mismatch detected, replacing fragment");
        }

        Fragment nextFragment;
        if (isAdmin) {
            nextFragment = new AdminEventsFragment();
            android.util.Log.d("EventsRouter", "Creating AdminEventsFragment");
        } else if (showOrganizer) {
            nextFragment = new OrganizerEventsFragment();
            android.util.Log.d("EventsRouter", "Creating OrganizerEventsFragment");
        } else {
            nextFragment = new UserEventsFragment();
            android.util.Log.d("EventsRouter", "Creating UserEventsFragment");
        }

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

    private boolean isAdmin(@Nullable String role) {
        if (role == null) {
            return false;
        }
        String roleUpper = role.toUpperCase();
        return "ADMIN".equals(roleUpper);
    }

    private boolean isOrganizer(@Nullable String role) {
        if (role == null) {
            return false;
        }
        String roleUpper = role.toUpperCase();
        return "ORGANIZER".equals(roleUpper) || "ORGANISER".equals(roleUpper);
    }

    /**
     * Notifies MainActivity to update the action bar title and bottom nav.
     * This is called after the role is loaded from Firebase.
     */
    private void updateMainActivityTitle() {
        if (getActivity() instanceof com.example.arcane.MainActivity) {
            // Use post to ensure it runs on the main thread after current operations
            getView().post(() -> {
                if (getActivity() != null) {
                    com.example.arcane.MainActivity mainActivity = (com.example.arcane.MainActivity) getActivity();
                    mainActivity.updateActionBarTitleForHome();
                    mainActivity.updateBottomNavTitle();
                }
            });
        }
    }
}

