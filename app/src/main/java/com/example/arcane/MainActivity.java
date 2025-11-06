package com.example.arcane;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.arcane.databinding.ActivityMainBinding;
import com.example.arcane.R;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        
        // Setup default navigation for Scan and Profile tabs
        NavigationUI.setupWithNavController(binding.navView, navController);
        
        // Override Events tab behavior to route by role
        binding.navView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                // This is the Events tab - route by role
                String userRole = getUserRole();
                
                if (isOrganizer(userRole)) {
                    navController.navigate(R.id.navigation_home); // OrganizerEventsFragment
                } else {
                    navController.navigate(R.id.navigation_user_events); // UserEventsFragment
                }
                return true; // Event handled
            }
            
            // For other tabs (Scan, Profile), use default NavigationUI behavior
            return NavigationUI.onNavDestinationSelected(item, navController);
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.navigation_welcome || destination.getId() == R.id.navigation_login || destination.getId() == R.id.navigation_create_account) {
                binding.navView.setVisibility(android.view.View.GONE);
            } else {
                binding.navView.setVisibility(android.view.View.VISIBLE);
            }

            // Hide action bar for welcome and create event pages (they have their own toolbars)
            if (destination.getId() == R.id.navigation_welcome || destination.getId() == R.id.navigation_create_event) {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().hide();
                }
                // Remove top padding to eliminate gap
                if (destination.getId() == R.id.navigation_create_event) {
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
                if (destination.getId() != R.id.navigation_welcome && destination.getId() != R.id.navigation_create_event) {
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

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    /**
     * Get user role from SharedPreferences cache
     * Returns null if not found or user not logged in
     */
    private String getUserRole() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        return prefs.getString("user_role", null);
    }

    /**
     * Check if the role is organizer (case-insensitive)
     * Handles both "ORGANIZER" and "ORGANISER" spellings
     */
    private boolean isOrganizer(String role) {
        if (role == null) {
            return false;
        }
        String roleUpper = role.toUpperCase();
        return "ORGANIZER".equals(roleUpper) || "ORGANISER".equals(roleUpper);
    }

}