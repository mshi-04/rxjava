package com.example.rxjava.domain.model;

public final class WindSpeed {

    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 12;

    private final int level;

    private WindSpeed(int level) {
        this.level = level;
    }

    public static WindSpeed ofLevel(int level) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("風速階級が範囲外: " + level);
        }
        return new WindSpeed(level);
    }

    public int getLevel() {
        return level;
    }
}
