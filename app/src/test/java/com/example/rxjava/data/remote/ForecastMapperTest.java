package com.example.rxjava.data.remote;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class ForecastMapperTest {

    @Test
    void mapsEveryEntryInOrder() {
        ForecastResponse response = response(
                entry(20260816, "lightrain", 26, 25, 2),
                entry(20260817, "clear", 32, 24, 3));

        List<DailyForecast> forecasts = ForecastMapper.toDailyForecasts(response);

        assertAll(
                () -> assertEquals(2, forecasts.size()),
                () -> assertEquals(WeatherCondition.LIGHT_RAIN, forecasts.get(0).getWeather()),
                () -> assertEquals(WeatherCondition.CLEAR, forecasts.get(1).getWeather()));
    }

    @Test
    void splitsTheDateIntegerIntoYearMonthAndDay() {
        ForecastResponse response = response(entry(20260816, "clear", 30, 20, 2));

        ForecastDate date = ForecastMapper.toDailyForecasts(response).get(0).getDate();

        assertAll(
                () -> assertEquals(2026, date.getYear()),
                () -> assertEquals(8, date.getMonth()),
                () -> assertEquals(16, date.getDay()));
    }

    @Test
    void carriesTemperaturesAndWind() {
        ForecastResponse response = response(entry(20260816, "clear", 26, 25, 2));

        DailyForecast forecast = ForecastMapper.toDailyForecasts(response).get(0);
        TemperatureRange range = forecast.getTemperature();

        assertAll(
                () -> assertEquals(26, range.getMax().getCelsius()),
                () -> assertEquals(25, range.getMin().getCelsius()),
                () -> assertEquals(2, forecast.getWind().getLevel()));
    }

    @Test
    void fallsBackToUnknownForAnUnrecognisedWeatherCode() {
        ForecastResponse response = response(entry(20260816, "meteorshower", 26, 25, 2));

        DailyForecast forecast = ForecastMapper.toDailyForecasts(response).get(0);

        assertEquals(WeatherCondition.UNKNOWN, forecast.getWeather());
    }

    @Test
    void fallsBackToUnknownForAMissingWeatherCode() {
        ForecastResponse response = response(entry(20260816, null, 26, 25, 2));

        DailyForecast forecast = ForecastMapper.toDailyForecasts(response).get(0);

        assertEquals(WeatherCondition.UNKNOWN, forecast.getWeather());
    }

    @Test
    void returnsAnEmptyListForAnEmptyDataseries() {
        ForecastResponse response = new ForecastResponse();
        response.dataseries = Collections.emptyList();

        assertEquals(0, ForecastMapper.toDailyForecasts(response).size());
    }

    @Test
    void throwsWhenDataseriesIsMissing() {
        assertThrows(IllegalStateException.class,
                () -> ForecastMapper.toDailyForecasts(new ForecastResponse()));
    }

    @Test
    void throwsWhenTemperatureIsMissing() {
        ForecastResponse.Entry entry = new ForecastResponse.Entry();
        entry.date = 20260816;
        entry.weather = "clear";

        assertThrows(IllegalStateException.class,
                () -> ForecastMapper.toDailyForecasts(response(entry)));
    }

    private static ForecastResponse response(ForecastResponse.Entry... entries) {
        ForecastResponse response = new ForecastResponse();
        response.product = "civillight";
        response.init = "2026081606";
        response.dataseries = Arrays.asList(entries);
        return response;
    }

    private static ForecastResponse.Entry entry(
            int date, String weather, int max, int min, int wind) {
        ForecastResponse.Temperature temperature = new ForecastResponse.Temperature();
        temperature.max = max;
        temperature.min = min;

        ForecastResponse.Entry entry = new ForecastResponse.Entry();
        entry.date = date;
        entry.weather = weather;
        entry.temperature = temperature;
        entry.maxWind = wind;
        return entry;
    }
}
