package com.example.rxjava.domain.model;

public final class ForecastDate {

    private final int year;
    private final int month;
    private final int day;

    private ForecastDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static ForecastDate of(int year, int month, int day) {
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
