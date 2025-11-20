package com.example.arcane.ui.qrcode;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentQrScannerBinding;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

/**
 * QRScannerFragment.java
 * 
 * Purpose: Allows users to scan QR codes using the device camera and navigate to event details.
 * 
 * Design Pattern: Follows MVVM architecture pattern with ViewBinding for type-safe view access.
 * Uses ZXing library for QR code scanning.
 * 
 * @version 1.0
 */
public class QRScannerFragment extends Fragment {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private FragmentQrScannerBinding binding;
    private DecoratedBarcodeView barcodeView;
    private boolean isScanning = false;
    private String lastScannedCode = null;
    private long lastScanTime = 0;
    private static final long SCAN_COOLDOWN_MS = 3000; // 3 seconds cooldown between scans

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentQrScannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup back button
        binding.backButton.setOnClickListener(v -> navigateBack());

        // Setup barcode scanner
        barcodeView = binding.cameraPreview;
        barcodeView.setStatusText("Point camera at QR code");

        // Check camera permission
        if (checkCameraPermission()) {
            startScanning();
        } else {
            requestCameraPermission();
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                navigateBack();
            }
        }
    }

    private void startScanning() {
        try {
            isScanning = true;
            lastScannedCode = null;
            lastScanTime = 0;
            
            barcodeView.decodeContinuous(new BarcodeCallback() {
                @Override
                public void barcodeResult(BarcodeResult result) {
                    if (!isScanning) {
                        return; // Don't process if scanning is stopped
                    }
                    
                    if (result.getText() != null) {
                        String scannedText = result.getText();
                        long currentTime = System.currentTimeMillis();
                        
                        // Check if this is the same code we just scanned
                        if (scannedText.equals(lastScannedCode) && 
                            (currentTime - lastScanTime) < SCAN_COOLDOWN_MS) {
                            // Same code scanned recently, ignore it
                            return;
                        }
                        
                        // New code or enough time has passed, process it
                        lastScannedCode = scannedText;
                        lastScanTime = currentTime;
                        handleScannedCode(scannedText);
                    }
                }

                @Override
                public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {
                    // Optional: Handle possible result points for UI feedback
                }
            });
            barcodeView.resume();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Camera error: " + e.getMessage() + ". Please check emulator camera settings.", Toast.LENGTH_LONG).show();
            android.util.Log.e("QRScanner", "Camera error", e);
        }
    }

    private void handleScannedCode(String scannedText) {
        // Stop scanning to prevent multiple scans
        isScanning = false;
        barcodeView.pause();

        // Parse the scanned code - QR codes are generated as "EVENT:" + eventId
        String eventId = null;
        if (scannedText != null && scannedText.startsWith("EVENT:")) {
            eventId = scannedText.substring(6); // Remove "EVENT:" prefix
        }

        if (eventId != null && !eventId.isEmpty()) {
            // Navigate to event detail page
            navigateToEventDetail(eventId);
        } else {
            Toast.makeText(requireContext(), "Invalid QR code format", Toast.LENGTH_SHORT).show();
            // Resume scanning after a delay
            isScanning = true;
            barcodeView.postDelayed(() -> {
                if (isScanning) {
                    barcodeView.resume();
                }
            }, 2000);
        }
    }

    private void navigateToEventDetail(String eventId) {
        try {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            Bundle args = new Bundle();
            args.putString("eventId", eventId);
            
            // Store eventId for navigation listener
            final String eventIdToNavigate = eventId;
            
            // Add a one-time listener to navigate to event detail immediately after home navigation
            androidx.navigation.NavController.OnDestinationChangedListener oneTimeListener = 
                new androidx.navigation.NavController.OnDestinationChangedListener() {
                    @Override
                    public void onDestinationChanged(@NonNull NavController controller,
                                                    @NonNull androidx.navigation.NavDestination destination,
                                                    @Nullable Bundle arguments) {
                        // When we reach home, immediately navigate to event detail
                        if (destination.getId() == R.id.navigation_home) {
                            // Remove this listener to prevent it from firing again
                            controller.removeOnDestinationChangedListener(this);
                            
                            // Navigate to event detail immediately
                            Bundle detailArgs = new Bundle();
                            detailArgs.putString("eventId", eventIdToNavigate);
                            controller.navigate(R.id.navigation_event_detail, detailArgs);
                        }
                    }
                };
            
            navController.addOnDestinationChangedListener(oneTimeListener);
            
            // Navigate to Events tab first to update bottom navigation to Events
            // Use popUpTo to remove scanner from back stack
            androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.navigation_dashboard, true)
                    .build();
            
            // Navigate to home first (this sets bottom nav to Events tab)
            // The listener will immediately navigate to event detail
            navController.navigate(R.id.navigation_home, null, navOptions);
        } catch (Exception e) {
            android.util.Log.e("QRScanner", "Error in navigateToEventDetail", e);
            Toast.makeText(requireContext(), "Error navigating to event", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (barcodeView != null && checkCameraPermission() && isScanning) {
            // Reset scanning state when resuming
            isScanning = true;
            lastScannedCode = null;
            lastScanTime = 0;
            try {
                barcodeView.resume();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Unable to start camera. Please check camera settings in emulator.", Toast.LENGTH_LONG).show();
                android.util.Log.e("QRScanner", "Camera resume error", e);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        isScanning = false;
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isScanning = false;
        if (barcodeView != null) {
            barcodeView.pause();
        }
        binding = null;
    }
}

