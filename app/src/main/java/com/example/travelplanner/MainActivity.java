package com.example.travelplanner;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private long selectedDateMillis;
    private final SimpleDateFormat DATE_DISPLAY =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        selectedDateMillis = cal.getTimeInMillis();

        EditText etCity = findViewById(R.id.etCity);
        MaterialButton btnPickDate = findViewById(R.id.btnPickDate);
        Button btnSearch = findViewById(R.id.btnSearch);
        Button btnPlanTrip = findViewById(R.id.btnPlanTrip);
        Button btnSavedTrips = findViewById(R.id.btnSavedTrips);

        btnPickDate.setText(DATE_DISPLAY.format(new Date(selectedDateMillis)));

        btnPickDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selectedDateMillis);
            new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, 12, 0, 0);
                selected.set(Calendar.MILLISECOND, 0);
                selectedDateMillis = selected.getTimeInMillis();
                btnPickDate.setText(DATE_DISPLAY.format(selected.getTime()));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnPlanTrip.setOnClickListener(v ->
                startActivity(new Intent(this, TripPlanActivity.class))
        );

        btnSearch.setOnClickListener(v -> {
            String city = etCity.getText().toString().trim();
            if (city.isEmpty()) {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, WeatherActivity.class);
            intent.putExtra("city", city);
            intent.putExtra("dateMillis", selectedDateMillis);
            startActivity(intent);
        });

        btnSavedTrips.setOnClickListener(v ->
                startActivity(new Intent(this, SavedTripsActivity.class))
        );
    }
}
