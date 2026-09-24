package com.example.rxjava.fake;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.repository.ForecastRepository;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.SingleSubject;

@Singleton
public final class FakeForecastRepository implements ForecastRepository {

    // テストスレッドが書き、購読スレッドが読むため可視性を保証する
    private volatile Single<List<DailyForecast>> response = Single.just(Collections.emptyList());
    private volatile Location requestedLocation;
    private final AtomicInteger subscriptionCount = new AtomicInteger();
    private volatile SingleSubject<List<DailyForecast>> pendingResponse;

    @Inject
    public FakeForecastRepository() {
    }

    public void respondWith(List<DailyForecast> forecasts) {
        response = Single.just(forecasts);
    }

    public void failWith(Throwable error) {
        response = Single.error(error);
    }

    public void remainPending() {
        pendingResponse = SingleSubject.create();
        response = pendingResponse;
    }

    public void completePending(List<DailyForecast> forecasts) {
        pendingResponse.onSuccess(forecasts);
    }

    public int subscriptionCount() {
        return subscriptionCount.get();
    }

    public void resetSubscriptionCount() {
        subscriptionCount.set(0);
    }

    public Location requestedLocation() {
        return requestedLocation;
    }

    @Override
    public Single<List<DailyForecast>> find(Location location) {
        // defer で包み、購読の時点の設定を読む。再試行のテストが途中で応答を差し替える
        return Single.defer(() -> {
            subscriptionCount.incrementAndGet();
            requestedLocation = location;
            return response;
        });
    }
}
