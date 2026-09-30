package com.example.travelplanner;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.travelplanner.db.Trip;
import com.example.travelplanner.db.TripDatabase;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WeatherActivity extends AppCompatActivity {

    private LinearLayout layoutLoading;
    private TextView tvLoadingStatus;
    private ScrollView scrollView;
    private LinearLayout llContent;
    private MaterialButton btnSave;

    private String cityName, country, weatherDescription, recommendation;
    private double temperature, windSpeed;
    private int humidity;

    private final SimpleDateFormat DATE_DISPLAY =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        layoutLoading = findViewById(R.id.layoutLoading);
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus);
        scrollView = findViewById(R.id.scrollView);
        llContent = findViewById(R.id.llContent);
        btnSave = findViewById(R.id.btnSave);
        findViewById(R.id.btnHome).setOnClickListener(v -> goHome());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Weather");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String city = getIntent().getStringExtra("city");
        long dateMillis = getIntent().getLongExtra("dateMillis", System.currentTimeMillis());

        fetchAll(city, dateMillis);
    }

    private void fetchAll(String city, long dateMillis) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            String error = fetchWeather(client, city, dateMillis);

            if (error != null) {
                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    finish();
                });
                return;
            }

            runOnUiThread(() -> tvLoadingStatus.setText("Considering optimal clothing..."));
            recommendation = fetchRecommendation();

            String dateDisplay = DATE_DISPLAY.format(new Date(dateMillis));
            runOnUiThread(() -> {
                layoutLoading.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);
                renderResults(dateDisplay);
            });

        }).start();
    }

    private String fetchWeather(OkHttpClient client, String city, long dateMillis) {
        try {
            Calendar today = Calendar.getInstance();
            clearTime(today);
            Calendar target = Calendar.getInstance();
            target.setTimeInMillis(dateMillis);
            clearTime(target);

            long diffDays = (target.getTimeInMillis() - today.getTimeInMillis()) / (1000L * 60 * 60 * 24);

            if (diffDays > 5) return "Forecast only available up to 5 days ahead";

            if (diffDays <= 0) {
                String url = Constants.WEATHER_URL + "?q=" + city
                        + "&appid=" + Constants.WEATHER_API_KEY + "&units=metric";
                Response response = client.newCall(new Request.Builder().url(url).build()).execute();
                JSONObject json = new JSONObject(response.body().string());

                if (json.optInt("cod", 200) != 200) return "City not found. Check the spelling.";

                cityName = json.getString("name");
                country = json.getJSONObject("sys").getString("country");
                temperature = json.getJSONObject("main").getDouble("temp");
                humidity = json.getJSONObject("main").getInt("humidity");
                windSpeed = json.getJSONObject("wind").getDouble("speed");
                weatherDescription = json.getJSONArray("weather").getJSONObject(0).getString("description");
                return null;
            }

            String url = Constants.FORECAST_URL + "?q=" + city
                    + "&appid=" + Constants.WEATHER_API_KEY + "&units=metric";
            Response response = client.newCall(new Request.Builder().url(url).build()).execute();
            JSONObject json = new JSONObject(response.body().string());

            if (!"200".equals(json.optString("cod", "200"))) return "City not found. Check the spelling.";

            cityName = json.getJSONObject("city").getString("name");
            country = json.getJSONObject("city").getString("country");

            Calendar targetNoon = (Calendar) target.clone();
            targetNoon.set(Calendar.HOUR_OF_DAY, 12);

            JSONArray list = json.getJSONArray("list");
            JSONObject best = null;
            long bestDiff = Long.MAX_VALUE;
            for (int i = 0; i < list.length(); i++) {
                JSONObject entry = list.getJSONObject(i);
                long diff = Math.abs(entry.getLong("dt") * 1000L - targetNoon.getTimeInMillis());
                if (diff < bestDiff) { bestDiff = diff; best = entry; }
            }

            if (best == null) return "No forecast data available";

            temperature = best.getJSONObject("main").getDouble("temp");
            humidity = best.getJSONObject("main").getInt("humidity");
            windSpeed = best.getJSONObject("wind").getDouble("speed");
            weatherDescription = best.getJSONArray("weather").getJSONObject(0).getString("description");
            return null;

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private String fetchRecommendation() {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            String prompt = "For the city below, respond with ONLY a comma-separated clothing list of 3-5 items. "
                    + "Capitalize every item (e.g. T-shirt, Jeans, Boots). "
                    + "No sentences, no explanations.\n\n"
                    + cityName + ", " + country + ": "
                    + Math.round(temperature) + "°C, " + weatherDescription
                    + ", humidity " + humidity + "%, wind " + windSpeed + " m/s";

            JSONObject body = new JSONObject();
            body.put("model", Constants.GROQ_MODEL);
            JSONArray messages = new JSONArray();
            JSONObject msg = new JSONObject();
            msg.put("role", "user");
            msg.put("content", prompt);
            messages.put(msg);
            body.put("messages", messages);

            RequestBody requestBody = RequestBody.create(
                    body.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(Constants.GROQ_URL)
                    .addHeader("Authorization", "Bearer " + Constants.GROQ_API_KEY)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();
            return new JSONObject(response.body().string())
                    .getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content");

        } catch (Exception e) {
            return null;
        }
    }

    private void renderResults(String dateDisplay) {
        TextView tvDate = new TextView(this);
        tvDate.setText(dateDisplay);
        tvDate.setTextSize(20);
        tvDate.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dateParams.bottomMargin = dp(8);
        tvDate.setLayoutParams(dateParams);
        llContent.addView(tvDate);

        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(8));
        card.setCardElevation(dp(2));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(20);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.addView(content);

        TextView tvCity = new TextView(this);
        tvCity.setText(cityName + (country != null ? ", " + country : ""));
        tvCity.setTextSize(17);
        tvCity.setTypeface(null, Typeface.BOLD);
        content.addView(tvCity);

        TextView tvTemp = new TextView(this);
        tvTemp.setText(Math.round(temperature) + "°C  •  " + capitalize(weatherDescription));
        tvTemp.setTextSize(15);
        setTopMargin(tvTemp, dp(4));
        content.addView(tvTemp);

        TextView tvDetails = new TextView(this);
        tvDetails.setText("Humidity: " + humidity + "%  |  Wind: " + windSpeed + " m/s");
        tvDetails.setTextSize(14);
        setTopMargin(tvDetails, dp(2));
        content.addView(tvDetails);

        if (recommendation != null && !recommendation.trim().isEmpty()) {
            View divider = new View(this);
            divider.setBackgroundColor(0x33888888);
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            divParams.topMargin = dp(10);
            divParams.bottomMargin = dp(8);
            divider.setLayoutParams(divParams);
            content.addView(divider);

            TextView tvLabel = new TextView(this);
            tvLabel.setText("WHAT TO PACK");
            tvLabel.setTextSize(10);
            tvLabel.setTypeface(null, Typeface.BOLD);
            tvLabel.setAlpha(0.55f);
            tvLabel.setLetterSpacing(0.12f);
            content.addView(tvLabel);

            ChipGroup chipGroup = new ChipGroup(this);
            chipGroup.setChipSpacingVertical(dp(2));
            LinearLayout.LayoutParams cgParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cgParams.topMargin = dp(6);
            chipGroup.setLayoutParams(cgParams);

            for (String item : recommendation.split(",")) {
                String trimmed = item.trim();
                if (trimmed.isEmpty()) continue;
                Chip chip = new Chip(this);
                chip.setText(trimmed);
                chip.setClickable(false);
                chip.setCheckable(false);
                chip.setFocusable(false);
                chipGroup.addView(chip);
            }
            content.addView(chipGroup);
        }

        llContent.addView(card);

        btnSave.setVisibility(View.VISIBLE);
        btnSave.setOnClickListener(v -> saveTrip(btnSave));
    }

    private void saveTrip(MaterialButton btn) {
        Trip trip = new Trip();
        trip.city = cityName;
        trip.country = country;
        trip.temperature = temperature;
        trip.description = weatherDescription;
        trip.recommendation = recommendation != null ? recommendation : "";
        trip.savedAt = System.currentTimeMillis();

        new Thread(() -> {
            TripDatabase.getInstance(this).tripDao().insert(trip);
            runOnUiThread(() -> {
                Toast.makeText(this, "Trip saved!", Toast.LENGTH_SHORT).show();
                btn.setEnabled(false);
                btn.setText("Saved");
            });
        }).start();
    }

    private void setTopMargin(View view, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = margin;
        view.setLayoutParams(p);
    }

    private void clearTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
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
