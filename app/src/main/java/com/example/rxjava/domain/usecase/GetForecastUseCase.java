package com.example.rxjava.domain.usecase;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.repository.ForecastRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public final class GetForecastUseCase {

    private final ForecastRepository repository;

    @Inject
    public GetForecastUseCase(ForecastRepository repository) {
        this.repository = repository;
    }

    public Single<List<DailyForecast>> invoke(Location location) {
        return repository.find(location);
    }
}
