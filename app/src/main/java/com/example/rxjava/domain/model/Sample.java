package com.example.rxjava.domain.model;

import java.util.Objects;

public final class Sample {

    private final SampleId id;
    private final String title;
    private final String description;

    public Sample(SampleId id, String title, String description) {
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.description = Objects.requireNonNull(description);
    }

    public SampleId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
