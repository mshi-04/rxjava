package com.example.rxjava.data.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteException;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.rxjava.data.db.ForecastDatabase;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;

@RunWith(AndroidJUnit4.class)
public class ForecastLocalDataSourceImplTest {

    private static final Location OTHER_LOCATION = Location.of(34, 135);

    private final AtomicReference<Disposable> disposeOnForecastDelete = new AtomicReference<>();

    private Context context;
    private String databaseName;
    private ExecutorService queryExecutor;
    private ForecastDatabase database;
    private ForecastLocalDataSourceImpl store;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseName = "forecast-test-" + UUID.randomUUID() + ".db";
        queryExecutor = Executors.newSingleThreadExecutor();
        openDatabase(databaseBuilder());
    }

    @After
    public void tearDown() {
        database.close();
        context.deleteDatabase(databaseName);
        queryExecutor.shutdownNow();
    }

    @Test
    public void read_afterReopening_restoresAllFieldsAndOrder() {
        // Arrange
        saveStored(Location.TOKYO, Arrays.asList(forecast(17), forecast(16)), 1234);

        // Act
        database.close();
        openDatabase(databaseBuilder());
        ForecastLocalDataSource.Snapshot snapshot = readStored(Location.TOKYO);

        // Assert
        DailyForecast restored = snapshot.forecasts().get(0);
        assertEquals(1234, snapshot.fetchedAtMillis());
        assertEquals(2, snapshot.forecasts().size());
        assertEquals(17, snapshot.forecasts().get(0).getDate().getDay());
        assertEquals(16, snapshot.forecasts().get(1).getDate().getDay());
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
        saveStored(Location.TOKYO, Arrays.asList(forecast(16), forecast(17)), 100);
        saveStored(OTHER_LOCATION, Collections.singletonList(forecast(18)), 200);

        // Act
        saveStored(Location.TOKYO, Collections.singletonList(forecast(19)), 300);

        // Assert
        ForecastLocalDataSource.Snapshot tokyo = readStored(Location.TOKYO);
        ForecastLocalDataSource.Snapshot other = readStored(OTHER_LOCATION);
        assertEquals(1, tokyo.forecasts().size());
        assertEquals(19, tokyo.forecasts().get(0).getDate().getDay());
        assertEquals(300, tokyo.fetchedAtMillis());
        assertEquals(18, other.forecasts().get(0).getDate().getDay());
        assertEquals(200, other.fetchedAtMillis());
    }

    @Test
    public void save_emptyList_distinguishesFetchedFromUnfetched() {
        // Arrange
        saveStored(Location.TOKYO, Collections.singletonList(forecast(16)), 100);

        // Act
        saveStored(Location.TOKYO, Collections.emptyList(), 200);

        // Assert
        ForecastLocalDataSource.Snapshot tokyo = readStored(Location.TOKYO);
        assertTrue(tokyo.forecasts().isEmpty());
        assertEquals(200, tokyo.fetchedAtMillis());
        assertUnfetched(OTHER_LOCATION);
    }

    @Test
    public void read_unfetchedLocation_completesWithoutSnapshot() {
        // Arrange
        Maybe<ForecastLocalDataSource.Snapshot> read = store.read(Location.TOKYO);

        // Act
        TestObserver<ForecastLocalDataSource.Snapshot> observer = read.test();

        // Assert
        observer.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors().assertNoValues();
    }

    @Test
    public void save_timestampInsertFailure_rollsBackForecastReplacement() {
        // Arrange
        saveStored(Location.TOKYO, Collections.singletonList(forecast(16)), 100);
        database.getOpenHelper().getWritableDatabase().execSQL(
                "CREATE TRIGGER reject_timestamp BEFORE INSERT ON last_fetched_at "
                        + "BEGIN SELECT RAISE(ABORT, 'forced timestamp failure'); END");

        // Act
        TestObserver<Void> observer =
                store.save(Location.TOKYO, Collections.singletonList(forecast(17)), 200).test();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        ForecastLocalDataSource.Snapshot snapshot = readStored(Location.TOKYO);
        observer.assertError(SQLiteException.class).assertNotComplete();
        assertEquals(1, snapshot.forecasts().size());
        assertEquals(16, snapshot.forecasts().get(0).getDate().getDay());
        assertEquals(100, snapshot.fetchedAtMillis());
    }

    @Test
    public void readAndSave_beforeSubscription_doesNotOpenDatabase() {
        // Arrange
        Maybe<ForecastLocalDataSource.Snapshot> read = store.read(Location.TOKYO);
        Completable save = store.save(Location.TOKYO, Collections.singletonList(forecast(16)), 100);

        // Act
        boolean openBeforeSubscription = database.isOpen();
        save.test().awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();
        TestObserver<ForecastLocalDataSource.Snapshot> observer = read.test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();

        // Assert
        assertFalse(openBeforeSubscription);
        assertEquals(100, observer.values().get(0).fetchedAtMillis());
        assertEquals(16, observer.values().get(0).forecasts().get(0).getDate().getDay());
    }

    @Test
    public void save_callerMutatesListAfterCall_savesOriginalContents() {
        // Arrange
        List<DailyForecast> forecasts = new ArrayList<>(Collections.singletonList(forecast(16)));
        Completable save = store.save(Location.TOKYO, forecasts, 100);

        // Act
        forecasts.set(0, forecast(20));
        forecasts.add(forecast(21));
        save.test().awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();

        // Assert
        ForecastLocalDataSource.Snapshot snapshot = readStored(Location.TOKYO);
        assertEquals(1, snapshot.forecasts().size());
        assertEquals(16, snapshot.forecasts().get(0).getDate().getDay());
    }

    @Test
    public void save_disposedBeforeTransactionStarts_writesNothing() {
        // Arrange
        // 保留した実行は transaction executor に積むだけにし、読み取りは別の query executor で行う
        Queue<Runnable> pendingTransactions = new ConcurrentLinkedQueue<>();
        database.close();
        openDatabase(databaseBuilder()
                .setQueryExecutor(queryExecutor)
                .setTransactionExecutor(pendingTransactions::add));
        TestObserver<Void> observer =
                store.save(Location.TOKYO, Collections.singletonList(forecast(16)), 100).test();
        int pendingBeforeDispose = pendingTransactions.size();

        // Act
        observer.dispose();
        runAll(pendingTransactions);

        // Assert
        assertEquals(1, pendingBeforeDispose);
        observer.assertNoErrors().assertNotComplete();
        assertUnfetched(Location.TOKYO);
    }

    @Test
    public void save_disposedDuringTransaction_writesBothForecastsAndTimestampWithoutNotifying() {
        // Arrange
        database.close();
        openDatabase(databaseBuilder().setQueryCallback(this::disposeOnForecastDelete, Runnable::run));
        saveStored(Location.TOKYO, Collections.singletonList(forecast(16)), 100);
        TestObserver<Void> observer = new TestObserver<>();
        disposeOnForecastDelete.set(observer);

        // Act
        store.save(Location.TOKYO, Collections.singletonList(forecast(17)), 200).subscribe(observer);
        awaitQueuedTransactions();

        // Assert
        ForecastLocalDataSource.Snapshot snapshot = readStored(Location.TOKYO);
        assertTrue(observer.isDisposed());
        observer.assertNoErrors().assertNotComplete();
        assertEquals(1, snapshot.forecasts().size());
        assertEquals(17, snapshot.forecasts().get(0).getDate().getDay());
        assertEquals(200, snapshot.fetchedAtMillis());
    }

    private RoomDatabase.Builder<ForecastDatabase> databaseBuilder() {
        return Room.databaseBuilder(context, ForecastDatabase.class, databaseName);
    }

    private void openDatabase(RoomDatabase.Builder<ForecastDatabase> builder) {
        database = builder.build();
        store = new ForecastLocalDataSourceImpl(database);
    }

    private void disposeOnForecastDelete(String sql, List<?> bindArgs) {
        Disposable disposable = disposeOnForecastDelete.get();
        if (disposable != null && sql.startsWith("DELETE FROM forecasts")) {
            disposable.dispose();
        }
    }

    private void awaitQueuedTransactions() {
        // transaction executor は直列なので、後から積んだ処理はそれまでのトランザクションの終了後に動く
        CountDownLatch drained = new CountDownLatch(1);
        database.getTransactionExecutor().execute(drained::countDown);
        try {
            assertTrue(drained.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void runAll(Queue<Runnable> tasks) {
        Runnable task;
        while ((task = tasks.poll()) != null) {
            task.run();
        }
    }

    private ForecastLocalDataSource.Snapshot readStored(Location location) {
        TestObserver<ForecastLocalDataSource.Snapshot> observer = store.read(location).test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors().assertValueCount(1);
        return observer.values().get(0);
    }

    private void assertUnfetched(Location location) {
        store.read(location).test()
                .awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors().assertNoValues();
    }

    private void saveStored(Location location, List<DailyForecast> forecasts, long fetchedAtMillis) {
        store.save(location, forecasts, fetchedAtMillis).test()
                .awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();
    }

    private static DailyForecast forecast(int day) {
        return new DailyForecast(ForecastDate.of(2026, 9, day), WeatherCondition.CLEAR,
                TemperatureRange.of(Temperature.ofCelsius(30), Temperature.ofCelsius(20)),
                WindSpeed.ofLevel(3));
    }
}
