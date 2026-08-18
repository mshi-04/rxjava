package com.example.rxjava.data.remote;

import com.squareup.moshi.Json;

import java.util.List;

public final class ForecastResponse {

    public static final class Temperature {

        public int max;

        public int min;
    }

    public static final class Entry {

        public int date;

        public String weather;

        @Json(name = "temp2m")
        public Temperature temperature;

        @Json(name = "wind10m_max")
        public int maxWind;
    }

    public List<Entry> dataseries;
}
