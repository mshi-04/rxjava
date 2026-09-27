package com.example.rxjava.data.repository;

import com.example.rxjava.data.remote.ForecastApi;
import com.example.rxjava.data.remote.ForecastMapper;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.repository.ForecastRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Completable;

public final class RemoteForecastRepository implements ForecastRepository {

    private final ForecastApi api;
    private final ForecastStore store;
    private final TimeProvider time;
    private static final long CACHE_DURATION_MILLIS = 24L * 60 * 60 * 1000;

    @Inject
    public RemoteForecastRepository(ForecastApi api, ForecastStore store, TimeProvider time) {
        this.api = api;
        this.store = store;
        this.time = time;
    }

    @Override
    public Single<List<DailyForecast>> find(Location location) {
        return Single.defer(() -> store.read(location)).flatMap(cached -> {
            long now = time.currentTimeMillis();
            Long lastFetchedAt = cached.lastFetchedAt();
            if (lastFetchedAt != null && lastFetchedAt <= now
                    && now - lastFetchedAt >= 0 && now - lastFetchedAt < CACHE_DURATION_MILLIS) {
                return Single.just(cached.forecasts());
            }
            return api.dailyForecast(location.getLatitude(), location.getLongitude())
                    .map(ForecastMapper::toDailyForecasts)
                    .flatMap(forecasts -> Completable.defer(() ->
                            store.save(location, forecasts, time.currentTimeMillis()))
                            .andThen(Single.just(forecasts)));
        });
    }
}
