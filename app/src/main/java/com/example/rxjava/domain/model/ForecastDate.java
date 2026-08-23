package com.example.rxjava.domain.model;

public final class ForecastDate {

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2999;

    private final int year;
    private final int month;
    private final int day;

    private ForecastDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static ForecastDate of(int year, int month, int day) {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException("年が範囲外: " + year);
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("月が範囲外: " + month);
        }
        if (day < 1 || day > 31) {
            throw new IllegalArgumentException("日が範囲外: " + day);
        }
        return new ForecastDate(year, month, day);
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }
}
