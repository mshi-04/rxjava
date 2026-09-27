package com.example.rxjava.di;

import android.content.Context;
import androidx.room.Room;
import com.example.rxjava.data.local.ForecastDatabase;
import com.example.rxjava.data.local.RoomForecastStore;
import com.example.rxjava.data.repository.ForecastStore;
import com.example.rxjava.data.repository.TimeProvider;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public final class DatabaseModule {
    private DatabaseModule() {}

    @Provides
    @Singleton
    static ForecastDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, ForecastDatabase.class, "forecast.db").build();
    }

    @Provides
    @Singleton
    static ForecastStore provideForecastStore(RoomForecastStore store) {
        return store;
    }

    @Provides
    static TimeProvider provideTimeProvider() {
        return System::currentTimeMillis;
    }
}
