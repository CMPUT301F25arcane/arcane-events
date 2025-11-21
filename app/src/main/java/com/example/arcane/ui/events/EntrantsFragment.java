/**
 * EntrantsFragment.java
 * 
 * Purpose: Displays a list of all entrants registered for an event, showing their
 * status and contact information for organizers.
 * 
 * Design Pattern: Follows MVVM architecture pattern with Repository pattern for data access.
 * Uses ViewBinding for type-safe view access and RecyclerView with adapter pattern for list display.
 * 
 * Outstanding Issues:
 * - View map functionality is not yet implemented
 * - Location information is not available in the Users model
 * 
 * @version 1.0
 */
package com.example.arcane.ui.events;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentEntrantsBinding;
import com.example.arcane.model.Decision;
import com.example.arcane.model.Users;
import com.example.arcane.model.WaitingListEntry;
import com.example.arcane.repository.DecisionRepository;
import com.example.arcane.service.EventService;
import com.example.arcane.service.UserService;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fragment for displaying event entrants list.
 *
 * <p>Shows all users who have registered for an event, including their status
 * (PENDING, INVITED, ACCEPTED, DECLINED, LOST) and contact information.
 * Used by organizers to manage event registrations.</p>
 *
 * @version 1.0
 */
public class EntrantsFragment extends Fragment {

    private FragmentEntrantsBinding binding;
    private EntrantAdapter adapter;
    private EventService eventService;
    private UserService userService;
    private DecisionRepository decisionRepository;
    private String eventId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEntrantsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (eventId == null) {
            Toast.makeText(requireContext(), "Event ID is required", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        eventService = new EventService();
        userService = new UserService();
        decisionRepository = new DecisionRepository();

        adapter = new EntrantAdapter();
        binding.entrantsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.entrantsRecyclerView.setAdapter(adapter);

        // Setup toolbar back button
        binding.entrantsToolbar.setNavigationOnClickListener(v -> navigateBack());

        // Hide view map button for now (can be implemented later)
        binding.viewMapButton.setVisibility(View.GONE);

        // Setup export CSV button
        binding.exportCsvButton.setOnClickListener(v -> exportEnrolledEntrantsToCSV());

        // Load entrants
        loadEntrants();
    }

    private void loadEntrants() {
        eventService.getEventRegistrations(eventId)
                .addOnSuccessListener(result -> {
                    if (!isAdded() || binding == null || adapter == null) return;
                    
                    String status = (String) result.get("status");
                    if (!"success".equals(status)) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to load entrants", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> registrations = (List<Map<String, Object>>) result.get("registrations");
                    if (registrations == null || registrations.isEmpty()) {
                        adapter.setItems(new ArrayList<>());
                        return;
                    }

                    // Load user details for each registration
                    List<EntrantItem> entrantItems = new ArrayList<>();
                    final int[] remaining = {registrations.size()};

                    for (Map<String, Object> registration : registrations) {
                        String entrantId = (String) registration.get("entrantId");
                        String decisionStatus = (String) registration.get("status");

                        userService.getUserById(entrantId)
                                .addOnSuccessListener(userDoc -> {
                                    if (!isAdded() || adapter == null) return;
                                    
                                    Users user = userDoc.toObject(Users.class);
                                    EntrantItem item = new EntrantItem();
                                    if (user != null) {
                                        item.name = user.getName();
                                        item.email = user.getEmail();
                                        item.phone = user.getPhone();
                                    } else {
                                        item.name = "Unknown";
                                        item.email = "";
                                        item.phone = "";
                                    }
                                    item.status = decisionStatus != null ? decisionStatus : "PENDING";
                                    entrantItems.add(item);

                                    remaining[0] -= 1;
                                    if (remaining[0] == 0) {
                                        adapter.setItems(entrantItems);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded() || adapter == null) return;
                                    // On failure, still add entry with limited info
                                    EntrantItem item = new EntrantItem();
                                    item.name = "Unknown";
                                    item.email = "";
                                    item.phone = "";
                                    item.status = decisionStatus != null ? decisionStatus : "PENDING";
                                    entrantItems.add(item);

                                    remaining[0] -= 1;
                                    if (remaining[0] == 0) {
                                        adapter.setItems(entrantItems);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading entrants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void exportEnrolledEntrantsToCSV() {
        if (eventId == null) {
            Toast.makeText(requireContext(), "Event ID is required", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.exportCsvButton.setEnabled(false);
        binding.exportCsvButton.setText("Exporting...");

        decisionRepository.getDecisionsForEvent(eventId)
                .addOnSuccessListener(decisionsSnapshot -> {
                    if (!isAdded() || binding == null) return;

                    if (decisionsSnapshot == null || decisionsSnapshot.isEmpty()) {
                        Toast.makeText(requireContext(), "No entrants found", Toast.LENGTH_SHORT).show();
                        binding.exportCsvButton.setEnabled(true);
                        binding.exportCsvButton.setText("Export CSV");
                        return;
                    }

                    Map<String, String> entrantStatusMap = new HashMap<>();
                    for (QueryDocumentSnapshot doc : decisionsSnapshot) {
                        Decision decision = doc.toObject(Decision.class);
                        if (decision.getEntrantId() != null) {
                            entrantStatusMap.put(decision.getEntrantId(), decision.getStatus() != null ? decision.getStatus() : "PENDING");
                        }
                    }

                    if (entrantStatusMap.isEmpty()) {
                        Toast.makeText(requireContext(), "No entrants found", Toast.LENGTH_SHORT).show();
                        binding.exportCsvButton.setEnabled(true);
                        binding.exportCsvButton.setText("Export CSV");
                        return;
                    }

                    loadUserDetailsAndExportCSV(new ArrayList<>(entrantStatusMap.keySet()), entrantStatusMap);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    Toast.makeText(requireContext(), "Failed to load entrants: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.exportCsvButton.setEnabled(true);
                    binding.exportCsvButton.setText("Export CSV");
                });
    }

    private void loadUserDetailsAndExportCSV(List<String> entrantIds, Map<String, String> entrantStatusMap) {
        List<EntrantItem> allEntrants = new ArrayList<>();
        final int[] remaining = {entrantIds.size()};

        if (entrantIds.isEmpty()) {
            Toast.makeText(requireContext(), "No entrants found", Toast.LENGTH_SHORT).show();
            binding.exportCsvButton.setEnabled(true);
            binding.exportCsvButton.setText("Export CSV");
            return;
        }

        for (String entrantId : entrantIds) {
            final String status = entrantStatusMap.get(entrantId);
            userService.getUserById(entrantId)
                    .addOnSuccessListener(userDoc -> {
                        if (!isAdded() || binding == null) return;

                        Users user = userDoc.toObject(Users.class);
                        EntrantItem item = new EntrantItem();
                        if (user != null) {
                            item.name = user.getName() != null ? user.getName() : "";
                            item.email = user.getEmail() != null ? user.getEmail() : "";
                            item.phone = user.getPhone() != null ? user.getPhone() : "";
                        } else {
                            item.name = "Unknown";
                            item.email = "";
                            item.phone = "";
                        }
                        item.status = status != null ? status : "PENDING";
                        allEntrants.add(item);

                        remaining[0] -= 1;
                        if (remaining[0] == 0) {
                            generateAndShareCSV(allEntrants);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded() || binding == null) return;
                        EntrantItem item = new EntrantItem();
                        item.name = "Unknown";
                        item.email = "";
                        item.phone = "";
                        item.status = status != null ? status : "PENDING";
                        allEntrants.add(item);

                        remaining[0] -= 1;
                        if (remaining[0] == 0) {
                            generateAndShareCSV(allEntrants);
                        }
                    });
        }
    }

    private void generateAndShareCSV(List<EntrantItem> allEntrants) {
        if (!isAdded() || binding == null) return;

        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Name,Email,Phone,Status\n");

            for (EntrantItem item : allEntrants) {
                String name = escapeCSV(item.name != null ? item.name : "");
                String email = escapeCSV(item.email != null ? item.email : "");
                String phone = escapeCSV(item.phone != null ? item.phone : "");
                String status = escapeCSV(item.status != null ? item.status : "");

                csv.append(name).append(",");
                csv.append(email).append(",");
                csv.append(phone).append(",");
                csv.append(status).append("\n");
            }

            File csvFile = createCSVFile(csv.toString());
            if (csvFile != null) {
                shareCSVFile(csvFile);
                Toast.makeText(requireContext(), "CSV exported successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to create CSV file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error generating CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            binding.exportCsvButton.setEnabled(true);
            binding.exportCsvButton.setText("Export CSV");
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private File createCSVFile(String csvContent) {
        try {
            File cacheDir = requireContext().getCacheDir();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "event_entrants_" + dateFormat.format(new Date()) + ".csv";
            File csvFile = new File(cacheDir, fileName);

            FileWriter writer = new FileWriter(csvFile);
            writer.write(csvContent);
            writer.close();

            return csvFile;
        } catch (IOException e) {
            return null;
        }
    }

    private void shareCSVFile(File csvFile) {
        try {
            android.net.Uri fileUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    csvFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Enrolled Entrants");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share CSV"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error sharing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateBack() {
        androidx.navigation.NavController navController = androidx.navigation.Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Simple data class for entrant display
    static class EntrantItem {
        String name;
        String email;
        String phone;
        String status;
    }
}

