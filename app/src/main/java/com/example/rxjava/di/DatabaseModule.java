package com.example.rxjava.di;

import android.content.Context;

import androidx.room.Room;

import com.example.rxjava.data.local.ForecastDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public final class DatabaseModule {

    private static final String DATABASE_NAME = "forecast.db";

    private DatabaseModule() {
    }

    @Provides
    @Singleton
    static ForecastDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, ForecastDatabase.class, DATABASE_NAME).build();
    }
}
