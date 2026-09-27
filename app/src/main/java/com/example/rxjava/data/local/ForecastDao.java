package com.example.rxjava.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Maybe;

import java.util.List;

@Dao
public interface ForecastDao {
    @Query("SELECT * FROM forecasts WHERE latitude = :latitude AND longitude = :longitude ORDER BY position")
    Single<List<Forecast>> readForecasts(double latitude, double longitude);

    @Query("SELECT * FROM last_fetched_at WHERE latitude = :latitude AND longitude = :longitude")
    Maybe<LastFetchedAt> readLastFetchedAt(double latitude, double longitude);

    @Query("DELETE FROM forecasts WHERE latitude = :latitude AND longitude = :longitude")
    void deleteForecasts(double latitude, double longitude);

    @Insert
    void insertForecasts(List<Forecast> forecasts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLastFetchedAt(LastFetchedAt lastFetchedAt);
}
