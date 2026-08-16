package com.example.rxjava.domain.repository;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public interface ForecastRepository {

    Single<List<DailyForecast>> find(Location location);
}
