package com.example.rxjava.presentation.viewmodel;

import com.example.rxjava.domain.model.DailyForecast;

import java.util.List;

public sealed interface ForecastUiState {

    record Loading() implements ForecastUiState {
    }

    record Success(List<DailyForecast> forecasts) implements ForecastUiState {

        public Success {
            forecasts = List.copyOf(forecasts);
        }
    }

    record Failure() implements ForecastUiState {
    }
}
