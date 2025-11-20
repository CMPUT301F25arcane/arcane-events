package com.example.arcane.ui.profile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.arcane.R;

/**
 * Custom view that draws a circular crop overlay with grid lines.
 */
public class CircularCropOverlay extends View {
    
    private Paint overlayPaint;
    private Paint circlePaint;
    private Paint gridPaint;
    private int centerX, centerY;
    private int radius;
    
    public CircularCropOverlay(Context context) {
        super(context);
        init();
    }
    
    public CircularCropOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public CircularCropOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        // Allow touch events to pass through to the image view below
        return false;
    }
    
    private void init() {
        // Dark overlay paint
        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(0xCC000000); // Semi-transparent black
        
        // Circle border paint
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(4f);
        circlePaint.setColor(ContextCompat.getColor(getContext(), R.color.on_brand));
        
        // Grid lines paint
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(0x80FFFFFF); // Semi-transparent white
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        radius = Math.min(w, h) / 2 - 20; // 20dp margin
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Draw dark overlay in four rectangles around the circle
        int width = getWidth();
        int height = getHeight();
        
        // Top rectangle
        canvas.drawRect(0, 0, width, centerY - radius, overlayPaint);
        // Bottom rectangle
        canvas.drawRect(0, centerY + radius, width, height, overlayPaint);
        // Left rectangle
        canvas.drawRect(0, centerY - radius, centerX - radius, centerY + radius, overlayPaint);
        // Right rectangle
        canvas.drawRect(centerX + radius, centerY - radius, width, centerY + radius, overlayPaint);
        
        // Draw circle border
        canvas.drawCircle(centerX, centerY, radius, circlePaint);
        
        // Draw grid lines (rule of thirds)
        float third = radius * 2 / 3;
        float startX = centerX - radius;
        float endX = centerX + radius;
        float startY = centerY - radius;
        float endY = centerY + radius;
        
        // Vertical lines
        canvas.drawLine(centerX - third, startY, centerX - third, endY, gridPaint);
        canvas.drawLine(centerX + third, startY, centerX + third, endY, gridPaint);
        
        // Horizontal lines
        canvas.drawLine(startX, centerY - third, endX, centerY - third, gridPaint);
        canvas.drawLine(startX, centerY + third, endX, centerY + third, gridPaint);
    }
}

