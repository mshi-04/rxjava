package com.example.rxjava.data.remote;

import com.squareup.moshi.Json;

import java.util.List;

public final class ForecastResponse {

    public static final class Temperature {

        public Integer max;

        public Integer min;
    }

    public static final class Entry {

        public Integer date;

        public String weather;

        @Json(name = "temp2m")
        public Temperature temperature;

        @Json(name = "wind10m_max")
        public Integer maxWind;
    }

    public List<Entry> dataseries;
}
