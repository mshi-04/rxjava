package com.example.rxjava.data.local;

import com.example.rxjava.data.local.dao.ForecastDao;
import com.example.rxjava.data.repository.ForecastStore;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class ForecastStoreImpl implements ForecastStore {

    private final ForecastDao dao;
    private final Scheduler transactionScheduler;

    @Inject
    public ForecastStoreImpl(ForecastDatabase database) {
        this.dao = database.forecastDao();
        this.transactionScheduler = Schedulers.from(database.getTransactionExecutor());
    }

    @Override
    public Maybe<Snapshot> read(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        return dao.readLastFetchedAt(latitude, longitude)
                .flatMapSingle(lastFetchedAt -> dao.readForecasts(latitude, longitude)
                        .map(rows -> new Snapshot(
                                ForecastEntityMapper.toDomain(rows),
                                lastFetchedAt.lastFetchedAt)));
    }

    @Override
    public Completable save(Location location, List<DailyForecast> forecasts, long fetchedAtMillis) {
        List<DailyForecast> forecastsAtCall = new ArrayList<>(forecasts);
        return Completable.fromAction(() -> dao.replace(
                        location.getLatitude(),
                        location.getLongitude(),
                        ForecastEntityMapper.toEntities(location, forecastsAtCall),
                        ForecastEntityMapper.toLastFetchedAtEntity(location, fetchedAtMillis)))
                .subscribeOn(transactionScheduler);
    }
}
