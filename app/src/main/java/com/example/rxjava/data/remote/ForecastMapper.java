package com.example.rxjava.data.remote;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.Temperature;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;
import com.example.rxjava.domain.model.WindSpeed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ForecastMapper {

    private static final Map<String, WeatherCondition> CONDITIONS = conditions();

    private ForecastMapper() {
    }

    public static List<DailyForecast> toDailyForecasts(ForecastResponse response) {
        if (response == null || response.dataseries == null) {
            throw new IllegalStateException("dataseries を含まないレスポンス");
        }

        List<DailyForecast> forecasts = new ArrayList<>(response.dataseries.size());
        for (ForecastResponse.Entry entry : response.dataseries) {
            forecasts.add(toDailyForecast(entry));
        }
        return Collections.unmodifiableList(forecasts);
    }

    private static DailyForecast toDailyForecast(ForecastResponse.Entry entry) {
        if (entry == null) {
            throw new IllegalStateException("dataseries に空の要素がある");
        }
        if (entry.temperature == null) {
            throw new IllegalStateException("temp2m を含まない要素: " + entry.date);
        }

        return new DailyForecast(
                toForecastDate(entry.date),
                toWeatherCondition(entry.weather),
                toTemperatureRange(entry.temperature),
                WindSpeed.ofLevel(entry.maxWind));
    }

    private static ForecastDate toForecastDate(int yyyymmdd) {
        return ForecastDate.of(yyyymmdd / 10000, yyyymmdd / 100 % 100, yyyymmdd % 100);
    }

    private static TemperatureRange toTemperatureRange(ForecastResponse.Temperature temperature) {
        return TemperatureRange.of(
                Temperature.ofCelsius(temperature.max),
                Temperature.ofCelsius(temperature.min));
    }

    private static WeatherCondition toWeatherCondition(String code) {
        return CONDITIONS.getOrDefault(code, WeatherCondition.UNKNOWN);
    }

    private static Map<String, WeatherCondition> conditions() {
        Map<String, WeatherCondition> map = new HashMap<>();
        map.put("clear", WeatherCondition.CLEAR);
        map.put("pcloudy", WeatherCondition.PARTLY_CLOUDY);
        map.put("mcloudy", WeatherCondition.MOSTLY_CLOUDY);
        map.put("cloudy", WeatherCondition.CLOUDY);
        map.put("humid", WeatherCondition.HUMID);
        map.put("lightrain", WeatherCondition.LIGHT_RAIN);
        map.put("oshower", WeatherCondition.OCCASIONAL_SHOWERS);
        map.put("ishower", WeatherCondition.ISOLATED_SHOWERS);
        map.put("rain", WeatherCondition.RAIN);
        map.put("lightsnow", WeatherCondition.LIGHT_SNOW);
        map.put("snow", WeatherCondition.SNOW);
        map.put("rainsnow", WeatherCondition.RAIN_SNOW);
        map.put("ts", WeatherCondition.THUNDERSTORM);
        map.put("tsrain", WeatherCondition.THUNDERSTORM_WITH_RAIN);
        return Collections.unmodifiableMap(map);
    }
}
