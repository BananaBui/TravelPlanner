package com.example.travelplanner;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.travelplanner.db.Trip;
import com.example.travelplanner.db.TripDatabase;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;

public class RecommendationActivity extends AppCompatActivity {

    private TextView tvWeatherSummary, tvRecommendation;
    private ProgressBar progressBar;
    private Button btnSave;

    private String city, country, description, recommendation;
    private double temperature, wind;
    private int humidity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation);

        tvWeatherSummary = findViewById(R.id.tvWeatherSummary);
        tvRecommendation = findViewById(R.id.tvRecommendation);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSave);

        city = getIntent().getStringExtra("city");
        country = getIntent().getStringExtra("country");
        temperature = getIntent().getDoubleExtra("temperature", 0);
        description = getIntent().getStringExtra("description");
        humidity = getIntent().getIntExtra("humidity", 0);
        wind = getIntent().getDoubleExtra("wind", 0);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Clothing Recommendation");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvWeatherSummary.setText(
                city + ", " + country + "\n"
                + Math.round(temperature) + "°C  •  " + description + "\n"
                + "Humidity: " + humidity + "%  •  Wind: " + wind + " m/s"
        );

        fetchRecommendation();

        btnSave.setOnClickListener(v -> saveTrip());
    }

    private void fetchRecommendation() {
        progressBar.setVisibility(View.VISIBLE);
        tvRecommendation.setVisibility(View.GONE);
        btnSave.setVisibility(View.GONE);

        String prompt = "I am traveling to " + city + ", " + country + ". "
                + "The weather is: " + description + ", "
                + Math.round(temperature) + "°C, humidity " + humidity + "%, wind " + wind + " m/s. "
                + "What clothing and accessories should I pack? "
                + "Give a short, practical answer in 2-3 sentences. "
                + "Mention specific items like jacket, t-shirt, shorts, umbrella, sunscreen, boots, etc.";

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build();

                JSONObject body = new JSONObject();
                body.put("model", Constants.GROQ_MODEL);
                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("role", "user");
                msg.put("content", prompt);
                messages.put(msg);
                body.put("messages", messages);

                RequestBody requestBody = RequestBody.create(
                        body.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(Constants.GROQ_URL)
                        .addHeader("Authorization", "Bearer " + Constants.GROQ_API_KEY)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                String responseText = response.body().string();
                JSONObject responseJson = new JSONObject(responseText);
                recommendation = responseJson
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvRecommendation.setVisibility(View.VISIBLE);
                    tvRecommendation.setText(recommendation);
                    btnSave.setVisibility(View.VISIBLE);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void saveTrip() {
        Trip trip = new Trip();
        trip.city = city;
        trip.country = country;
        trip.temperature = temperature;
        trip.description = description;
        trip.recommendation = recommendation;
        trip.savedAt = System.currentTimeMillis();

        new Thread(() -> {
            TripDatabase.getInstance(this).tripDao().insert(trip);
            runOnUiThread(() -> {
                Toast.makeText(this, "Trip saved!", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(false);
                btnSave.setText("Saved");
            });
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
