package com.example.rxjava.di;

import com.example.rxjava.data.repository.RemoteForecastRepository;
import com.example.rxjava.domain.repository.ForecastRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract ForecastRepository bindForecastRepository(RemoteForecastRepository repository);
}
