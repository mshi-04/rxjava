package com.example.rxjava.data.local;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.rxjava.data.local.entity.ForecastEntity;
import com.example.rxjava.data.local.entity.LastFetchedAtEntity;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.model.Temperature;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;
import com.example.rxjava.domain.model.WindSpeed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class ForecastEntityMapperTest {

    @Test
    void toEntities_eachWeatherCondition_storesFixedString() {
        // Arrange
        Map<WeatherCondition, String> expected = expectedStoredWeather();

        // Act
        Map<WeatherCondition, String> stored = new EnumMap<>(WeatherCondition.class);
        for (WeatherCondition condition : WeatherCondition.values()) {
            stored.put(condition, storeWeather(condition));
        }

        // Assert
        assertAll(Stream.of(WeatherCondition.values()).map(condition ->
                () -> assertEquals(expected.get(condition), stored.get(condition), condition.toString())));
    }

    @Test
    void toDomain_eachStoredWeatherString_restoresWeatherCondition() {
        // Arrange
        Map<WeatherCondition, String> storedWeather = expectedStoredWeather();

        // Act
        Map<WeatherCondition, WeatherCondition> restored = new EnumMap<>(WeatherCondition.class);
        for (Map.Entry<WeatherCondition, String> entry : storedWeather.entrySet()) {
            restored.put(entry.getKey(), restoreWeather(entry.getValue()));
        }

        // Assert
        assertAll(storedWeather.keySet().stream().map(condition ->
                () -> assertEquals(condition, restored.get(condition), condition.toString())));
    }

    @Test
    void toDomain_unknownStoredWeather_throwsIllegalStateNamingValue() {
        // Arrange
        ForecastEntity row = row(0, 16, "METEOR_SHOWER");
        List<ForecastEntity> rows = Collections.singletonList(row);

        // Act
        Executable mapping = () -> ForecastEntityMapper.toDomain(rows);

        // Assert
        IllegalStateException thrown = assertThrows(IllegalStateException.class, mapping);
        assertTrue(thrown.getMessage().contains("METEOR_SHOWER"), thrown.getMessage());
    }

    @Test
    void toDomain_outOfRangeWindLevel_throwsIllegalArgument() {
        // Arrange
        ForecastEntity row = row(0, 16, "CLEAR");
        row.windLevel = 99;
        List<ForecastEntity> rows = Collections.singletonList(row);

        // Act
        Executable mapping = () -> ForecastEntityMapper.toDomain(rows);

        // Assert
        assertThrows(IllegalArgumentException.class, mapping);
    }

    @Test
    void toEntities_forecasts_setsLocationAndPositionsInOrder() {
        // Arrange
        Location location = Location.of(34.5, 135.25);
        List<DailyForecast> forecasts = Arrays.asList(
                forecast(16, WeatherCondition.CLEAR),
                forecast(17, WeatherCondition.RAIN),
                forecast(18, WeatherCondition.SNOW));

        // Act
        List<ForecastEntity> rows = ForecastEntityMapper.toEntities(location, forecasts);

        // Assert
        assertAll(
                () -> assertEquals(3, rows.size()),
                () -> assertAll(IntStream.range(0, rows.size()).mapToObj(index -> () -> assertAll(
                        () -> assertEquals(index, rows.get(index).position),
                        () -> assertEquals(34.5, rows.get(index).latitude),
                        () -> assertEquals(135.25, rows.get(index).longitude),
                        () -> assertEquals(16 + index, rows.get(index).day)))));
    }

    @Test
    void toDomain_entitiesFromToEntities_keepsAllFieldsAndOrder() {
        // Arrange
        List<DailyForecast> forecasts = Arrays.asList(
                new DailyForecast(ForecastDate.of(2026, 9, 17), WeatherCondition.LIGHT_RAIN,
                        TemperatureRange.of(Temperature.ofCelsius(26), Temperature.ofCelsius(-5)),
                        WindSpeed.ofLevel(2)),
                new DailyForecast(ForecastDate.of(2026, 10, 1), WeatherCondition.UNKNOWN,
                        TemperatureRange.of(Temperature.ofCelsius(30), Temperature.ofCelsius(20)),
                        WindSpeed.ofLevel(8)));
        List<ForecastEntity> rows = ForecastEntityMapper.toEntities(Location.TOKYO, forecasts);

        // Act
        List<DailyForecast> restored = ForecastEntityMapper.toDomain(rows);

        // Assert
        assertAll(
                () -> assertEquals(2, restored.size()),
                () -> assertEquals(2026, restored.get(0).getDate().getYear()),
                () -> assertEquals(9, restored.get(0).getDate().getMonth()),
                () -> assertEquals(17, restored.get(0).getDate().getDay()),
                () -> assertEquals(WeatherCondition.LIGHT_RAIN, restored.get(0).getWeather()),
                () -> assertEquals(26, restored.get(0).getTemperature().getMax().getCelsius()),
                () -> assertEquals(-5, restored.get(0).getTemperature().getMin().getCelsius()),
                () -> assertEquals(2, restored.get(0).getWind().getLevel()),
                () -> assertEquals(10, restored.get(1).getDate().getMonth()),
                () -> assertEquals(1, restored.get(1).getDate().getDay()),
                () -> assertEquals(WeatherCondition.UNKNOWN, restored.get(1).getWeather()),
                () -> assertEquals(8, restored.get(1).getWind().getLevel()));
    }

    @Test
    void toLastFetchedAtEntity_locationAndMillis_setsAllFields() {
        // Arrange
        Location location = Location.of(34.5, 135.25);

        // Act
        LastFetchedAtEntity row = ForecastEntityMapper.toLastFetchedAtEntity(location, 1_789_000_000_000L);

        // Assert
        assertAll(
                () -> assertEquals(34.5, row.latitude),
                () -> assertEquals(135.25, row.longitude),
                () -> assertEquals(1_789_000_000_000L, row.lastFetchedAt));
    }

    private static Map<WeatherCondition, String> expectedStoredWeather() {
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
        return map;
    }

    private static String storeWeather(WeatherCondition condition) {
        List<DailyForecast> forecasts = Collections.singletonList(forecast(16, condition));
        return ForecastEntityMapper.toEntities(Location.TOKYO, forecasts).get(0).weather;
    }

    private static WeatherCondition restoreWeather(String stored) {
        List<ForecastEntity> rows = Collections.singletonList(row(0, 16, stored));
        return ForecastEntityMapper.toDomain(rows).get(0).getWeather();
    }

    private static DailyForecast forecast(int day, WeatherCondition weather) {
        return new DailyForecast(ForecastDate.of(2026, 9, day), weather,
                TemperatureRange.of(Temperature.ofCelsius(30), Temperature.ofCelsius(20)),
                WindSpeed.ofLevel(3));
    }

    private static ForecastEntity row(int position, int day, String weather) {
        ForecastEntity row = new ForecastEntity();
        row.latitude = Location.TOKYO.getLatitude();
        row.longitude = Location.TOKYO.getLongitude();
        row.position = position;
        row.year = 2026;
        row.month = 9;
        row.day = day;
        row.weather = weather;
        row.maxTemperature = 30;
        row.minTemperature = 20;
        row.windLevel = 3;
        return row;
    }
}
