package com.example.rxjava.data.repository;

import com.example.rxjava.data.remote.ForecastMapper;
import com.example.rxjava.data.remote.ForecastApi;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.repository.ForecastRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class RemoteForecastRepository implements ForecastRepository {

    private final ForecastApi api;

    @Inject
    public RemoteForecastRepository(ForecastApi api) {
        this.api = api;
    }

    @Override
    public Single<List<DailyForecast>> find(Location location) {
        return api.dailyForecast(location.getLatitude(), location.getLongitude())
                .map(ForecastMapper::toDailyForecasts);
    }
}
