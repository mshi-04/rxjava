package com.example.rxjava.presentation.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.Temperature;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;
import com.example.rxjava.domain.model.WindSpeed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ForecastUiStateTest {

    @Test
    void Success_forecasts_keepsForecasts() {
        // Arrange
        List<DailyForecast> forecasts = Collections.singletonList(clearOnAugust16());

        // Act
        ForecastUiState.Success state = new ForecastUiState.Success(forecasts);

        // Assert
        assertEquals(forecasts, state.forecasts());
    }

    @Test
    void Success_nullForecasts_throwsNullPointer() {
        // Act
        Executable creation = () -> new ForecastUiState.Success(null);

        // Assert
        assertThrows(NullPointerException.class, creation);
    }

    @Test
    void Success_sourceListModifiedLater_keepsOriginalForecasts() {
        // Arrange
        List<DailyForecast> source = new ArrayList<>();
        source.add(clearOnAugust16());
        ForecastUiState.Success state = new ForecastUiState.Success(source);

        // Act
        source.clear();

        // Assert
        assertEquals(1, state.forecasts().size());
    }

    @Test
    void forecasts_modification_throwsUnsupportedOperation() {
        // Arrange
        ForecastUiState.Success state =
                new ForecastUiState.Success(Collections.singletonList(clearOnAugust16()));

        // Act
        Executable modification = () -> state.forecasts().clear();

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
