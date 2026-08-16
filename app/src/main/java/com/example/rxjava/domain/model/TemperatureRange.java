package com.example.rxjava.domain.model;

import java.util.Objects;

public final class TemperatureRange {

    private final Temperature max;
    private final Temperature min;

    private TemperatureRange(Temperature max, Temperature min) {
        this.max = max;
        this.min = min;
    }

    public static TemperatureRange of(Temperature max, Temperature min) {
        Objects.requireNonNull(max);
        Objects.requireNonNull(min);
        if (max.isColderThan(min)) {
            throw new IllegalArgumentException(
                    "最高気温が最低気温を下回る: " + max.getCelsius() + " < " + min.getCelsius());
        }
        return new TemperatureRange(max, min);
    }

    public Temperature getMax() {
        return max;
    }

    public Temperature getMin() {
        return min;
    }
}
