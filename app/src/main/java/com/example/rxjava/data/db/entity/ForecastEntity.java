package com.example.rxjava.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "forecasts", primaryKeys = {"latitude", "longitude", "position"})
public final class ForecastEntity {

    public double latitude;
    public double longitude;
    public int position;
    public int year;
    public int month;
    public int day;
    @NonNull
    public String weather = "";
    public int maxTemperature;
    public int minTemperature;
    public int windLevel;
}
