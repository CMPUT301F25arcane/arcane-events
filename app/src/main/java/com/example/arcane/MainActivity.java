package com.example.arcane;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.arcane.databinding.ActivityMainBinding;
import com.example.arcane.R;
import com.example.arcane.model.Users;
import com.example.arcane.service.UserService;

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
        NavigationUI.setupWithNavController(navView, navController);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.navigation_welcome
                    || destination.getId() == R.id.navigation_login
                    || destination.getId() == R.id.navigation_create_account) {
                binding.navView.setVisibility(android.view.View.GONE);
            } else {
                binding.navView.setVisibility(android.view.View.VISIBLE);
            }
            
            // Update action bar title based on role for home destination
            if (destination.getId() == R.id.navigation_home) {
                updateActionBarTitleForHome();
            }
            
            // Disable back button for top-level destinations (no back button should show)
            if (getSupportActionBar() != null) {
                boolean isTopLevel = destination.getId() == R.id.navigation_home || 
                                     destination.getId() == R.id.navigation_dashboard || 
                                     destination.getId() == R.id.navigation_notifications;
                getSupportActionBar().setDisplayHomeAsUpEnabled(!isTopLevel);
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

    private void updateActionBarTitleForHome() {
        if (getSupportActionBar() == null) return;
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            getSupportActionBar().setTitle("My Events");
            return;
        }

        UserService userService = new UserService();
        userService.getUserById(currentUser.getUid())
                .addOnSuccessListener(snapshot -> {
                    String role = null;
                    if (snapshot.exists()) {
                        Users user = snapshot.toObject(Users.class);
                        if (user != null) {
                            role = user.getRole();
                        }
                    }
                    
                    if (role != null) {
                        String r = role.toUpperCase();
                        if ("ORGANISER".equals(r) || "ORGANIZER".equals(r)) {
                            getSupportActionBar().setTitle("My Events (Organizer)");
                        } else {
                            getSupportActionBar().setTitle("My Events");
                        }
                    } else {
                        getSupportActionBar().setTitle("My Events");
                    }
                })
                .addOnFailureListener(e -> {
                    getSupportActionBar().setTitle("My Events");
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

}