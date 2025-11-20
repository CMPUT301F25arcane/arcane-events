package com.example.arcane.ui.qrcode;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.arcane.R;
import com.example.arcane.databinding.FragmentQrCodeBinding;
import com.example.arcane.model.Event;
import com.example.arcane.repository.EventRepository;
import com.example.arcane.service.EventService;
import com.example.arcane.util.QrCodeGenerator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.zxing.WriterException;

import java.util.HashMap;
import java.util.Map;

/**
 * Full-screen QR code display for organizers.
 *
 * <p>Fetches the event's stored QR code image and shows it with a simple back-only toolbar.
 * Bottom navigation is hidden via MainActivity destination handling.</p>
 */
public class QrCodeFragment extends Fragment {
    private static final String TAG = "QrCodeFragment";
    private static final int QR_CODE_SIZE_PX = 512;
    private static final int QR_STYLE_VERSION = 1;

    private FragmentQrCodeBinding binding;
    private String eventId;
    private EventRepository eventRepository;
    private EventService eventService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
        eventRepository = new EventRepository();
        eventService = new EventService();
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQrCodeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.qrToolbar.setNavigationIcon(R.drawable.ic_back);
        binding.qrToolbar.setNavigationOnClickListener(v -> navigateBack());

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "Event not provided", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        loadQrCode();
    }

    private void loadQrCode() {
        binding.qrProgress.setVisibility(View.VISIBLE);
        eventRepository.getEventById(eventId)
                .addOnSuccessListener(this::handleEventLoaded)
                .addOnFailureListener(e -> {
                    binding.qrProgress.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Unable to load event", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleEventLoaded(DocumentSnapshot snapshot) {
        binding.qrProgress.setVisibility(View.GONE);
        if (snapshot == null || !snapshot.exists()) {
            Toast.makeText(requireContext(), "Event not found", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        Event event = snapshot.toObject(Event.class);
        if (event == null) {
            binding.qrImage.setImageResource(R.drawable.ic_qr_code);
            binding.qrInstructions.setText("QR code not available.");
            return;
        }

        ensureLatestQr(event);
    }

    private void ensureLatestQr(@NonNull Event event) {
        boolean needsRefresh = event.getQrCodeImageBase64() == null
                || event.getQrCodeImageBase64().isEmpty()
                || event.getQrStyleVersion() == null
                || !event.getQrStyleVersion().equals(QR_STYLE_VERSION);

        if (needsRefresh) {
            regenerateQr(event);
        } else {
            displayQr(event.getQrCodeImageBase64());
        }
    }

    private void regenerateQr(@NonNull Event event) {
        if (eventId == null) {
            displayError("QR code not available.");
            return;
        }

        binding.qrProgress.setVisibility(View.VISIBLE);
        try {
            String base64 = QrCodeGenerator.generateBase64("EVENT:" + eventId, QR_CODE_SIZE_PX);
            binding.qrProgress.setVisibility(View.GONE);

            if (base64 == null) {
                displayError("Failed to generate QR code.");
                return;
            }

            displayQr(base64);
            Map<String, Object> updates = new HashMap<>();
            updates.put("qrCodeImageBase64", base64);
            updates.put("qrStyleVersion", QR_STYLE_VERSION);
            eventService.updateEventFields(eventId, updates);
        } catch (WriterException e) {
            binding.qrProgress.setVisibility(View.GONE);
            Log.e(TAG, "Unable to regenerate QR", e);
            displayError("Failed to generate QR code.");
        }
    }

    private void displayQr(@Nullable String base64) {
        Bitmap bitmap = decodeBase64(base64);
        if (bitmap != null) {
            binding.qrImage.setImageBitmap(bitmap);
            binding.qrInstructions.setText("Tap the back button to return to event details.");
        } else {
            displayError("Failed to load QR code.");
        }
    }

    private void displayError(String message) {
        binding.qrImage.setImageResource(R.drawable.ic_qr_code);
        binding.qrInstructions.setText(message);
    }

    @Nullable
    private Bitmap decodeBase64(@Nullable String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void navigateBack() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

