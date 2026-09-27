package com.example.rxjava.data.repository;

import com.example.rxjava.data.datasource.ForecastLocalDataSource;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;

final class FakeForecastLocalDataSource implements ForecastLocalDataSource {

    private final Map<String, Snapshot> snapshots = new HashMap<>();
    RuntimeException readFailure;
    RuntimeException saveFailure;
    Maybe<Snapshot> readOverride;
    Completable saveGate = Completable.complete();
    int reads;
    int saves;

    @Override
    public Maybe<Snapshot> read(Location location) {
        return Maybe.defer(() -> {
            reads++;
            if (readFailure != null) {
                return Maybe.error(readFailure);
            }
            if (readOverride != null) {
                return readOverride;
            }
            Snapshot snapshot = snapshots.get(key(location));
            return snapshot == null ? Maybe.empty() : Maybe.just(snapshot);
        });
    }

    @Override
    public Completable save(Location location, List<DailyForecast> forecasts, long fetchedAtMillis) {
        Snapshot snapshot = new Snapshot(forecasts, fetchedAtMillis);
        return Completable.defer(() -> saveGate.andThen(Completable.fromAction(() -> {
            if (saveFailure != null) {
                throw saveFailure;
            }
            saves++;
            snapshots.put(key(location), snapshot);
        })));
    }

    void put(Location location, Snapshot snapshot) {
        snapshots.put(key(location), snapshot);
    }

    Snapshot stored(Location location) {
        return snapshots.get(key(location));
    }

    private static String key(Location location) {
        return location.getLatitude() + ":" + location.getLongitude();
    }
}
