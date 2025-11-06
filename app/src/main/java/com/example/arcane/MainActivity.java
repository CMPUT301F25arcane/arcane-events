package com.example.arcane;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.arcane.databinding.ActivityMainBinding;
import com.example.arcane.R;

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

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

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

}