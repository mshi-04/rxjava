package com.example.rxjava.di;

import com.example.rxjava.data.catalog.SampleCatalog;
import com.example.rxjava.data.repository.InMemorySampleRepository;
import com.example.rxjava.domain.repository.SampleRepository;
import com.example.rxjava.domain.usecase.GetSampleListUseCase;
import com.example.rxjava.domain.usecase.GetSampleUseCase;
import com.example.rxjava.domain.usecase.RunSampleUseCase;

public final class ServiceLocator {

    private static volatile ServiceLocator instance;

    private final GetSampleListUseCase getSampleListUseCase;
    private final GetSampleUseCase getSampleUseCase;
    private final RunSampleUseCase runSampleUseCase;

    private ServiceLocator() {
        SampleRepository repository = new InMemorySampleRepository(SampleCatalog.definitions());
        getSampleListUseCase = new GetSampleListUseCase(repository);
        getSampleUseCase = new GetSampleUseCase(repository);
        runSampleUseCase = new RunSampleUseCase(repository);
    }

    public static ServiceLocator getInstance() {
        if (instance == null) {
            synchronized (ServiceLocator.class) {
                if (instance == null) {
                    instance = new ServiceLocator();
                }
            }
        }
        return instance;
    }

    public GetSampleListUseCase getSampleListUseCase() {
        return getSampleListUseCase;
    }

    public GetSampleUseCase getSampleUseCase() {
        return getSampleUseCase;
    }

    public RunSampleUseCase getRunSampleUseCase() {
        return runSampleUseCase;
    }
}
