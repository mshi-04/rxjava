package com.example.rxjava.domain.model;

import java.util.Objects;

public final class DailyForecast {

    private final ForecastDate date;
    private final WeatherCondition weather;
    private final TemperatureRange temperature;
    private final WindSpeed wind;

    public DailyForecast(
            ForecastDate date,
            WeatherCondition weather,
            TemperatureRange temperature,
            WindSpeed wind) {
        this.date = Objects.requireNonNull(date);
        this.weather = Objects.requireNonNull(weather);
        this.temperature = Objects.requireNonNull(temperature);
        this.wind = Objects.requireNonNull(wind);
    }

    public ForecastDate getDate() {
        return date;
    }

    public WeatherCondition getWeather() {
        return weather;
    }

    public TemperatureRange getTemperature() {
        return temperature;
    }

    public WindSpeed getWind() {
        return wind;
    }
}
