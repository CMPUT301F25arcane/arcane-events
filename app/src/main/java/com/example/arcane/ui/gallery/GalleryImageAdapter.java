/**
 * GalleryImageAdapter.java
 * 
 * Purpose: RecyclerView adapter for displaying event poster images in a gallery grid.
 * 
 * Design Pattern: Adapter pattern for RecyclerView. Implements the standard
 * RecyclerView.Adapter interface to bind event image data to view holders.
 * 
 * Outstanding Issues: None currently identified.
 * 
 * @version 1.0
 */
package com.example.arcane.ui.gallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arcane.R;
import com.example.arcane.model.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying event poster images in a gallery.
 *
 * <p>Manages the display of event poster images in a grid layout,
 * loading images from base64 strings stored in the Event model.</p>
 *
 * @version 1.0
 */
public class GalleryImageAdapter extends RecyclerView.Adapter<GalleryImageAdapter.GalleryImageViewHolder> {

    private final List<Event> events = new ArrayList<>();

    /**
     * Sets the list of events to display.
     *
     * @param items the list of events with poster images
     */
    public void setItems(@NonNull List<Event> items) {
        events.clear();
        // Only add events that have poster images
        for (Event event : items) {
            if (event.getPosterImageUrl() != null && !event.getPosterImageUrl().isEmpty()) {
                events.add(event);
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Creates a new ViewHolder for a gallery image item.
     *
     * @param parent the parent ViewGroup
     * @param viewType the view type
     * @return a new GalleryImageViewHolder instance
     */
    @NonNull
    @Override
    public GalleryImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_image, parent, false);
        return new GalleryImageViewHolder(view);
    }

    /**
     * Binds event data to the ViewHolder.
     *
     * @param holder the ViewHolder to bind data to
     * @param position the position of the item in the list
     */
    @Override
    public void onBindViewHolder(@NonNull GalleryImageViewHolder holder, int position) {
        Event event = events.get(position);
        loadEventImage(holder.imageView, event);
        
        // Hide options button for now (can be implemented later if needed)
        holder.optionsButton.setVisibility(View.GONE);
    }

    /**
     * Returns the number of items in the adapter.
     *
     * @return the number of events with poster images
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for gallery image items.
     */
    static class GalleryImageViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final View optionsButton;

        GalleryImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.gallery_image_thumbnail);
            optionsButton = itemView.findViewById(R.id.image_options_button);
        }
    }

    /**
     * Loads and displays the event image from base64 string or shows placeholder.
     *
     * @param imageView the ImageView to display the image in
     * @param event the event containing the image data
     */
    private void loadEventImage(@NonNull ImageView imageView, @NonNull Event event) {
        String imageData = event.getPosterImageUrl();
        
        if (imageData != null && !imageData.isEmpty()) {
            // Check if it's a base64 string (not a URL)
            if (!imageData.startsWith("http://") && !imageData.startsWith("https://")) {
                try {
                    byte[] imageBytes = Base64.decode(imageData, Base64.NO_WRAP);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        return;
                    }
                } catch (Exception e) {
                    // If decoding fails, fall through to placeholder
                }
            }
            // If it's a URL, you could use Glide/Picasso here
            // For now, fall through to placeholder
        }
        
        // Show placeholder if no image or loading failed
        imageView.setImageResource(android.R.drawable.ic_menu_gallery);
    }
}

