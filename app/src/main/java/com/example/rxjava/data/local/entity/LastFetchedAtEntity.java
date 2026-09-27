package com.example.rxjava.data.local.entity;

import androidx.room.Entity;

@Entity(tableName = "last_fetched_at", primaryKeys = {"latitude", "longitude"})
public final class LastFetchedAtEntity {

    public double latitude;
    public double longitude;
    public long lastFetchedAt;
}
