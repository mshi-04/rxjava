package com.example.rxjava.data.datasource;

import com.example.rxjava.data.remote.ForecastApi;
import com.example.rxjava.data.util.RemoteForecastMapper;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class ForecastRemoteDataSourceImpl implements ForecastRemoteDataSource {

    private final ForecastApi api;

    @Inject
    public ForecastRemoteDataSourceImpl(ForecastApi api) {
        this.api = api;
    }

    @Override
    public Single<List<DailyForecast>> fetch(Location location) {
        return api.dailyForecast(location.getLatitude(), location.getLongitude())
                .map(RemoteForecastMapper::toDailyForecasts);
    }
}
