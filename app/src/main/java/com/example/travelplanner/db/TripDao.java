package com.example.travelplanner.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TripDao {
    @Insert
    void insert(Trip trip);

    @Query("SELECT * FROM trips ORDER BY savedAt DESC")
    List<Trip> getAll();

    @Delete
    void delete(Trip trip);
}
