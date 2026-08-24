package com.example.rxjava.domain.model;

public final class Location {

    public static final Location TOKYO = of(35.690, 139.692);

    private final double latitude;
    private final double longitude;

    private Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Location of(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("緯度が範囲外: " + latitude);
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("経度が範囲外: " + longitude);
        }
        return new Location(latitude, longitude);
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
