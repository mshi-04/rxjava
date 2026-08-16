package com.example.rxjava.di;

import com.example.rxjava.domain.repository.ForecastRepository;
import com.example.rxjava.domain.usecase.GetForecastUseCase;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public final class UseCaseModule {

    private UseCaseModule() {
    }

    @Provides
    static GetForecastUseCase provideGetForecastUseCase(ForecastRepository repository) {
        return new GetForecastUseCase(repository);
    }
}
