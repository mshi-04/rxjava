package com.example.rxjava.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "forecasts", primaryKeys = {"latitude", "longitude", "position"})
public final class Forecast {
    public double latitude;
    public double longitude;
    public int position;
    public int year;
    public int month;
    public int day;
    @NonNull
    public String weather = "UNKNOWN";
    public int maxTemperature;
    public int minTemperature;
    public int windLevel;
}
