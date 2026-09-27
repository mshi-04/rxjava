package com.example.rxjava.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.rxjava.data.db.entity.ForecastEntity;
import com.example.rxjava.data.db.entity.LastFetchedAtEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

@Dao
public abstract class ForecastDao {

    @Query("SELECT * FROM forecasts WHERE latitude = :latitude AND longitude = :longitude ORDER BY position")
    public abstract Single<List<ForecastEntity>> readForecasts(double latitude, double longitude);

    @Query("SELECT * FROM last_fetched_at WHERE latitude = :latitude AND longitude = :longitude")
    public abstract Maybe<LastFetchedAtEntity> readLastFetchedAt(double latitude, double longitude);

    @Transaction
    public void replace(
            double latitude,
            double longitude,
            List<ForecastEntity> forecasts,
            LastFetchedAtEntity lastFetchedAt) {
        deleteForecasts(latitude, longitude);
        insertForecasts(forecasts);
        insertLastFetchedAt(lastFetchedAt);
    }

    @Query("DELETE FROM forecasts WHERE latitude = :latitude AND longitude = :longitude")
    abstract void deleteForecasts(double latitude, double longitude);

    @Insert
    abstract void insertForecasts(List<ForecastEntity> forecasts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertLastFetchedAt(LastFetchedAtEntity lastFetchedAt);
}
