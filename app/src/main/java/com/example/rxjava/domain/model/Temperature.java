package com.example.rxjava.domain.model;

public final class Temperature {

    private static final int MIN_CELSIUS = -100;
    private static final int MAX_CELSIUS = 100;

    private final int celsius;

    private Temperature(int celsius) {
        this.celsius = celsius;
    }

    public static Temperature ofCelsius(int celsius) {
        if (celsius < MIN_CELSIUS || celsius > MAX_CELSIUS) {
            throw new IllegalArgumentException("気温が範囲外: " + celsius);
        }
        return new Temperature(celsius);
    }

    public int getCelsius() {
        return celsius;
    }

    public boolean isColderThan(Temperature other) {
        return celsius < other.celsius;
    }
}
