package com.example.rxjava.data.local;

import com.example.rxjava.data.local.entity.ForecastEntity;
import com.example.rxjava.data.local.entity.LastFetchedAtEntity;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.model.Temperature;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;
import com.example.rxjava.domain.model.WindSpeed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ForecastEntityMapper {

    private static final Map<WeatherCondition, String> STORED_WEATHER = storedWeather();
    private static final Map<String, WeatherCondition> CONDITIONS = conditions(STORED_WEATHER);

    private ForecastEntityMapper() {
    }

    static List<DailyForecast> toDomain(List<ForecastEntity> rows) {
        List<DailyForecast> forecasts = new ArrayList<>(rows.size());
        for (ForecastEntity row : rows) {
            forecasts.add(toDailyForecast(row));
        }
        return Collections.unmodifiableList(forecasts);
    }

    static List<ForecastEntity> toEntities(Location location, List<DailyForecast> forecasts) {
        List<ForecastEntity> rows = new ArrayList<>(forecasts.size());
        for (int position = 0; position < forecasts.size(); position++) {
            rows.add(toEntity(location, position, forecasts.get(position)));
        }
        return rows;
    }

    static LastFetchedAtEntity toLastFetchedAtEntity(Location location, long fetchedAtMillis) {
        LastFetchedAtEntity row = new LastFetchedAtEntity();
        row.latitude = location.getLatitude();
        row.longitude = location.getLongitude();
        row.lastFetchedAt = fetchedAtMillis;
        return row;
    }

    private static DailyForecast toDailyForecast(ForecastEntity row) {
        return new DailyForecast(
                ForecastDate.of(row.year, row.month, row.day),
                toWeatherCondition(row.weather),
                TemperatureRange.of(
                        Temperature.ofCelsius(row.maxTemperature),
                        Temperature.ofCelsius(row.minTemperature)),
                WindSpeed.ofLevel(row.windLevel));
    }

    private static ForecastEntity toEntity(Location location, int position, DailyForecast forecast) {
        ForecastEntity row = new ForecastEntity();
        row.latitude = location.getLatitude();
        row.longitude = location.getLongitude();
        row.position = position;
        row.year = forecast.getDate().getYear();
        row.month = forecast.getDate().getMonth();
        row.day = forecast.getDate().getDay();
        row.weather = toStoredWeather(forecast.getWeather());
        row.maxTemperature = forecast.getTemperature().getMax().getCelsius();
        row.minTemperature = forecast.getTemperature().getMin().getCelsius();
        row.windLevel = forecast.getWind().getLevel();
        return row;
    }

    private static WeatherCondition toWeatherCondition(String stored) {
        WeatherCondition condition = CONDITIONS.get(stored);
        if (condition == null) {
            throw new IllegalStateException("対応表にない保存済みの天気区分: " + stored);
        }
        return condition;
    }

    private static String toStoredWeather(WeatherCondition condition) {
        String stored = STORED_WEATHER.get(condition);
        if (stored == null) {
            throw new IllegalStateException("保存用の文字列がない天気区分: " + condition);
        }
        return stored;
    }

    private static Map<WeatherCondition, String> storedWeather() {
        Map<WeatherCondition, String> map = new EnumMap<>(WeatherCondition.class);
        map.put(WeatherCondition.CLEAR, "CLEAR");
        map.put(WeatherCondition.PARTLY_CLOUDY, "PARTLY_CLOUDY");
        map.put(WeatherCondition.MOSTLY_CLOUDY, "MOSTLY_CLOUDY");
        map.put(WeatherCondition.CLOUDY, "CLOUDY");
        map.put(WeatherCondition.HUMID, "HUMID");
        map.put(WeatherCondition.LIGHT_RAIN, "LIGHT_RAIN");
        map.put(WeatherCondition.OCCASIONAL_SHOWERS, "OCCASIONAL_SHOWERS");
        map.put(WeatherCondition.ISOLATED_SHOWERS, "ISOLATED_SHOWERS");
        map.put(WeatherCondition.RAIN, "RAIN");
        map.put(WeatherCondition.LIGHT_SNOW, "LIGHT_SNOW");
        map.put(WeatherCondition.SNOW, "SNOW");
        map.put(WeatherCondition.RAIN_SNOW, "RAIN_SNOW");
        map.put(WeatherCondition.THUNDERSTORM, "THUNDERSTORM");
        map.put(WeatherCondition.THUNDERSTORM_WITH_RAIN, "THUNDERSTORM_WITH_RAIN");
        map.put(WeatherCondition.UNKNOWN, "UNKNOWN");
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, WeatherCondition> conditions(Map<WeatherCondition, String> storedWeather) {
        Map<String, WeatherCondition> map = new HashMap<>();
        for (Map.Entry<WeatherCondition, String> entry : storedWeather.entrySet()) {
            map.put(entry.getValue(), entry.getKey());
        }
        return Collections.unmodifiableMap(map);
    }
}
