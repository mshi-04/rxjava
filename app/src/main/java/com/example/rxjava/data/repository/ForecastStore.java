package com.example.rxjava.data.repository;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Completable;

public interface ForecastStore {
    Single<Snapshot> read(Location location);
    Completable save(Location location, List<DailyForecast> forecasts, long lastFetchedAt);

    record Snapshot(List<DailyForecast> forecasts, Long lastFetchedAt) {
        public Snapshot {
            forecasts = Collections.unmodifiableList(new ArrayList<>(forecasts));
        }
    }
}
