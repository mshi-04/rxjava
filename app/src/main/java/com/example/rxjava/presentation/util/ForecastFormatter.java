package com.example.rxjava.presentation.util;

import androidx.annotation.StringRes;

import com.example.rxjava.R;
import com.example.rxjava.domain.model.WeatherCondition;

public final class ForecastFormatter {

    private ForecastFormatter() {
    }

    @StringRes
    public static int weatherLabelResId(WeatherCondition weather) {
        return switch (weather) {
            case CLEAR -> R.string.weather_clear;
            case PARTLY_CLOUDY -> R.string.weather_partly_cloudy;
            case MOSTLY_CLOUDY -> R.string.weather_mostly_cloudy;
            case CLOUDY -> R.string.weather_cloudy;
            case HUMID -> R.string.weather_humid;
            case LIGHT_RAIN -> R.string.weather_light_rain;
            case OCCASIONAL_SHOWERS -> R.string.weather_occasional_showers;
            case ISOLATED_SHOWERS -> R.string.weather_isolated_showers;
            case RAIN -> R.string.weather_rain;
            case LIGHT_SNOW -> R.string.weather_light_snow;
            case SNOW -> R.string.weather_snow;
            case RAIN_SNOW -> R.string.weather_rain_snow;
            case THUNDERSTORM -> R.string.weather_thunderstorm;
            case THUNDERSTORM_WITH_RAIN -> R.string.weather_thunderstorm_with_rain;
            case UNKNOWN -> R.string.weather_unknown;
        };
    }
}
