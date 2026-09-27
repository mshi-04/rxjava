package com.example.rxjava.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Forecast.class, LastFetchedAt.class}, version = 1, exportSchema = true)
public abstract class ForecastDatabase extends RoomDatabase {
    public abstract ForecastDao forecastDao();
}
