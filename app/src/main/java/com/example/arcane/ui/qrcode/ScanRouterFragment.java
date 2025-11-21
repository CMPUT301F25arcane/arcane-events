/**
 * ScanRouterFragment.java
 * 
 * Purpose: Router fragment that dynamically displays either OrganizerScanEventsFragment
 * or QRScannerFragment based on the user's role stored in SharedPreferences.
 * 
 * Design Pattern: Router/Container pattern. Acts as a container that manages child fragments
 * based on user role, implementing dynamic fragment switching without navigation.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.qrcode;

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
 * Router fragment for managing scan display based on user role.
 *
 * <p>Dynamically displays either OrganizerScanEventsFragment or QRScannerFragment
 * based on the user's role. Listens to SharedPreferences changes to update
 * the displayed fragment when the role changes.</p>
 *
 * @version 1.0
 */
public class ScanRouterFragment extends Fragment {

    private static final String CHILD_TAG = "scan_router_child";
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
        return inflater.inflate(R.layout.fragment_scan_router, container, false);
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

        // First, use cached role from SharedPreferences for immediate routing
        String cachedRole = sharedPreferences.getString("user_role", null);
        routeToFragment(cachedRole);

        // Then verify/update from Firebase in the background
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository userRepository = new UserRepository();
            userRepository.getUserById(currentUser.getUid())
                    .addOnSuccessListener(snapshot -> {
                        if (!isAdded()) return;
                        
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
                            String currentCachedRole = sharedPreferences.getString("user_role", null);
                            if (!role.equals(currentCachedRole)) {
                                sharedPreferences.edit().putString("user_role", role).apply();
                                // Only re-route if role changed
                                routeToFragment(role);
                            }
                        } else if (role == null && cachedRole != null) {
                            // Firebase doesn't have role but we had cached - keep using cached
                            // No need to re-route
                        }
                    })
                    .addOnFailureListener(e -> {
                        // On failure, keep using cached role (already routed above)
                        // No need to do anything
                    });
        }
    }

    private void routeToFragment(@Nullable String role) {
        if (getView() == null) {
            return;
        }

        boolean showOrganizer = isOrganizer(role);

        Fragment current = getChildFragmentManager().findFragmentByTag(CHILD_TAG);
        if (current != null) {
            // Only skip replacement if the current fragment matches the desired fragment
            if (showOrganizer && current instanceof OrganizerScanEventsFragment) {
                return;
            }
            if (!showOrganizer && current instanceof QRScannerFragment) {
                return;
            }
        }

        Fragment nextFragment;
        if (showOrganizer) {
            nextFragment = new OrganizerScanEventsFragment();
        } else {
            nextFragment = new QRScannerFragment();
        }

        FragmentTransaction transaction = getChildFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.scan_router_container, nextFragment, CHILD_TAG);

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

