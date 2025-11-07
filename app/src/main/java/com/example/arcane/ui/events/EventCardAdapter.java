package com.example.arcane.ui.events;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.arcane.R;
import com.example.arcane.model.Event;

import java.util.ArrayList;
import java.util.List;

public class EventCardAdapter extends RecyclerView.Adapter<EventCardAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(@NonNull Event event);
    }

    private final List<Event> events = new ArrayList<>();
    private final OnEventClickListener listener;

    public EventCardAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<Event> items) {
        events.clear();
        events.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.titleView.setText(event.getEventName());
        holder.locationView.setText(event.getLocation());

        if (event.getEventDate() != null) {
            holder.dateView.setText(android.text.format.DateFormat.format("yyyy-MM-dd", event.getEventDate().toDate()));
        } else {
            holder.dateView.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEventClick(event);
        });

        // Placeholder image; integrate Glide/Picasso later if needed
        holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView titleView;
        TextView dateView;
        TextView locationView;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.event_image);
            titleView = itemView.findViewById(R.id.event_name);
            dateView = itemView.findViewById(R.id.event_date);
            locationView = itemView.findViewById(R.id.event_location);
        }
    }
}


