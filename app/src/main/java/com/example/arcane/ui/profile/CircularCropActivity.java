package com.example.arcane.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.arcane.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * CircularCropActivity.java
 * 
 * Purpose: Allows users to crop and position images in a circular frame.
 * 
 * Features:
 * - Circular crop overlay
 * - Pinch to zoom
 * - Pan to move image
 * - Grid overlay for alignment
 * 
 * @version 1.0
 */
public class CircularCropActivity extends AppCompatActivity {
    
    private ImageView imageView;
    private View overlayView;
    private Bitmap originalBitmap;
    private Matrix matrix;
    private float scaleFactor = 1.0f;
    private float lastTouchX, lastTouchY;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;
    private ScaleGestureDetector scaleGestureDetector;
    private int imageViewWidth, imageViewHeight;
    private int cropSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circular_crop);

        imageView = findViewById(R.id.crop_image_view);
        overlayView = findViewById(R.id.crop_overlay);
        Button doneButton = findViewById(R.id.crop_done_button);
        Button cancelButton = findViewById(R.id.crop_cancel_button);

        // Get image URI from intent
        Uri imageUri = getIntent().getParcelableExtra("imageUri");
        if (imageUri == null) {
            Toast.makeText(this, "No image provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Load image with memory optimization
        try {
            // First, get image dimensions without loading full bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
            }
            
            // Calculate sample size to reduce memory usage
            int reqWidth = 1024;
            int reqHeight = 1024;
            int width = options.outWidth;
            int height = options.outHeight;
            int inSampleSize = 1;
            
            if (height > reqHeight || width > reqWidth) {
                int halfHeight = height / 2;
                int halfWidth = width / 2;
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            
            // Now load the bitmap with sample size
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            
            inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                originalBitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
                
                if (originalBitmap == null) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                
                imageView.setImageBitmap(originalBitmap);
            }
        } catch (OutOfMemoryError e) {
            android.util.Log.e("CircularCrop", "Out of memory loading image", e);
            Toast.makeText(this, "Image too large. Please try a smaller image.", Toast.LENGTH_LONG).show();
            finish();
            return;
        } catch (Exception e) {
            android.util.Log.e("CircularCrop", "Error loading image", e);
            Toast.makeText(this, "Error loading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        matrix = new Matrix();
        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        // Setup scale gesture detector
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scale = scaleFactor * detector.getScaleFactor();
                scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
                scaleFactor = scale;
                
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                
                matrix.setScale(scaleFactor, scaleFactor, focusX, focusY);
                imageView.setImageMatrix(matrix);
                return true;
            }
        });

        // Setup touch listener for panning
        imageView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX();
                    lastTouchY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!scaleGestureDetector.isInProgress()) {
                        float dx = event.getX() - lastTouchX;
                        float dy = event.getY() - lastTouchY;
                        
                        matrix.postTranslate(dx, dy);
                        imageView.setImageMatrix(matrix);
                        
                        lastTouchX = event.getX();
                        lastTouchY = event.getY();
                    }
                    break;
            }
            return true;
        });

        // Get image view dimensions after layout
        imageView.post(() -> {
            imageViewWidth = imageView.getWidth();
            imageViewHeight = imageView.getHeight();
            cropSize = Math.min(imageViewWidth, imageViewHeight) - 40; // 20dp margin on each side
            
            // Center and scale image initially
            centerAndScaleImage();
        });

        doneButton.setOnClickListener(v -> {
            try {
                Bitmap croppedBitmap = getCroppedBitmap();
                if (croppedBitmap == null) {
                    Toast.makeText(this, "Failed to crop image", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Resize to reasonable size before encoding (512x512 max for profile picture)
                Bitmap finalBitmap = croppedBitmap;
                int size = Math.min(croppedBitmap.getWidth(), croppedBitmap.getHeight());
                if (size > 512) {
                    float scale = 512f / size;
                    int newWidth = Math.round(croppedBitmap.getWidth() * scale);
                    int newHeight = Math.round(croppedBitmap.getHeight() * scale);
                    finalBitmap = Bitmap.createScaledBitmap(croppedBitmap, newWidth, newHeight, true);
                    // Recycle the original if we created a new one
                    if (finalBitmap != croppedBitmap) {
                        croppedBitmap.recycle();
                    }
                }
                
                // Convert to base64 to pass through intent
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                int quality = 85;
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                byte[] imageBytes = outputStream.toByteArray();
                
                // Check size limit (1.5MB) and compress more if needed
                int maxSize = (int) (1.5 * 1024 * 1024);
                if (imageBytes.length > maxSize) {
                    // Compress more aggressively
                    outputStream = new ByteArrayOutputStream();
                    quality = 70;
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                    imageBytes = outputStream.toByteArray();
                    
                    // If still too large, reduce quality further
                    if (imageBytes.length > maxSize) {
                        outputStream = new ByteArrayOutputStream();
                        quality = 60;
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                        imageBytes = outputStream.toByteArray();
                    }
                }
                
                // Recycle bitmap to free memory
                finalBitmap.recycle();
                
                String base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
                
                Intent resultIntent = new Intent();
                resultIntent.putExtra("croppedImageBase64", base64);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } catch (OutOfMemoryError e) {
                android.util.Log.e("CircularCrop", "Out of memory error", e);
                Toast.makeText(this, "Image too large. Please try a smaller image.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                android.util.Log.e("CircularCrop", "Error processing cropped image", e);
                Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        cancelButton.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void centerAndScaleImage() {
        if (originalBitmap == null || imageViewWidth == 0 || imageViewHeight == 0) {
            return;
        }

        float bitmapWidth = originalBitmap.getWidth();
        float bitmapHeight = originalBitmap.getHeight();
        
        // Calculate scale to fit image in crop circle
        float scaleX = cropSize / bitmapWidth;
        float scaleY = cropSize / bitmapHeight;
        scaleFactor = Math.max(scaleX, scaleY) * 1.2f; // Slightly larger to allow movement
        
        // Center the image
        float dx = (imageViewWidth - bitmapWidth * scaleFactor) / 2;
        float dy = (imageViewHeight - bitmapHeight * scaleFactor) / 2;
        
        matrix.setScale(scaleFactor, scaleFactor);
        matrix.postTranslate(dx, dy);
        imageView.setImageMatrix(matrix);
    }

    private Bitmap getCroppedBitmap() {
        try {
            if (originalBitmap == null || imageViewWidth == 0 || imageViewHeight == 0 || cropSize <= 0) {
                android.util.Log.e("CircularCrop", "Invalid state for cropping");
                return null;
            }

            // Get the current transformation
            float[] values = new float[9];
            matrix.getValues(values);
            float scale = values[Matrix.MSCALE_X];
            float transX = values[Matrix.MTRANS_X];
            float transY = values[Matrix.MTRANS_Y];

            if (scale <= 0) {
                android.util.Log.e("CircularCrop", "Invalid scale: " + scale);
                return null;
            }

            // Calculate crop center and size
            int centerX = imageViewWidth / 2;
            int centerY = imageViewHeight / 2;
            int radius = cropSize / 2;

            // Create a square bitmap for the crop
            Bitmap croppedBitmap;
            try {
                croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError e) {
                android.util.Log.e("CircularCrop", "Out of memory creating crop bitmap", e);
                return null;
            }
            
            Canvas canvas = new Canvas(croppedBitmap);

            // Create circular path for clipping
            Path circlePath = new Path();
            circlePath.addCircle(radius, radius, radius, Path.Direction.CW);

            // Clip to circle
            canvas.clipPath(circlePath);

            // Calculate source rectangle in original bitmap
            // The image is scaled and translated, so we need to reverse the transformation
            float sourceX = (centerX - transX - radius) / scale;
            float sourceY = (centerY - transY - radius) / scale;
            float sourceSize = cropSize / scale;

            // Ensure source coordinates are within bounds
            sourceX = Math.max(0, Math.min(sourceX, originalBitmap.getWidth() - 1));
            sourceY = Math.max(0, Math.min(sourceY, originalBitmap.getHeight() - 1));
            
            // Adjust sourceSize to fit within bitmap bounds
            float maxSourceX = originalBitmap.getWidth() - sourceX;
            float maxSourceY = originalBitmap.getHeight() - sourceY;
            sourceSize = Math.min(sourceSize, Math.min(maxSourceX, maxSourceY));

            if (sourceSize <= 0) {
                croppedBitmap.recycle();
                android.util.Log.e("CircularCrop", "Invalid source size: " + sourceSize);
                return null;
            }

            // Ensure source rectangle is valid
            int sourceLeft = (int) sourceX;
            int sourceTop = (int) sourceY;
            int sourceRight = Math.min((int) (sourceX + sourceSize), originalBitmap.getWidth());
            int sourceBottom = Math.min((int) (sourceY + sourceSize), originalBitmap.getHeight());
            
            if (sourceRight <= sourceLeft || sourceBottom <= sourceTop) {
                croppedBitmap.recycle();
                android.util.Log.e("CircularCrop", "Invalid source rectangle");
                return null;
            }

            android.graphics.Rect sourceRect = new android.graphics.Rect(
                sourceLeft, 
                sourceTop, 
                sourceRight, 
                sourceBottom
            );
            RectF destRect = new RectF(0, 0, cropSize, cropSize);

            // Draw the cropped portion
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setFilterBitmap(true);
            canvas.drawBitmap(originalBitmap, sourceRect, destRect, paint);

            return croppedBitmap;
        } catch (Exception e) {
            android.util.Log.e("CircularCrop", "Error in getCroppedBitmap", e);
            return null;
        }
    }
}

