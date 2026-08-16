package com.example.rxjava.presentation.util;

import androidx.annotation.StringRes;

import com.example.rxjava.R;
import com.example.rxjava.domain.model.WeatherCondition;

public final class ForecastFormatter {

    private ForecastFormatter() {
    }

    @StringRes
    public static int weatherLabelResId(WeatherCondition weather) {
        switch (weather) {
            case CLEAR:
                return R.string.weather_clear;
            case PARTLY_CLOUDY:
                return R.string.weather_partly_cloudy;
            case MOSTLY_CLOUDY:
                return R.string.weather_mostly_cloudy;
            case CLOUDY:
                return R.string.weather_cloudy;
            case HUMID:
                return R.string.weather_humid;
            case LIGHT_RAIN:
                return R.string.weather_light_rain;
            case OCCASIONAL_SHOWERS:
                return R.string.weather_occasional_showers;
            case ISOLATED_SHOWERS:
                return R.string.weather_isolated_showers;
            case RAIN:
                return R.string.weather_rain;
            case LIGHT_SNOW:
                return R.string.weather_light_snow;
            case SNOW:
                return R.string.weather_snow;
            case RAIN_SNOW:
                return R.string.weather_rain_snow;
            case THUNDERSTORM:
                return R.string.weather_thunderstorm;
            case THUNDERSTORM_WITH_RAIN:
                return R.string.weather_thunderstorm_with_rain;
            case UNKNOWN:
            default:
                return R.string.weather_unknown;
        }
    }
}
