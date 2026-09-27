package com.example.rxjava.di;

import com.example.rxjava.data.local.ForecastStoreImpl;
import com.example.rxjava.data.datasource.ForecastDataSourceImpl;
import com.example.rxjava.data.repository.CachingForecastRepository;
import com.example.rxjava.data.datasource.ForecastDataSource;
import com.example.rxjava.data.repository.ForecastStore;
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
    abstract ForecastRepository bindForecastRepository(CachingForecastRepository repository);

    @Binds
    abstract ForecastDataSource bindForecastDataSource(ForecastDataSourceImpl dataSource);

    @Binds
    @Singleton
    abstract ForecastStore bindForecastStore(ForecastStoreImpl store);

    @Provides
    static TimeProvider provideTimeProvider() {
        return System::currentTimeMillis;
    }
}
