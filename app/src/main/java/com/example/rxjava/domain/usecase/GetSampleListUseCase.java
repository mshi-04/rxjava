package com.example.rxjava.domain.usecase;

import com.example.rxjava.domain.model.Sample;
import com.example.rxjava.domain.repository.SampleRepository;

import java.util.List;

public final class GetSampleListUseCase {

    private final SampleRepository repository;

    public GetSampleListUseCase(SampleRepository repository) {
        this.repository = repository;
    }

    public List<Sample> invoke() {
        return repository.findAll();
    }
}
