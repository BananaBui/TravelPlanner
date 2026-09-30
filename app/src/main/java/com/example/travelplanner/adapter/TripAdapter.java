package com.example.travelplanner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.travelplanner.R;
import com.example.travelplanner.db.Trip;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.ViewHolder> {

    public interface OnDeleteListener {
        void onDelete(Trip trip);
    }

    private final List<Trip> trips;
    private final OnDeleteListener listener;

    public TripAdapter(List<Trip> trips, OnDeleteListener listener) {
        this.trips = trips;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Trip trip = trips.get(position);
        holder.tvCity.setText(trip.city + ", " + trip.country);
        holder.tvTemp.setText(Math.round(trip.temperature) + "°C  •  " + trip.description);
        String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(new Date(trip.savedAt));
        holder.tvDate.setText(date);

        holder.chipGroup.removeAllViews();
        boolean hasRec = trip.recommendation != null && !trip.recommendation.trim().isEmpty();
        if (hasRec) {
            for (String item : trip.recommendation.split(",")) {
                String trimmed = item.trim();
                if (trimmed.isEmpty()) continue;
                Chip chip = new Chip(holder.chipGroup.getContext());
                chip.setText(trimmed);
                chip.setClickable(false);
                chip.setCheckable(false);
                chip.setFocusable(false);
                holder.chipGroup.addView(chip);
            }
        }
        int recVisibility = hasRec ? View.VISIBLE : View.GONE;
        holder.divider.setVisibility(recVisibility);
        holder.tvPackLabel.setVisibility(recVisibility);
        holder.chipGroup.setVisibility(recVisibility);

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(trip));
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCity, tvTemp, tvDate, tvPackLabel;
        View divider;
        ChipGroup chipGroup;
        Button btnDelete;

        ViewHolder(View view) {
            super(view);
            tvCity = view.findViewById(R.id.tvCity);
            tvTemp = view.findViewById(R.id.tvTemp);
            tvDate = view.findViewById(R.id.tvDate);
            tvPackLabel = view.findViewById(R.id.tvPackLabel);
            divider = view.findViewById(R.id.divider);
            chipGroup = view.findViewById(R.id.chipGroupRec);
            btnDelete = view.findViewById(R.id.btnDelete);
        }
    }
}
