/**
 * QrCodeGenerator.java
 * 
 * Purpose: Utility class for generating QR codes in bitmap or base64 format.
 * 
 * Design Pattern: Utility class pattern. Provides static helper methods for QR code
 * generation using the ZXing library, abstracting the complexity of QR code creation.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
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
 * Utility class for generating QR codes.
 *
 * <p>Provides methods to generate QR codes as Bitmap images or base64-encoded strings.
 * Uses the ZXing library for QR code generation.</p>
 *
 * @version 1.0
 */
public final class QrCodeGenerator {

    private QrCodeGenerator() {
        // no instances
    }

    /**
     * Generates a QR code as a Bitmap image.
     *
     * @param data the data to encode in the QR code
     * @param size the size of the QR code in pixels (width and height)
     * @return a Bitmap containing the QR code, or null if generation fails
     * @throws WriterException if QR code generation fails
     */
    @Nullable
    public static Bitmap generateBitmap(@NonNull String data, int size) throws WriterException {
        BarcodeEncoder encoder = new BarcodeEncoder();
        return encoder.encodeBitmap(data, BarcodeFormat.QR_CODE, size, size);
    }

    /**
     * Generates a QR code as a base64-encoded string.
     *
     * <p>The QR code is first generated as a Bitmap, then compressed to PNG format
     * and encoded as a base64 string.</p>
     *
     * @param data the data to encode in the QR code
     * @param size the size of the QR code in pixels (width and height)
     * @return a base64-encoded string of the QR code PNG image, or null if generation fails
     * @throws WriterException if QR code generation fails
     */
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

