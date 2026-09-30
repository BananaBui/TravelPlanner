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
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TripWeatherActivity extends AppCompatActivity {

    private LinearLayout layoutLoading;
    private TextView tvLoadingStatus;
    private ScrollView scrollView;
    private LinearLayout llResults;
    private MaterialButton btnSave;

    private final List<DayWeather> allDayWeather = new ArrayList<>();
    private Map<String, String> recommendations = new HashMap<>();

    static class WeatherResult {
        String city;
        String country;
        double temperature;
        String description;
        int humidity;
        double wind;
        String error;
    }

    static class DayWeather {
        String dateDisplay;
        List<WeatherResult> results = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_weather);

        layoutLoading = findViewById(R.id.layoutLoading);
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus);
        scrollView = findViewById(R.id.scrollView);
        llResults = findViewById(R.id.llResults);
        btnSave = findViewById(R.id.btnSave);
        findViewById(R.id.btnHome).setOnClickListener(v -> goHome());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Trip Weather");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        fetchAll(getIntent().getStringExtra("tripPlan"));
    }

    private void fetchAll(String tripPlanJson) {
        new Thread(() -> {
            try {
                JSONArray plan = new JSONArray(tripPlanJson);
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build();

                List<DayWeather> allDays = new ArrayList<>();
                for (int d = 0; d < plan.length(); d++) {
                    JSONObject dayObj = plan.getJSONObject(d);
                    long dateMillis = dayObj.getLong("dateMillis");
                    String dateDisplay = dayObj.getString("dateDisplay");
                    JSONArray cities = dayObj.getJSONArray("cities");

                    DayWeather dw = new DayWeather();
                    dw.dateDisplay = dateDisplay;
                    for (int c = 0; c < cities.length(); c++) {
                        dw.results.add(fetchWeatherForDate(client, cities.getString(c), dateMillis));
                    }
                    allDays.add(dw);
                }
                allDayWeather.clear();
                allDayWeather.addAll(allDays);

                runOnUiThread(() -> tvLoadingStatus.setText("Considering optimal clothing..."));
                recommendations = fetchRecommendations(allDays);

                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                    for (DayWeather day : allDays) {
                        renderDay(day, recommendations);
                    }
                    btnSave.setVisibility(View.VISIBLE);
                    btnSave.setOnClickListener(v -> saveTrip());
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private Map<String, String> fetchRecommendations(List<DayWeather> allDays) {
        Map<String, String> recommendations = new HashMap<>();
        List<String> validCityKeys = new ArrayList<>();

        StringBuilder prompt = new StringBuilder(
                "For each city below, respond with ONLY a comma-separated clothing list of 3-5 items. " +
                "Capitalize every item (e.g. T-shirt, Jeans, Boots). " +
                "No sentences, no explanations. Format: '1. Item, Item, Item'\n\n");

        int idx = 1;
        for (DayWeather day : allDays) {
            for (WeatherResult r : day.results) {
                if (r.error != null) continue;
                validCityKeys.add(r.city.toLowerCase());
                prompt.append(idx).append(". ").append(r.city);
                if (r.country != null) prompt.append(", ").append(r.country);
                prompt.append(" (").append(day.dateDisplay).append("): ")
                        .append(Math.round(r.temperature)).append("°C, ")
                        .append(r.description).append(", humidity ").append(r.humidity)
                        .append("%, wind ").append(r.wind).append(" m/s\n");
                idx++;
            }
        }

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
            msg.put("content", prompt.toString());
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
            String content = new JSONObject(response.body().string())
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            for (String line : content.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int dotPos = line.indexOf('.');
                if (dotPos > 0) {
                    try {
                        int lineIdx = Integer.parseInt(line.substring(0, dotPos).trim()) - 1;
                        if (lineIdx >= 0 && lineIdx < validCityKeys.size()) {
                            recommendations.put(validCityKeys.get(lineIdx), line.substring(dotPos + 1).trim());
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception ignored) {
        }

        return recommendations;
    }

    private WeatherResult fetchWeatherForDate(OkHttpClient client, String cityName, long dateMillis) {
        WeatherResult result = new WeatherResult();
        result.city = cityName;

        try {
            Calendar today = Calendar.getInstance();
            clearTime(today);
            Calendar target = Calendar.getInstance();
            target.setTimeInMillis(dateMillis);
            clearTime(target);

            long diffDays = (target.getTimeInMillis() - today.getTimeInMillis()) / (1000L * 60 * 60 * 24);

            if (diffDays > 5) {
                result.error = "Forecast only available up to 5 days ahead";
                return result;
            }

            if (diffDays <= 0) {
                String url = Constants.WEATHER_URL
                        + "?q=" + cityName
                        + "&appid=" + Constants.WEATHER_API_KEY
                        + "&units=metric";
                Response response = client.newCall(new Request.Builder().url(url).build()).execute();
                JSONObject json = new JSONObject(response.body().string());

                if (json.optInt("cod", 200) != 200) { result.error = "City not found"; return result; }

                result.country = json.getJSONObject("sys").getString("country");
                result.temperature = json.getJSONObject("main").getDouble("temp");
                result.humidity = json.getJSONObject("main").getInt("humidity");
                result.wind = json.getJSONObject("wind").getDouble("speed");
                result.description = json.getJSONArray("weather").getJSONObject(0).getString("description");
                return result;
            }

            String url = Constants.FORECAST_URL
                    + "?q=" + cityName
                    + "&appid=" + Constants.WEATHER_API_KEY
                    + "&units=metric";
            Response response = client.newCall(new Request.Builder().url(url).build()).execute();
            JSONObject json = new JSONObject(response.body().string());

            if (!"200".equals(json.optString("cod", "200"))) { result.error = "City not found"; return result; }

            result.country = json.getJSONObject("city").getString("country");

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

            if (best == null) { result.error = "No forecast data available"; return result; }

            result.temperature = best.getJSONObject("main").getDouble("temp");
            result.humidity = best.getJSONObject("main").getInt("humidity");
            result.wind = best.getJSONObject("wind").getDouble("speed");
            result.description = best.getJSONArray("weather").getJSONObject(0).getString("description");
            return result;

        } catch (Exception e) {
            result.error = e.getMessage();
            return result;
        }
    }

    private void renderDay(DayWeather day, Map<String, String> recommendations) {
        boolean isFirst = llResults.getChildCount() == 0;

        TextView tvDate = new TextView(this);
        tvDate.setText(day.dateDisplay);
        tvDate.setTextSize(20);
        tvDate.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        headerParams.topMargin = isFirst ? dp(8) : dp(30);
        headerParams.bottomMargin = dp(8);
        tvDate.setLayoutParams(headerParams);
        llResults.addView(tvDate);

        for (WeatherResult r : day.results) {
            MaterialCardView card = new MaterialCardView(this);
            card.setRadius(dp(8));
            card.setCardElevation(dp(2));
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(8);
            card.setLayoutParams(cardParams);

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(12), dp(12), dp(12), dp(12));
            card.addView(content);

            if (r.error != null) {
                TextView tvErr = new TextView(this);
                tvErr.setText(r.city + "  —  " + r.error);
                tvErr.setTextSize(15);
                content.addView(tvErr);
            } else {
                TextView tvCityName = new TextView(this);
                tvCityName.setText(r.city + (r.country != null ? ", " + r.country : ""));
                tvCityName.setTextSize(17);
                tvCityName.setTypeface(null, Typeface.BOLD);
                content.addView(tvCityName);

                TextView tvTemp = new TextView(this);
                tvTemp.setText(Math.round(r.temperature) + "°C  •  " + capitalize(r.description));
                tvTemp.setTextSize(15);
                setTopMargin(tvTemp, dp(4));
                content.addView(tvTemp);

                TextView tvDetails = new TextView(this);
                tvDetails.setText("Humidity: " + r.humidity + "%  |  Wind: " + r.wind + " m/s");
                tvDetails.setTextSize(14);
                setTopMargin(tvDetails, dp(2));
                content.addView(tvDetails);

                String rec = recommendations.get(r.city.toLowerCase());
                if (rec != null && !rec.isEmpty()) {
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

                    for (String item : rec.split(",")) {
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
            }

            llResults.addView(card);
        }
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

    private void saveTrip() {
        new Thread(() -> {
            TripDatabase db = TripDatabase.getInstance(this);
            for (DayWeather day : allDayWeather) {
                for (WeatherResult r : day.results) {
                    if (r.error != null) continue;
                    Trip trip = new Trip();
                    trip.city = r.city;
                    trip.country = r.country;
                    trip.temperature = r.temperature;
                    trip.description = r.description;
                    trip.recommendation = recommendations.getOrDefault(r.city.toLowerCase(), "");
                    trip.savedAt = System.currentTimeMillis();
                    db.tripDao().insert(trip);
                }
            }
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
