package com.example.rxjava.fake;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.repository.ForecastRepository;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;

@Singleton
public final class FakeForecastRepository implements ForecastRepository {

    private Single<List<DailyForecast>> response = Single.just(Collections.emptyList());

    @Inject
    public FakeForecastRepository() {
    }

    public void respondWith(List<DailyForecast> forecasts) {
        response = Single.just(forecasts);
    }

    public void failWith(Throwable error) {
        response = Single.error(error);
    }

    @Override
    public Single<List<DailyForecast>> find(Location location) {
        // defer で包み、購読の時点の設定を読む。再試行のテストが途中で応答を差し替える
        return Single.defer(() -> response);
    }
}
