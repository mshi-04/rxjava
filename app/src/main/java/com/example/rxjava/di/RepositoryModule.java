package com.example.rxjava.di;

import com.example.rxjava.data.remote.ForecastApi;
import com.example.rxjava.data.repository.RemoteForecastRepository;
import com.example.rxjava.domain.repository.ForecastRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public final class RepositoryModule {

    private RepositoryModule() {
    }

    @Provides
    @Singleton
    static ForecastRepository provideForecastRepository(ForecastApi api) {
        return new RemoteForecastRepository(api);
    }
}
