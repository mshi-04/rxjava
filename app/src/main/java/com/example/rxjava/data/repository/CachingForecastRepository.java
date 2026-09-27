package com.example.rxjava.data.repository;

import com.example.rxjava.data.datasource.ForecastDataSource;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.repository.ForecastRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

public final class CachingForecastRepository implements ForecastRepository {

    private static final long CACHE_DURATION_MILLIS = TimeUnit.HOURS.toMillis(24);

    private final ForecastDataSource dataSource;
    private final ForecastStore store;
    private final TimeProvider time;

    @Inject
    public CachingForecastRepository(ForecastDataSource dataSource, ForecastStore store, TimeProvider time) {
        this.dataSource = dataSource;
        this.store = store;
        this.time = time;
    }

    @Override
    public Single<List<DailyForecast>> find(Location location) {
        return store.read(location)
                .filter(this::isFresh)
                .map(ForecastStore.Snapshot::forecasts)
                .switchIfEmpty(Single.defer(() -> fetchAndSave(location)));
    }

    private boolean isFresh(ForecastStore.Snapshot snapshot) {
        long elapsed = time.currentTimeMillis() - snapshot.fetchedAtMillis();
        return elapsed >= 0 && elapsed < CACHE_DURATION_MILLIS;
    }

    private Single<List<DailyForecast>> fetchAndSave(Location location) {
        return dataSource.fetch(location).flatMap(forecasts -> Completable
                .defer(() -> store.save(location, forecasts, time.currentTimeMillis()))
                .andThen(Single.just(forecasts)));
    }
}
