package com.example.arcane.util;

import android.graphics.Bitmap;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.ByteArrayOutputStream;

/**
 * Utility methods for generating QR codes in bitmap or base64 form.
 */
public final class QrCodeGenerator {

    private QrCodeGenerator() {
        // no instances
    }

    @Nullable
    public static Bitmap generateBitmap(@NonNull String data, int size) throws WriterException {
        BarcodeEncoder encoder = new BarcodeEncoder();
        return encoder.encodeBitmap(data, BarcodeFormat.QR_CODE, size, size);
    }

    @Nullable
    public static String generateBase64(@NonNull String data, int size) throws WriterException {
        Bitmap bitmap = generateBitmap(data, size);
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }
}

