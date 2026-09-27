package com.example.rxjava.data.local;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.rxjava.data.repository.ForecastStore;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.model.Temperature;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;
import com.example.rxjava.domain.model.WindSpeed;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class RoomForecastStoreTest {
    private Context context;
    private String databaseName;
    private ForecastDatabase database;
    private RoomForecastStore store;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "forecast-test-" + UUID.randomUUID() + ".db";
        openDatabase();
    }

    private void openDatabase() {
        database = Room.databaseBuilder(context, ForecastDatabase.class, databaseName).build();
        store = new RoomForecastStore(database);
    }

    @After
    public void tearDown() {
        database.close();
        context.deleteDatabase(databaseName);
    }

    @Test
    public void read_afterReopening_restoresAllFieldsAndOrder() {
        // Arrange
        saveStored(Location.TOKYO, Arrays.asList(forecast(17), forecast(16)), 1234);
        // Act
        database.close();
        openDatabase();
        ForecastStore.Snapshot snapshot = readStored(Location.TOKYO);
        // Assert
        assertEquals(Long.valueOf(1234), snapshot.lastFetchedAt());
        assertEquals(2, snapshot.forecasts().size());
        assertEquals(17, snapshot.forecasts().get(0).getDate().getDay());
        assertEquals(16, snapshot.forecasts().get(1).getDate().getDay());
        DailyForecast restored = snapshot.forecasts().get(0);
        assertEquals(2026, restored.getDate().getYear());
        assertEquals(9, restored.getDate().getMonth());
        assertEquals(WeatherCondition.CLEAR, restored.getWeather());
        assertEquals(30, restored.getTemperature().getMax().getCelsius());
        assertEquals(20, restored.getTemperature().getMin().getCelsius());
        assertEquals(3, restored.getWind().getLevel());
    }

    @Test
    public void save_replacement_removesPreviousRowsAndPreservesOtherLocation() {
        // Arrange
        Location other = Location.of(34, 135);
        saveStored(Location.TOKYO, Arrays.asList(forecast(16), forecast(17)), 100);
        saveStored(other, Collections.singletonList(forecast(18)), 200);
        // Act
        saveStored(Location.TOKYO, Collections.singletonList(forecast(19)), 300);
        // Assert
        ForecastStore.Snapshot tokyo = readStored(Location.TOKYO);
        assertEquals(1, tokyo.forecasts().size());
        assertEquals(19, tokyo.forecasts().get(0).getDate().getDay());
        assertEquals(Long.valueOf(300), tokyo.lastFetchedAt());
        assertEquals(18, readStored(other).forecasts().get(0).getDate().getDay());
        assertEquals(Long.valueOf(200), readStored(other).lastFetchedAt());
    }

    @Test
    public void save_emptyList_distinguishesFetchedFromMissing() {
        // Arrange
        saveStored(Location.TOKYO, Collections.singletonList(forecast(16)), 100);
        // Act
        saveStored(Location.TOKYO, Collections.emptyList(), 200);
        // Assert
        assertTrue(readStored(Location.TOKYO).forecasts().isEmpty());
        assertEquals(Long.valueOf(200), readStored(Location.TOKYO).lastFetchedAt());
        assertNull(readStored(Location.of(34, 135)).lastFetchedAt());
    }

    @Test
    public void save_timestampInsertFailure_rollsBackForecastReplacement() {
        // Arrange
        saveStored(Location.TOKYO, Collections.singletonList(forecast(16)), 100);
        database.getOpenHelper().getWritableDatabase().execSQL(
                "CREATE TRIGGER reject_timestamp BEFORE INSERT ON last_fetched_at "
                        + "BEGIN SELECT RAISE(ABORT, 'forced timestamp failure'); END");
        // Act
        io.reactivex.rxjava3.observers.TestObserver<Void> observer =
                store.save(Location.TOKYO, Collections.singletonList(forecast(17)), 200).test();
        observer.awaitDone(5, java.util.concurrent.TimeUnit.SECONDS);
        ForecastStore.Snapshot snapshot = readStored(Location.TOKYO);
        // Assert
        observer.assertError(android.database.sqlite.SQLiteException.class).assertNotComplete();
        assertEquals(1, snapshot.forecasts().size());
        assertEquals(16, snapshot.forecasts().get(0).getDate().getDay());
        assertEquals(Long.valueOf(100), snapshot.lastFetchedAt());
    }



    @Test
    public void readAndSave_beforeSubscription_doesNotOpenDatabase() {
        // Arrange
        io.reactivex.rxjava3.core.Single<ForecastStore.Snapshot> read = store.read(Location.TOKYO);
        io.reactivex.rxjava3.core.Completable save = store.save(Location.TOKYO,
                Collections.singletonList(forecast(16)), 100);
        // Act
        boolean openBeforeSubscription = database.isOpen();
        save.test().awaitDone(5, java.util.concurrent.TimeUnit.SECONDS).assertComplete().assertNoErrors();
        io.reactivex.rxjava3.observers.TestObserver<ForecastStore.Snapshot> observer = read.test();
        observer.awaitDone(5, java.util.concurrent.TimeUnit.SECONDS).assertComplete().assertNoErrors();
        // Assert
        assertFalse(openBeforeSubscription);
        assertEquals(Long.valueOf(100), observer.values().get(0).lastFetchedAt());
        assertEquals(16, observer.values().get(0).forecasts().get(0).getDate().getDay());
    }

    @Test
    public void read_unfetchedLocation_emitsEmptySnapshot() {
        // Arrange
        io.reactivex.rxjava3.core.Single<ForecastStore.Snapshot> read = store.read(Location.TOKYO);
        // Act
        io.reactivex.rxjava3.observers.TestObserver<ForecastStore.Snapshot> observer = read.test();
        observer.awaitDone(5, java.util.concurrent.TimeUnit.SECONDS).assertComplete().assertNoErrors();
        // Assert
        assertNull(observer.values().get(0).lastFetchedAt());
        assertTrue(observer.values().get(0).forecasts().isEmpty());
    }

    private ForecastStore.Snapshot readStored(Location location) {
        io.reactivex.rxjava3.observers.TestObserver<ForecastStore.Snapshot> observer = store.read(location).test();
        observer.awaitDone(5, java.util.concurrent.TimeUnit.SECONDS).assertComplete().assertNoErrors();
        return observer.values().get(0);
    }

    private void saveStored(Location location, List<DailyForecast> forecasts, long timestamp) {
        store.save(location, forecasts, timestamp).test()
                .awaitDone(5, java.util.concurrent.TimeUnit.SECONDS).assertComplete().assertNoErrors();
    }

    private static DailyForecast forecast(int day) {
        return new DailyForecast(ForecastDate.of(2026, 9, day), WeatherCondition.CLEAR,
                TemperatureRange.of(Temperature.ofCelsius(30), Temperature.ofCelsius(20)),
                WindSpeed.ofLevel(3));
    }
}
