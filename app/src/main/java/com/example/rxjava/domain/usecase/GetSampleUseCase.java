package com.example.rxjava.domain.usecase;

import com.example.rxjava.domain.model.Sample;
import com.example.rxjava.domain.model.SampleId;
import com.example.rxjava.domain.repository.SampleRepository;

public final class GetSampleUseCase {

    private final SampleRepository repository;

    public GetSampleUseCase(SampleRepository repository) {
        this.repository = repository;
    }

    public Sample invoke(SampleId id) {
        return repository.findById(id);
    }
}
