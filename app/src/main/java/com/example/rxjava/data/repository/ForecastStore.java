package com.example.rxjava.data.repository;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

public interface ForecastStore {

    Maybe<Snapshot> read(Location location);

    Completable save(Location location, List<DailyForecast> forecasts, long fetchedAtMillis);

    record Snapshot(List<DailyForecast> forecasts, long fetchedAtMillis) {

        public Snapshot {
            List<DailyForecast> copy = new ArrayList<>(forecasts);
            for (DailyForecast forecast : copy) {
                Objects.requireNonNull(forecast, "予報一覧に null の要素がある");
            }
            forecasts = Collections.unmodifiableList(copy);
        }
    }
}
