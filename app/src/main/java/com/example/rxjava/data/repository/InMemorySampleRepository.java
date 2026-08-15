package com.example.rxjava.data.repository;

import com.example.rxjava.data.catalog.SampleDefinition;
import com.example.rxjava.domain.model.Sample;
import com.example.rxjava.domain.model.SampleId;
import com.example.rxjava.domain.model.SampleLogLine;
import com.example.rxjava.domain.repository.SampleRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;

public final class InMemorySampleRepository implements SampleRepository {

    private final Map<SampleId, SampleDefinition> definitions = new LinkedHashMap<>();

    public InMemorySampleRepository(List<SampleDefinition> definitions) {
        for (SampleDefinition definition : definitions) {
            this.definitions.put(definition.getId(), definition);
        }
    }

    @Override
    public List<Sample> findAll() {
        List<Sample> samples = new ArrayList<>(definitions.size());
        for (SampleDefinition definition : definitions.values()) {
            samples.add(definition.toSample());
        }
        return Collections.unmodifiableList(samples);
    }

    @Override
    public Sample findById(SampleId id) {
        return requireDefinition(id).toSample();
    }

    @Override
    public Observable<SampleLogLine> run(SampleId id) {
        SampleDefinition definition = requireDefinition(id);
        // defer で包み、実行ごとに状態を持つサンプルが購読のたびに作り直されるようにする
        return Observable.defer(definition::execute);
    }

    private SampleDefinition requireDefinition(SampleId id) {
        SampleDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("未登録のサンプル: " + id);
        }
        return definition;
    }
}
