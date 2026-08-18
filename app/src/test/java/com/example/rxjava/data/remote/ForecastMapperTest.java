package com.example.rxjava.data.remote;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class ForecastMapperTest {

    @Test
    void toDailyForecasts_multipleEntries_mapsAllInOrder() {
        // Arrange
        ForecastResponse response = response(
                entry(20260816, "lightrain", 26, 25, 2),
                entry(20260817, "clear", 32, 24, 3));

        // Act
        List<DailyForecast> forecasts = ForecastMapper.toDailyForecasts(response);

        // Assert
        assertAll(
                () -> assertEquals(2, forecasts.size()),
                () -> assertEquals(WeatherCondition.LIGHT_RAIN, forecasts.get(0).getWeather()),
                () -> assertEquals(WeatherCondition.CLEAR, forecasts.get(1).getWeather()));
    }

    @Test
    void toDailyForecasts_dateInteger_splitsIntoYearMonthAndDay() {
        // Arrange
        ForecastResponse response = response(entry(20260816, "clear", 30, 20, 2));

        // Act
        ForecastDate date = ForecastMapper.toDailyForecasts(response).get(0).getDate();

        // Assert
        assertAll(
                () -> assertEquals(2026, date.getYear()),
                () -> assertEquals(8, date.getMonth()),
                () -> assertEquals(16, date.getDay()));
    }

    @Test
    void toDailyForecasts_entry_carriesTemperatureAndWind() {
        // Arrange
        ForecastResponse response = response(entry(20260816, "clear", 26, 25, 2));

        // Act
        DailyForecast forecast = ForecastMapper.toDailyForecasts(response).get(0);
        TemperatureRange range = forecast.getTemperature();

        // Assert
        assertAll(
                () -> assertEquals(26, range.getMax().getCelsius()),
                () -> assertEquals(25, range.getMin().getCelsius()),
                () -> assertEquals(2, forecast.getWind().getLevel()));
    }

    @Test
    void toDailyForecasts_unknownWeatherCode_fallsBackToUnknown() {
        // Arrange
        ForecastResponse response = response(entry(20260816, "meteorshower", 26, 25, 2));

        // Act
        DailyForecast forecast = ForecastMapper.toDailyForecasts(response).get(0);

        // Assert
        assertEquals(WeatherCondition.UNKNOWN, forecast.getWeather());
    }

    @Test
    void toDailyForecasts_missingWeatherCode_fallsBackToUnknown() {
        // Arrange
        ForecastResponse response = response(entry(20260816, null, 26, 25, 2));

        // Act
        DailyForecast forecast = ForecastMapper.toDailyForecasts(response).get(0);

        // Assert
        assertEquals(WeatherCondition.UNKNOWN, forecast.getWeather());
    }

    @Test
    void toDailyForecasts_emptyDataseries_returnsEmptyList() {
        // Arrange
        ForecastResponse response = new ForecastResponse();
        response.dataseries = Collections.emptyList();

        // Act
        List<DailyForecast> forecasts = ForecastMapper.toDailyForecasts(response);

        // Assert
        assertEquals(0, forecasts.size());
    }

    @Test
    void toDailyForecasts_missingDataseries_throwsIllegalState() {
        // Arrange
        Executable mapping = () -> ForecastMapper.toDailyForecasts(new ForecastResponse());

        // Act & Assert
        assertThrows(IllegalStateException.class, mapping);
    }

    @Test
    void toDailyForecasts_missingTemperature_throwsIllegalState() {
        // Arrange
        ForecastResponse.Entry entry = new ForecastResponse.Entry();
        entry.date = 20260816;
        entry.weather = "clear";
        Executable mapping = () -> ForecastMapper.toDailyForecasts(response(entry));

        // Act & Assert
        assertThrows(IllegalStateException.class, mapping);
    }

    @Test
    void toDailyForecasts_windLevelOutOfRange_throwsIllegalArgument() {
        // Arrange
        Executable mapping = () ->
                ForecastMapper.toDailyForecasts(response(entry(20260816, "clear", 26, 25, 99)));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, mapping);
    }

    private static ForecastResponse response(ForecastResponse.Entry... entries) {
        ForecastResponse response = new ForecastResponse();
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
