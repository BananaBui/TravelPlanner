package com.example.travelplanner;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TripPlanActivity extends AppCompatActivity {

    private LinearLayout llDaysContainer;
    private final List<DayData> days = new ArrayList<>();
    private int dayCount = 0;

    private final SimpleDateFormat DATE_DISPLAY =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    static class DayData {
        long dateMillis;
        MaterialButton dateButton;
        LinearLayout citiesContainer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_plan);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Plan Your Trip");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        llDaysContainer = findViewById(R.id.llDaysContainer);
        MaterialButton btnAddDay = findViewById(R.id.btnAddDay);
        MaterialButton btnGetWeather = findViewById(R.id.btnGetWeather);

        btnAddDay.setOnClickListener(v -> addDay());
        btnGetWeather.setOnClickListener(v -> getWeather());
        findViewById(R.id.btnHome).setOnClickListener(v -> goHome());

        addDay();
    }

    private void addDay() {
        dayCount++;
        DayData dayData = new DayData();

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, dayCount - 1);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        dayData.dateMillis = cal.getTimeInMillis();

        MaterialCardView cardView = new MaterialCardView(this);
        cardView.setRadius(dp(12));
        cardView.setCardElevation(dp(2));
        LinearLayout.LayoutParams cvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cvParams.topMargin = dp(20);
        cvParams.bottomMargin = dp(4);
        cardView.setLayoutParams(cvParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(12), dp(16), dp(8));
        cardView.addView(content);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hrParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hrParams.bottomMargin = dp(8);
        headerRow.setLayoutParams(hrParams);

        TextView tvDayLabel = new TextView(this);
        tvDayLabel.setText(getString(R.string.day_label, dayCount));
        tvDayLabel.setTextSize(16);
        tvDayLabel.setTypeface(null, Typeface.BOLD);
        tvDayLabel.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        MaterialButton btnDate = new MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnDate.setText(DATE_DISPLAY.format(new Date(dayData.dateMillis)));
        btnDate.setTextSize(12);
        dayData.dateButton = btnDate;
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dateParams.setMarginEnd(dp(4));
        btnDate.setLayoutParams(dateParams);
        btnDate.setOnClickListener(v -> pickDate(dayData));

        Button btnRemoveDay = new Button(this, null, android.R.attr.borderlessButtonStyle);
        btnRemoveDay.setText(R.string.btn_remove);
        btnRemoveDay.setOnClickListener(v -> {
            llDaysContainer.removeView(cardView);
            days.remove(dayData);
        });

        headerRow.addView(tvDayLabel);
        headerRow.addView(btnDate);
        headerRow.addView(btnRemoveDay);

        LinearLayout citiesContainer = new LinearLayout(this);
        citiesContainer.setOrientation(LinearLayout.VERTICAL);
        dayData.citiesContainer = citiesContainer;

        Button btnAddCity = new Button(this, null, android.R.attr.borderlessButtonStyle);
        btnAddCity.setText(R.string.btn_add_city);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        addParams.topMargin = dp(2);
        btnAddCity.setLayoutParams(addParams);
        btnAddCity.setOnClickListener(v -> addCityRow(citiesContainer));

        content.addView(headerRow);
        content.addView(citiesContainer);
        content.addView(btnAddCity);

        llDaysContainer.addView(cardView);
        days.add(dayData);
        addCityRow(citiesContainer);
    }

    private void addCityRow(LinearLayout citiesContainer) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(2);
        row.setLayoutParams(rowParams);

        EditText etCity = new EditText(this);
        etCity.setHint("City name");
        etCity.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        etParams.setMarginEnd(dp(4));
        etCity.setLayoutParams(etParams);

        Button btnRemove = new Button(this, null, android.R.attr.borderlessButtonStyle);
        btnRemove.setText(R.string.btn_remove);
        btnRemove.setOnClickListener(v -> citiesContainer.removeView(row));

        row.addView(etCity);
        row.addView(btnRemove);
        citiesContainer.addView(row);
    }

    private void pickDate(DayData dayData) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dayData.dateMillis);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 12, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            dayData.dateMillis = selected.getTimeInMillis();
            dayData.dateButton.setText(DATE_DISPLAY.format(selected.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void getWeather() {
        if (days.isEmpty()) {
            Toast.makeText(this, "Please add at least one day", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray planArray = new JSONArray();
            for (DayData day : days) {
                List<String> cities = new ArrayList<>();
                for (int i = 0; i < day.citiesContainer.getChildCount(); i++) {
                    LinearLayout row = (LinearLayout) day.citiesContainer.getChildAt(i);
                    EditText et = (EditText) row.getChildAt(0);
                    String city = et.getText().toString().trim();
                    if (!city.isEmpty()) cities.add(city);
                }
                if (cities.isEmpty()) continue;

                JSONObject dayObj = new JSONObject();
                dayObj.put("dateMillis", day.dateMillis);
                dayObj.put("dateDisplay", DATE_DISPLAY.format(new Date(day.dateMillis)));
                JSONArray citiesArray = new JSONArray();
                for (String c : cities) citiesArray.put(c);
                dayObj.put("cities", citiesArray);
                planArray.put(dayObj);
            }

            if (planArray.length() == 0) {
                Toast.makeText(this, "Please add at least one city", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, TripWeatherActivity.class);
            intent.putExtra("tripPlan", planArray.toString());
            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
