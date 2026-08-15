package com.example.rxjava.domain.usecase;

import com.example.rxjava.domain.model.SampleId;
import com.example.rxjava.domain.model.SampleLogLine;
import com.example.rxjava.domain.repository.SampleRepository;

import io.reactivex.rxjava3.core.Observable;

public final class RunSampleUseCase {

    private final SampleRepository repository;

    public RunSampleUseCase(SampleRepository repository) {
        this.repository = repository;
    }

    public Observable<SampleLogLine> invoke(SampleId id) {
        return repository.run(id);
    }
}
