package com.example.rxjava.data.remote;

import com.squareup.moshi.Json;

import java.util.List;

public final class ForecastResponse {

    public static final class Temperature {

        @Json(name = "max")
        public int max;

        @Json(name = "min")
        public int min;
    }

    public static final class Entry {

        @Json(name = "date")
        public int date;

        @Json(name = "weather")
        public String weather;

        @Json(name = "temp2m")
        public Temperature temperature;

        @Json(name = "wind10m_max")
        public int maxWind;
    }

    @Json(name = "product")
    public String product;

    @Json(name = "init")
    public String init;

    @Json(name = "dataseries")
    public List<Entry> dataseries;
}
