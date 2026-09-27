package com.example.rxjava.data.local;

import com.example.rxjava.data.repository.ForecastStore;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.model.Temperature;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;
import com.example.rxjava.domain.model.WindSpeed;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import io.reactivex.rxjava3.core.Maybe;

import javax.inject.Inject;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class RoomForecastStore implements ForecastStore {
    private final ForecastDatabase database;

    @Inject
    public RoomForecastStore(ForecastDatabase database) {
        this.database = database;
    }

    @Override
    public Single<Snapshot> read(Location location) {
        return Maybe.defer(() -> database.forecastDao()
                .readLastFetchedAt(location.getLatitude(), location.getLongitude()))
                .flatMapSingle(timestamp -> database.forecastDao()
                        .readForecasts(location.getLatitude(), location.getLongitude()).map(rows -> {
            List<DailyForecast> forecasts = new ArrayList<>();
            for (Forecast forecastEntity : rows) {
                forecasts.add(new DailyForecast(
                        ForecastDate.of(forecastEntity.year, forecastEntity.month, forecastEntity.day),
                        WeatherCondition.valueOf(forecastEntity.weather),
                        TemperatureRange.of(Temperature.ofCelsius(forecastEntity.maxTemperature),
                                Temperature.ofCelsius(forecastEntity.minTemperature)),
                        WindSpeed.ofLevel(forecastEntity.windLevel)));
            }
            return new Snapshot(forecasts, timestamp.lastFetchedAt);
        })).switchIfEmpty(Single.just(new Snapshot(Collections.emptyList(), null)));
    }

    @Override
    public Completable save(Location location, List<DailyForecast> forecasts, long lastFetchedAt) {
        return Completable.fromAction(() -> saveInTransaction(location, forecasts, lastFetchedAt))
                .subscribeOn(Schedulers.from(database.getTransactionExecutor()));
    }

    private void saveInTransaction(Location location, List<DailyForecast> forecasts, long lastFetchedAt) {
        List<Forecast> entities = new ArrayList<>();
        for (int position = 0; position < forecasts.size(); position++) {
            DailyForecast forecast = forecasts.get(position);
            Forecast forecastEntity = new Forecast();
            forecastEntity.latitude = location.getLatitude();
            forecastEntity.longitude = location.getLongitude();
            forecastEntity.position = position;
            forecastEntity.year = forecast.getDate().getYear();
            forecastEntity.month = forecast.getDate().getMonth();
            forecastEntity.day = forecast.getDate().getDay();
            forecastEntity.weather = forecast.getWeather().name();
            forecastEntity.maxTemperature = forecast.getTemperature().getMax().getCelsius();
            forecastEntity.minTemperature = forecast.getTemperature().getMin().getCelsius();
            forecastEntity.windLevel = forecast.getWind().getLevel();
            entities.add(forecastEntity);
        }
        database.runInTransaction(() -> {
            ForecastDao dao = database.forecastDao();
            dao.deleteForecasts(location.getLatitude(), location.getLongitude());
            dao.insertForecasts(entities);
            dao.insertLastFetchedAt(new LastFetchedAt(
                    location.getLatitude(), location.getLongitude(), lastFetchedAt));
        });
    }
}
