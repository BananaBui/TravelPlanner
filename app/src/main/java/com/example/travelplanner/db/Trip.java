package com.example.travelplanner.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "trips")
public class Trip {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String city;
    public String country;
    public double temperature;
    public String description;
    public String recommendation;
    public long savedAt;
}
