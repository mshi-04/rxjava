package com.example.rxjava.data.datasource;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public interface ForecastDataSource {

    Single<List<DailyForecast>> fetch(Location location);
}
