package com.example.rxjava.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.rxjava.data.local.dao.ForecastDao;
import com.example.rxjava.data.local.entity.ForecastEntity;
import com.example.rxjava.data.local.entity.LastFetchedAtEntity;

@Database(entities = {ForecastEntity.class, LastFetchedAtEntity.class}, version = 1, exportSchema = true)
public abstract class ForecastDatabase extends RoomDatabase {

    public abstract ForecastDao forecastDao();
}
