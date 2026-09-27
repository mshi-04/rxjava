package com.example.rxjava.data.local;

import androidx.room.Entity;

@Entity(tableName = "last_fetched_at", primaryKeys = {"latitude", "longitude"})
public final class LastFetchedAt {
    public double latitude;
    public double longitude;
    public long lastFetchedAt;

    public LastFetchedAt(double latitude, double longitude, long lastFetchedAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastFetchedAt = lastFetchedAt;
    }
}
