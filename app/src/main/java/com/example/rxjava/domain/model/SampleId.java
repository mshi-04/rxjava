package com.example.rxjava.domain.model;

import java.util.Objects;

public final class SampleId {

    private final String value;

    private SampleId(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static SampleId of(String value) {
        return new SampleId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SampleId)) {
            return false;
        }
        return value.equals(((SampleId) other).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
