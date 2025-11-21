/**
 * MainActivity.java
 * 
 * Purpose: Main activity that hosts all fragments and manages navigation.
 * 
 * Design Pattern: Single Activity pattern with Navigation Component. Manages
 * bottom navigation visibility and action bar visibility based on current destination.
 * Uses ViewBinding for type-safe view access.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane;

import android.os.Bundle;
import android.graphics.Color;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.arcane.databinding.ActivityMainBinding;
import com.example.arcane.R;
import com.example.arcane.model.Users;
import com.example.arcane.model.UserProfile;
import com.example.arcane.repository.UserRepository;

/**
 * Main activity for the Arcane application.
 *
 * <p>Manages navigation, bottom navigation bar visibility, and action bar visibility
 * based on the current destination.</p>
 *
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState the saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configureStatusBar();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        
        // Setup bottom nav menu based on user role (default to scan, admin gets gallery)
        // This will configure NavigationUI appropriately for each role
        setupBottomNavigationMenu(navView, navController);
        
        // Update bottom nav title based on user role
        updateBottomNavTitle();

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.navigation_welcome
                    || destination.getId() == R.id.navigation_login
                    || destination.getId() == R.id.navigation_create_account
                    || destination.getId() == R.id.navigation_create_event
                    || destination.getId() == R.id.navigation_edit_event
                    || destination.getId() == R.id.navigation_qr_code
                    || destination.getId() == R.id.navigation_dashboard) {
                binding.navView.setVisibility(android.view.View.GONE);
            } else {
                binding.navView.setVisibility(android.view.View.VISIBLE);
            }
            
            // Update action bar title based on role for home destination
            if (destination.getId() == R.id.navigation_home) {
                // Use post-delay to ensure fragment is loaded and Firebase query can complete
                binding.getRoot().postDelayed(() -> {
                updateActionBarTitleForHome();
                    checkAdminAndSetupToolbar(); // Setup custom toolbar for admin
                }, 300);
            } else {
                // Reset action bar when not on events tab
                resetActionBar();
            }
            
            // Disable back button for top-level destinations (no back button should show)
            if (getSupportActionBar() != null) {
                boolean isTopLevel = destination.getId() == R.id.navigation_home || 
                                     destination.getId() == R.id.navigation_dashboard || 
                                     destination.getId() == R.id.navigation_browse_images || 
                                     destination.getId() == R.id.navigation_notifications;
                getSupportActionBar().setDisplayHomeAsUpEnabled(!isTopLevel);
            }

            // Hide action bar for welcome, create event, and QR scanner pages (they have their own toolbars)
            if (destination.getId() == R.id.navigation_welcome
                    || destination.getId() == R.id.navigation_create_event
                    || destination.getId() == R.id.navigation_edit_event
                    || destination.getId() == R.id.navigation_qr_code
                    || destination.getId() == R.id.navigation_browse_images
                    || destination.getId() == R.id.navigation_dashboard) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().hide();
                }
                // Remove top padding to eliminate gap
                if (destination.getId() == R.id.navigation_create_event
                        || destination.getId() == R.id.navigation_edit_event
                        || destination.getId() == R.id.navigation_qr_code
                        || destination.getId() == R.id.navigation_browse_images
                        || destination.getId() == R.id.navigation_dashboard) {
                    binding.container.setPadding(
                        binding.container.getPaddingLeft(),
                        0,
                        binding.container.getPaddingRight(),
                        binding.container.getPaddingBottom()
                    );
                }
            } else {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().show();
                }
                // Restore top padding for other pages
                if (destination.getId() != R.id.navigation_welcome
                        && destination.getId() != R.id.navigation_create_event
                        && destination.getId() != R.id.navigation_edit_event
                        && destination.getId() != R.id.navigation_qr_code
                        && destination.getId() != R.id.navigation_browse_images
                        && destination.getId() != R.id.navigation_dashboard) {
                    android.util.TypedValue tv = new android.util.TypedValue();
                    if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                        int actionBarSize = android.util.TypedValue.complexToDimensionPixelSize(
                            tv.data, getResources().getDisplayMetrics()
                        );
                        binding.container.setPadding(
                            binding.container.getPaddingLeft(),
                            actionBarSize,
                            binding.container.getPaddingRight(),
                            binding.container.getPaddingBottom()
                        );
                    }
                }
            }
        });
    }

    private void configureStatusBar() {
        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decorView);
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
        }
        getWindow().setStatusBarColor(Color.WHITE);
    }

    /**
     * Sets up the bottom navigation menu based on user role.
     * Admin users get gallery button, others get scan button.
     * Can be called to refresh the menu when role changes.
     */
    public void setupBottomNavigationMenu(BottomNavigationView navView, NavController navController) {
        if (navView == null || navController == null) return;
        
        getUserRole(role -> {
            boolean isAdmin = role != null && "ADMIN".equals(role.toUpperCase().trim());
            
            if (isAdmin) {
                // Admin: Rebuild menu with gallery in the middle position
                android.view.Menu menu = navView.getMenu();
                menu.clear();
                
                // Rebuild menu in correct order: Events (0), Gallery (1), Profile (2)
                android.view.MenuItem homeItem = menu.add(android.view.Menu.NONE, R.id.navigation_home, 0, getString(R.string.title_events));
                homeItem.setIcon(R.drawable.ic_nav_events);
                
                android.view.MenuItem galleryItem = menu.add(android.view.Menu.NONE, R.id.navigation_browse_images, 1, "Gallery");
                galleryItem.setIcon(R.drawable.ic_gallery);
                
                android.view.MenuItem profileItem = menu.add(android.view.Menu.NONE, R.id.navigation_notifications, 2, getString(R.string.title_profile));
                profileItem.setIcon(R.drawable.ic_nav_profile);
                
                // Update home title for admin
                homeItem.setTitle("All Events");
            } else {
                // Non-admin: Rebuild menu with scan in the middle position
                android.view.Menu menu = navView.getMenu();
                menu.clear();
                
                // Rebuild menu in correct order: Events (0), Scan (1), Profile (2)
                android.view.MenuItem homeItem = menu.add(android.view.Menu.NONE, R.id.navigation_home, 0, getString(R.string.title_events));
                homeItem.setIcon(R.drawable.ic_nav_events);
                
                android.view.MenuItem scanItem = menu.add(android.view.Menu.NONE, R.id.navigation_dashboard, 1, getString(R.string.title_scan));
                scanItem.setIcon(R.drawable.ic_nav_scan);
                
                android.view.MenuItem profileItem = menu.add(android.view.Menu.NONE, R.id.navigation_notifications, 2, getString(R.string.title_profile));
                profileItem.setIcon(R.drawable.ic_nav_profile);
            }
            
            // Setup AppBarConfiguration with appropriate top-level destinations
            int[] topLevelDestinations;
            if (isAdmin) {
                topLevelDestinations = new int[]{
                    R.id.navigation_home, 
                    R.id.navigation_browse_images, 
                    R.id.navigation_notifications
                };
            } else {
                topLevelDestinations = new int[]{
                    R.id.navigation_home, 
                    R.id.navigation_dashboard, 
                    R.id.navigation_notifications
                };
            }
            
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navView, navController);
        });
    }

    /**
     * Updates the bottom navigation menu title based on user role.
     * Can be called from fragments to refresh the title after role is loaded.
     */
    public void updateBottomNavTitle() {
        if (binding == null) return;
        BottomNavigationView navView = binding.navView;
        if (navView == null) return;
        
        getUserRole(role -> {
            if (role != null && "ADMIN".equals(role.toUpperCase().trim())) {
                navView.getMenu().findItem(R.id.navigation_home).setTitle("All Events");
            } else {
                navView.getMenu().findItem(R.id.navigation_home).setTitle(getString(R.string.title_events));
            }
        });
    }

    /**
     * Updates the action bar title based on the current user's role.
     * Can be called from fragments to refresh the title after role is loaded.
     */
    public void updateActionBarTitleForHome() {
        if (getSupportActionBar() == null) return;
        
        getUserRole(role -> {
            if (role != null) {
                String r = role.toUpperCase().trim();
                if ("ADMIN".equals(r)) {
                    // Admin uses custom toolbar, title is set in setupAdminToolbar()
                } else if ("ORGANISER".equals(r) || "ORGANIZER".equals(r)) {
                    getSupportActionBar().setTitle("My Events (Organizer)");
                } else {
                    getSupportActionBar().setTitle("My Events");
                }
            } else {
                getSupportActionBar().setTitle("My Events");
            }
        });
    }

    /**
     * Helper method to get user role from Firebase.
     * Extracts the common role-checking logic used across multiple methods.
     *
     * @param callback callback that receives the role (or null if not found)
     */
    private void getUserRole(java.util.function.Consumer<String> callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            callback.accept(null);
            return;
        }

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
                    callback.accept(role);
                })
                .addOnFailureListener(e -> callback.accept(null));
    }

    /**
     * Handles navigation up action.
     *
     * @return true if navigation was handled, false otherwise
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }


    /**
     * Sets up the custom admin toolbar view.
     */
    private void setupAdminToolbar() {
        if (getSupportActionBar() == null) return;
        
        // Inflate custom toolbar layout
        View customView = getLayoutInflater().inflate(R.layout.toolbar_admin_events, null);
        
        // Set title
        android.widget.TextView titleView = customView.findViewById(R.id.admin_toolbar_title);
        if (titleView != null) {
            titleView.setText("Browse Events (Admin)");
        }
        
        // Set as custom view
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setCustomView(customView, new androidx.appcompat.app.ActionBar.LayoutParams(
            androidx.appcompat.app.ActionBar.LayoutParams.MATCH_PARENT,
            androidx.appcompat.app.ActionBar.LayoutParams.MATCH_PARENT
        ));
        
        // Set up button click listeners
        android.widget.ImageButton notificationsButton = customView.findViewById(R.id.admin_notifications_button);
        android.widget.ImageButton usersButton = customView.findViewById(R.id.admin_users_button);
        
        if (notificationsButton != null) {
            notificationsButton.setOnClickListener(v -> 
                Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)
                    .navigate(R.id.navigation_all_notifications)
            );
        }
        
        if (usersButton != null) {
            usersButton.setOnClickListener(v -> 
                Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)
                    .navigate(R.id.navigation_all_users)
            );
        }
    }

    /**
     * Resets the action bar to default (removes custom view).
     */
    private void resetActionBar() {
        if (getSupportActionBar() == null) return;
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setCustomView(null);
    }

    /**
     * Checks if user is admin and sets up custom toolbar accordingly.
     */
    private void checkAdminAndSetupToolbar() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        if (navController.getCurrentDestination() == null || 
            navController.getCurrentDestination().getId() != R.id.navigation_home) {
            resetActionBar();
            return;
        }
        
        getUserRole(role -> {
            boolean isAdmin = role != null && "ADMIN".equals(role.toUpperCase().trim());
            if (isAdmin) {
                setupAdminToolbar();
            } else {
                resetActionBar();
            }
        });
    }

}