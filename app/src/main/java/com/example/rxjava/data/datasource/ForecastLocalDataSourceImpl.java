package com.example.rxjava.data.datasource;

import com.example.rxjava.data.db.ForecastDatabase;
import com.example.rxjava.data.db.ForecastEntityMapper;
import com.example.rxjava.data.db.dao.ForecastDao;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class ForecastLocalDataSourceImpl implements ForecastLocalDataSource {

    private final ForecastDao dao;
    private final Scheduler transactionScheduler;

    @Inject
    public ForecastLocalDataSourceImpl(ForecastDatabase database) {
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
