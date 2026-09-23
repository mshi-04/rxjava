package com.example.rxjava.di;

import com.example.rxjava.presentation.util.UiSchedulers;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public final class SchedulerModule {

    private SchedulerModule() {
    }

    @Provides
    static UiSchedulers provideUiSchedulers() {
        return UiSchedulers.android();
    }
}
