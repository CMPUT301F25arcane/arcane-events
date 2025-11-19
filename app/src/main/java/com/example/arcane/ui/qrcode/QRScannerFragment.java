package com.example.arcane.ui.qrcode;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
            barcodeView.decodeContinuous(new BarcodeCallback() {
                @Override
                public void barcodeResult(BarcodeResult result) {
                    if (result.getText() != null) {
                        handleScannedCode(result.getText());
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
            barcodeView.postDelayed(() -> barcodeView.resume(), 2000);
        }
    }

    private void navigateToEventDetail(String eventId) {
        NavController navController = Navigation.findNavController(requireView());
        Bundle args = new Bundle();
        args.putString("eventId", eventId);
        navController.navigate(R.id.navigation_event_detail, args);
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigateUp();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (barcodeView != null && checkCameraPermission()) {
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
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (barcodeView != null) {
            barcodeView.pause();
        }
        binding = null;
    }
}

