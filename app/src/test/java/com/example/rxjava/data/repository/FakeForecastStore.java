package com.example.rxjava.data.repository;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Completable;

final class FakeForecastStore implements ForecastStore {
    final Map<String, Snapshot> snapshots = new HashMap<>();
    RuntimeException readFailure;
    RuntimeException saveFailure;
    int reads;
    int saves;
    Completable saveGate = Completable.complete();
    Single<Snapshot> readOverride;

    @Override
    public Single<Snapshot> read(Location location) {
        return Single.defer(() -> {
            reads++;
            if (readFailure != null) return Single.error(readFailure);
            if (readOverride != null) return readOverride;
            return Single.just(snapshots.getOrDefault(key(location),
                    new Snapshot(java.util.Collections.emptyList(), null)));
        });
    }

    @Override
    public Completable save(Location location, List<DailyForecast> forecasts, long lastFetchedAt) {
        return Completable.defer(() -> saveGate.andThen(Completable.fromAction(() -> {
            if (saveFailure != null) throw saveFailure;
            saves++;
            snapshots.put(key(location), new Snapshot(forecasts, lastFetchedAt));
        })));
    }

    private String key(Location location) {
        return location.getLatitude() + ":" + location.getLongitude();
    }
}
