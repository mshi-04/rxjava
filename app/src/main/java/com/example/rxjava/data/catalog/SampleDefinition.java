package com.example.rxjava.data.catalog;

import com.example.rxjava.domain.model.Sample;
import com.example.rxjava.domain.model.SampleId;
import com.example.rxjava.domain.model.SampleLogLine;

import io.reactivex.rxjava3.core.Observable;

public final class SampleDefinition {

    public interface Body {

        Observable<SampleLogLine> execute();
    }

    private final SampleId id;
    private final String title;
    private final String description;
    private final Body body;

    public SampleDefinition(SampleId id, String title, String description, Body body) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.body = body;
    }

    public SampleId getId() {
        return id;
    }

    public Sample toSample() {
        return new Sample(id, title, description);
    }

    public Observable<SampleLogLine> execute() {
        return body.execute();
    }
}
