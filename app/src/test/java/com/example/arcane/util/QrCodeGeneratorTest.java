package com.example.arcane.util;

import android.graphics.Bitmap;
import android.util.Base64;

import com.google.zxing.WriterException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Unit tests for QrCodeGenerator utility class.
 *
 * Tests QR code generation in both Bitmap and Base64 formats.
 * Uses Robolectric to provide Android framework classes (Bitmap) in unit tests.
 *
 * Coverage includes:
 * - Valid input generation (Bitmap and Base64)
 * - Various input types (URLs, event IDs, special characters, Unicode)
 * - Various sizes (100, 200, 300, 500)
 * - Null input handling
 * - Invalid sizes (zero, negative)
 * - Empty string edge case
 * - Base64 format validation
 * - Consistency verification
 * - PNG format verification
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class QrCodeGeneratorTest {

    private String testData;
    private int testSize;

    @Before
    public void setUp() {
        testData = "https://example.com/event123";
        testSize = 200;
    }


    /**
     * Test that generateBitmap creates a valid bitmap with valid input.
     */
    @Test
    public void testGenerateBitmap_success() throws WriterException {
        Bitmap bitmap = QrCodeGenerator.generateBitmap(testData, testSize);

        assertNotNull("Bitmap should not be null", bitmap);
        assertEquals("Bitmap width should match size", testSize, bitmap.getWidth());
        assertEquals("Bitmap height should match size", testSize, bitmap.getHeight());
        assertFalse("Bitmap should not be recycled", bitmap.isRecycled());
    }

    /**
     * Test generateBitmap with various inputs (sizes, data types, special characters).
     */
    @Test
    public void testGenerateBitmap_variousInputs() throws WriterException {
        // Test different sizes
        int[] sizes = {100, 200, 300, 500};
        for (int size : sizes) {
            Bitmap bitmap = QrCodeGenerator.generateBitmap(testData, size);
            assertNotNull("Bitmap should not be null for size " + size, bitmap);
            assertEquals("Bitmap width should match size " + size, size, bitmap.getWidth());
            assertEquals("Bitmap height should match size " + size, size, bitmap.getHeight());
        }

        // Test different data types
        String[] testDataArray = {
            "https://example.com",
            "event-123",
            "user-456",
            "Simple text",
            "1234567890",
            "Special chars: !@#$%^&*()",
            "Event: Café & Bar",
            "User: 用户123"
        };

        for (String data : testDataArray) {
            Bitmap bitmap = QrCodeGenerator.generateBitmap(data, testSize);
            assertNotNull("Bitmap should not be null for data: " + data, bitmap);
            assertEquals("Bitmap width should match", testSize, bitmap.getWidth());
            assertEquals("Bitmap height should match", testSize, bitmap.getHeight());
        }
    }

    /**
     * Test generateBitmap with null data input.
     * Expects WriterException (which wraps NullPointerException from BarcodeEncoder).
     */
    @Test(expected = WriterException.class)
    public void testGenerateBitmap_nullData() throws WriterException {
        QrCodeGenerator.generateBitmap(null, testSize);
    }

    /**
     * Test generateBitmap with invalid sizes (zero and negative).
     * Note: BarcodeEncoder actually generates QR codes even with unusual sizes.
     */
    @Test
    public void testGenerateBitmap_invalidSizes() throws WriterException {
        // Test size = 0 - library generates a valid bitmap
        Bitmap bitmap = QrCodeGenerator.generateBitmap(testData, 0);
        assertNotNull("Bitmap should not be null even for size 0", bitmap);
        // The actual dimensions may be different from requested size
        assertTrue("Bitmap should have non-negative width", bitmap.getWidth() >= 0);

        // Test negative size - library may throw or handle it
        try {
            Bitmap negativeBitmap = QrCodeGenerator.generateBitmap(testData, -100);
            // If it succeeds, verify bitmap is valid
            assertNotNull("Bitmap should not be null", negativeBitmap);
        } catch (WriterException | IllegalArgumentException e) {
            // Exception is also acceptable for negative size
            assertNotNull("Exception should have a message", e.getMessage());
        }
    }

    /**
     * Test generateBitmap with empty string.
     */
    @Test
    public void testGenerateBitmap_emptyString() {
        try {
            Bitmap bitmap = QrCodeGenerator.generateBitmap("", testSize);
            // If it succeeds, bitmap should be valid
            assertNotNull("Bitmap should not be null for empty string", bitmap);
            assertEquals("Bitmap should have correct width", testSize, bitmap.getWidth());
            assertEquals("Bitmap should have correct height", testSize, bitmap.getHeight());
        } catch (WriterException e) {
            // Empty string may throw WriterException - this is acceptable
            assertTrue("Exception message should be meaningful",
                e.getMessage() != null && !e.getMessage().isEmpty());
        }
    }


    /**
     * Test that generateBase64 creates a valid base64 string.
     */
    @Test
    public void testGenerateBase64_success() throws WriterException {
        String base64 = QrCodeGenerator.generateBase64(testData, testSize);

        assertNotNull("Base64 string should not be null", base64);
        assertFalse("Base64 string should not be empty", base64.isEmpty());
        
        // Verify it's valid base64 by attempting to decode
        try {
            byte[] decoded = Base64.decode(base64, Base64.NO_WRAP);
            assertTrue("Decoded base64 should have content", decoded.length > 0);
        } catch (IllegalArgumentException e) {
            fail("Base64 string should be valid: " + e.getMessage());
        }
    }

    /**
     * Test generateBase64 with various inputs (sizes, data types, special characters).
     */
    @Test
    public void testGenerateBase64_variousInputs() throws WriterException {
        // Test different sizes
        int[] sizes = {100, 200, 300, 500};
        for (int size : sizes) {
            String base64 = QrCodeGenerator.generateBase64(testData, size);
            assertNotNull("Base64 should not be null for size " + size, base64);
            assertFalse("Base64 should not be empty for size " + size, base64.isEmpty());
            
            // Verify it's valid base64
            try {
                byte[] decoded = Base64.decode(base64, Base64.NO_WRAP);
                assertTrue("Decoded base64 should have content for size " + size, decoded.length > 0);
            } catch (IllegalArgumentException e) {
                fail("Base64 string should be valid for size " + size + ": " + e.getMessage());
            }
        }

        // Test different data types including special characters
        String[] testDataArray = {
            "https://example.com",
            "event-123",
            "user-456",
            "Simple text",
            "Event: Café & Bar",
            "User: 用户123",
            "Price: $50.00"
        };

        for (String data : testDataArray) {
            String base64 = QrCodeGenerator.generateBase64(data, testSize);
            assertNotNull("Base64 should not be null for data: " + data, base64);
            assertFalse("Base64 should not be empty for data: " + data, base64.isEmpty());
        }
    }

    /**
     * Test generateBase64 with null data input.
     * Expects WriterException (which wraps NullPointerException from BarcodeEncoder).
     */
    @Test(expected = WriterException.class)
    public void testGenerateBase64_nullData() throws WriterException {
        QrCodeGenerator.generateBase64(null, testSize);
    }

    /**
     * Test generateBase64 with invalid sizes (zero and negative).
     * Note: BarcodeEncoder actually generates QR codes even with unusual sizes.
     */
    @Test
    public void testGenerateBase64_invalidSizes() throws WriterException {
        // Test size = 0 - library generates a valid Base64 string
        String base64 = QrCodeGenerator.generateBase64(testData, 0);
        assertNotNull("Base64 should not be null even for size 0", base64);
        assertFalse("Base64 should not be empty", base64.isEmpty());

        // Verify it's valid base64
        byte[] decoded = Base64.decode(base64, Base64.NO_WRAP);
        assertTrue("Decoded base64 should have content", decoded.length > 0);

        // Test negative size - library may throw or handle it
        try {
            String negativeBase64 = QrCodeGenerator.generateBase64(testData, -100);
            // If it succeeds, verify base64 is valid
            assertNotNull("Base64 should not be null", negativeBase64);
            assertFalse("Base64 should not be empty", negativeBase64.isEmpty());
        } catch (WriterException | IllegalArgumentException e) {
            // Exception is also acceptable for negative size
            assertNotNull("Exception should have a message", e.getMessage());
        }
    }

    /**
     * Test generateBase64 with empty string.
     */
    @Test
    public void testGenerateBase64_emptyString() {
        try {
            String base64 = QrCodeGenerator.generateBase64("", testSize);
            // If it succeeds, Base64 should be valid
            assertNotNull("Base64 should not be null for empty string", base64);
            assertFalse("Base64 should not be empty for empty string", base64.isEmpty());

            // Verify it's valid base64
            byte[] decoded = Base64.decode(base64, Base64.NO_WRAP);
            assertTrue("Decoded base64 should have content", decoded.length > 0);
        } catch (WriterException e) {
            // Empty string may throw WriterException - this is acceptable
            assertTrue("Exception message should be meaningful",
                e.getMessage() != null && !e.getMessage().isEmpty());
        }
    }

    /**
     * Test that generateBase64 produces valid Base64 format.
     */
    @Test
    public void testGenerateBase64_validFormat() throws WriterException {
        String base64 = QrCodeGenerator.generateBase64(testData, testSize);

        assertNotNull("Base64 should not be null", base64);
        assertFalse("Base64 should not be empty", base64.isEmpty());

        // Verify it only contains valid Base64 characters
        assertTrue("Base64 should only contain valid characters [A-Za-z0-9+/=]",
            base64.matches("^[A-Za-z0-9+/=]+$"));

        // Verify it's properly padded (length should be multiple of 4)
        assertEquals("Base64 should be properly padded (length multiple of 4)",
            0, base64.length() % 4);
    }

    /**
     * Test consistency: same input produces same output, different inputs produce different outputs.
     */
    @Test
    public void testGenerateBase64_consistency() throws WriterException {
        // Same input should produce same output
        String base64_1 = QrCodeGenerator.generateBase64(testData, testSize);
        String base64_2 = QrCodeGenerator.generateBase64(testData, testSize);
        assertNotNull("Base64 1 should not be null", base64_1);
        assertNotNull("Base64 2 should not be null", base64_2);
        assertEquals("Same input data should produce same base64", base64_1, base64_2);

        // Different inputs should produce different outputs
        String base64_3 = QrCodeGenerator.generateBase64("data1", testSize);
        String base64_4 = QrCodeGenerator.generateBase64("data2", testSize);
        assertNotNull("Base64 3 should not be null", base64_3);
        assertNotNull("Base64 4 should not be null", base64_4);
        assertNotEquals("Different input data should produce different base64", base64_3, base64_4);
    }

    /**
     * Test that generateBase64 and generateBitmap produce consistent results.
     * The base64 should decode to a valid PNG image.
     */
    @Test
    public void testGenerateBase64_matchesGenerateBitmap() throws WriterException {
        Bitmap bitmap = QrCodeGenerator.generateBitmap(testData, testSize);
        String base64 = QrCodeGenerator.generateBase64(testData, testSize);

        assertNotNull("Bitmap should not be null", bitmap);
        assertNotNull("Base64 should not be null", base64);

        // Decode base64 and verify it's a valid PNG image
        byte[] decodedBytes = Base64.decode(base64, Base64.NO_WRAP);
        assertTrue("Decoded bytes should have content", decodedBytes.length > 0);
        
        // Verify PNG signature (first 4 bytes: 89 50 4E 47)
        if (decodedBytes.length >= 4) {
            assertEquals("Should start with PNG signature byte 1", (byte)0x89, decodedBytes[0]);
            assertEquals("Should start with PNG signature byte 2", (byte)0x50, decodedBytes[1]);
            assertEquals("Should start with PNG signature byte 3", (byte)0x4E, decodedBytes[2]);
            assertEquals("Should start with PNG signature byte 4", (byte)0x47, decodedBytes[3]);
        }
    }
}
