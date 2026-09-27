package com.example.rxjava.di;

import com.example.rxjava.data.datasource.ForecastLocalDataSourceImpl;
import com.example.rxjava.data.datasource.ForecastRemoteDataSourceImpl;
import com.example.rxjava.data.repository.ForecastRepositoryImpl;
import com.example.rxjava.data.datasource.ForecastRemoteDataSource;
import com.example.rxjava.data.datasource.ForecastLocalDataSource;
import com.example.rxjava.data.repository.TimeProvider;
import com.example.rxjava.domain.repository.ForecastRepository;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract ForecastRepository bindForecastRepository(ForecastRepositoryImpl repository);

    @Binds
    abstract ForecastRemoteDataSource bindForecastDataSource(ForecastRemoteDataSourceImpl dataSource);

    @Binds
    @Singleton
    abstract ForecastLocalDataSource bindForecastStore(ForecastLocalDataSourceImpl store);

    @Provides
    static TimeProvider provideTimeProvider() {
        return System::currentTimeMillis;
    }
}
