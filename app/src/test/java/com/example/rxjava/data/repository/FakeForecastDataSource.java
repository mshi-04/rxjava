package com.example.rxjava.data.repository;

import com.example.rxjava.data.datasource.ForecastDataSource;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;

import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Single;

final class FakeForecastDataSource implements ForecastDataSource {

    Single<List<DailyForecast>> response = Single.just(Collections.emptyList());
    int fetches;

    @Override
    public Single<List<DailyForecast>> fetch(Location location) {
        return Single.defer(() -> {
            fetches++;
            return response;
        });
    }
}
