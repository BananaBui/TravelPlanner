package com.example.travelplanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.travelplanner.adapter.TripAdapter;
import com.example.travelplanner.db.TripDatabase;
import java.util.List;
import com.example.travelplanner.db.Trip;

public class SavedTripsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_trips);

        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnHome).setOnClickListener(v -> goHome());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Saved Trips");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadTrips();
    }

    private void loadTrips() {
        new Thread(() -> {
            List<Trip> trips = TripDatabase.getInstance(this).tripDao().getAll();
            runOnUiThread(() -> {
                if (trips.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerView.setAdapter(new TripAdapter(trips, trip -> {
                        new Thread(() -> {
                            TripDatabase.getInstance(this).tripDao().delete(trip);
                            runOnUiThread(this::loadTrips);
                        }).start();
                    }));
                }
            });
        }).start();
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
