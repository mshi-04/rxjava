package com.example.rxjava.data.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.Temperature;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;
import com.example.rxjava.domain.model.WindSpeed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class ForecastStoreSnapshotTest {

    @Test
    void Snapshot_sourceListModifiedLater_keepsOriginalForecasts() {
        // Arrange
        DailyForecast forecast = clearOnAugust16();
        List<DailyForecast> source = new ArrayList<>();
        source.add(forecast);
        ForecastStore.Snapshot snapshot = new ForecastStore.Snapshot(source, 0L);

        // Act
        source.clear();

        // Assert
        assertAll(
                () -> assertEquals(1, snapshot.forecasts().size()),
                () -> assertSame(forecast, snapshot.forecasts().get(0)));
    }

    @Test
    void Snapshot_nullElement_throwsNullPointer() {
        // Arrange
        List<DailyForecast> forecasts = Arrays.asList(clearOnAugust16(), null);

        // Act
        Executable creation = () -> new ForecastStore.Snapshot(forecasts, 0L);

        // Assert
        assertThrows(NullPointerException.class, creation);
    }

    @Test
    void Snapshot_emptyList_isAccepted() {
        // Act
        ForecastStore.Snapshot snapshot = new ForecastStore.Snapshot(Collections.emptyList(), 1L);

        // Assert
        assertAll(
                () -> assertTrue(snapshot.forecasts().isEmpty()),
                () -> assertEquals(1L, snapshot.fetchedAtMillis()));
    }

    @Test
    void forecasts_modification_throwsUnsupportedOperation() {
        // Arrange
        ForecastStore.Snapshot snapshot =
                new ForecastStore.Snapshot(Collections.singletonList(clearOnAugust16()), 0L);

        // Act
        Executable modification = () -> snapshot.forecasts().clear();

        // Assert
        assertThrows(UnsupportedOperationException.class, modification);
    }

    private static DailyForecast clearOnAugust16() {
        return new DailyForecast(
                ForecastDate.of(2026, 8, 16),
                WeatherCondition.CLEAR,
                TemperatureRange.of(Temperature.ofCelsius(30), Temperature.ofCelsius(24)),
                WindSpeed.ofLevel(2));
    }
}
