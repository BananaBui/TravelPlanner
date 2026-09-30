# TravelPlanner

An Android app that checks the weather for your trip and suggests what to wear. Pick where you're going and when, and the app gets the forecast and uses AI (LLM) to suggest a short clothing list for each stop.

## Features

- **Quick check.** Pick a city and a date to see the temperature, conditions, humidity and wind, plus a clothing list.
- **Trip planner.** Plan several days with one or more cities per day. The app shows the forecast and clothing tips for every stop in one view.
- **Saved trips.** Save results on your phone to look at later, even offline, and delete them when you don't need them.

## Built with

- **Java** and the **Android SDK** (min SDK 24)
- **Material Components** for the UI
- **OkHttp** for API requests
- **Room** for storing saved trips locally
- **[OpenWeatherMap API](https://openweathermap.org/api)** for current weather and forecasts
- **[Groq API](https://console.groq.com/)** with the Llama 3.3 model for clothing suggestions

## How it works

1. You pick a city and a date, or build a multi-day trip.
2. The app gets the weather from OpenWeatherMap. It uses today's weather for today, and the forecast reading closest to midday for future dates.
3. It sends the weather details to the AI model, which returns 3–5 clothing items for each stop. For a multi-day trip, all stops go into a single request so results come back faster.
4. The results appear on screen, and you can save them to the app's local database.

## Project structure

```
app/src/main/java/com/example/travelplanner/
├── MainActivity.java           # Home screen
├── WeatherActivity.java        # Quick check for one city
├── TripPlanActivity.java       # Build a multi-day trip
├── TripWeatherActivity.java    # Weather + tips for the whole trip
├── SavedTripsActivity.java     # List of saved trips
├── adapter/                    # List adapter for saved trips
└── db/                         # Room database (Trip, TripDao, TripDatabase)
```

## Notes

- Forecasts are only available up to 5 days ahead.
- API keys go in `Constants.java`, which isn't committed. Copy `Constants.java.example` to `Constants.java` and add your own OpenWeatherMap and Groq keys.
