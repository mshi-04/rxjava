package com.example.rxjava.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.rxjava.data.db.dao.ForecastDao;
import com.example.rxjava.data.db.entity.ForecastEntity;
import com.example.rxjava.data.db.entity.LastFetchedAtEntity;

@Database(entities = {ForecastEntity.class, LastFetchedAtEntity.class}, version = 1, exportSchema = true)
public abstract class ForecastDatabase extends RoomDatabase {

    public abstract ForecastDao forecastDao();
}
