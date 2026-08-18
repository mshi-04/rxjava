package com.example.rxjava.di;

import com.example.rxjava.domain.repository.ForecastRepository;
import com.example.rxjava.fake.FakeForecastRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;

@Module
@TestInstallIn(components = SingletonComponent.class, replaces = RepositoryModule.class)
public abstract class FakeRepositoryModule {

    @Binds
    @Singleton
    abstract ForecastRepository bindForecastRepository(FakeForecastRepository repository);
}
