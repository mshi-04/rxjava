package com.example.rxjava.data.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.rxjava.data.datasource.ForecastLocalDataSource;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.model.Temperature;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;
import com.example.rxjava.domain.model.WindSpeed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.subjects.MaybeSubject;

class ForecastRepositoryImplTest {

    private static final long DAY_MILLIS = TimeUnit.HOURS.toMillis(24);

    private final FakeForecastRemoteDataSource dataSource = new FakeForecastRemoteDataSource();
    private final FakeForecastLocalDataSource store = new FakeForecastLocalDataSource();
    private long now = 100 * DAY_MILLIS;
    private ForecastRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ForecastRepositoryImpl(dataSource, store, () -> now);
    }

    @Test
    void find_beforeSubscription_doesNotReadOrFetch() {
        // Arrange
        Single<List<DailyForecast>> stream = repository.find(Location.TOKYO);

        // Act
        int readsBeforeSubscription = store.reads;
        int fetchesBeforeSubscription = dataSource.fetches;
        stream.test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Assert
        assertAll(
                () -> assertEquals(0, readsBeforeSubscription),
                () -> assertEquals(0, fetchesBeforeSubscription),
                () -> assertEquals(1, store.reads),
                () -> assertEquals(1, dataSource.fetches));
    }

    @Test
    void find_notFetchedYet_fetchesAndSaves() {
        // Arrange
        dataSource.response = Single.just(List.of(forecastOnAugust(16)));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> observer.assertComplete().assertValue(forecasts -> dayOfFirst(forecasts) == 16),
                () -> assertEquals(1, dataSource.fetches),
                () -> assertEquals(now, store.stored(Location.TOKYO).fetchedAtMillis()),
                () -> assertEquals(16, dayOfFirst(store.stored(Location.TOKYO).forecasts())));
    }

    @Test
    void find_freshCache_returnsStoredForecastWithoutFetch() {
        // Arrange
        store.put(Location.TOKYO, new ForecastLocalDataSource.Snapshot(Collections.emptyList(), now - (DAY_MILLIS - 1)));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> observer.assertComplete().assertValue(List::isEmpty),
                () -> assertEquals(0, dataSource.fetches));
    }

    @Test
    void find_exactly24HoursOld_fetchesAndSaves() {
        // Arrange
        store.put(Location.TOKYO, new ForecastLocalDataSource.Snapshot(Collections.emptyList(), now - DAY_MILLIS));
        dataSource.response = Single.just(List.of(forecastOnAugust(16)));

        // Act
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Assert
        assertAll(
                () -> assertEquals(1, dataSource.fetches),
                () -> assertEquals(now, store.stored(Location.TOKYO).fetchedAtMillis()),
                () -> assertEquals(16, dayOfFirst(store.stored(Location.TOKYO).forecasts())));
    }

    @Test
    void find_futureTimestamp_fetchesAgain() {
        // Arrange
        store.put(Location.TOKYO, new ForecastLocalDataSource.Snapshot(Collections.emptyList(), now + 1));

        // Act
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Assert
        assertEquals(1, dataSource.fetches);
    }

    @Test
    void find_expiredCacheAndFetchFailure_preservesStoredSnapshot() {
        // Arrange
        ForecastLocalDataSource.Snapshot previous =
                new ForecastLocalDataSource.Snapshot(List.of(forecastOnAugust(15)), now - (DAY_MILLIS + 1));
        store.put(Location.TOKYO, previous);
        IOException failure = new IOException("network failed");
        dataSource.response = Single.error(failure);

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> observer.assertError(failure).assertNoValues(),
                () -> assertSame(previous, store.stored(Location.TOKYO)),
                () -> assertEquals(0, store.saves));
    }

    @Test
    void find_expiredCacheAndMappingFailure_preservesPreviousForecastAndTimestamp() {
        // Arrange
        dataSource.response = Single.just(List.of(forecastOnAugust(16)));
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        long previous = now;
        now += DAY_MILLIS;
        IllegalStateException failure = new IllegalStateException("dataseries を含まないレスポンス");
        dataSource.response = Single.error(failure);

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> observer.assertError(failure).assertNoValues(),
                () -> assertEquals(previous, store.stored(Location.TOKYO).fetchedAtMillis()),
                () -> assertEquals(16, dayOfFirst(store.stored(Location.TOKYO).forecasts())));
    }

    @Test
    void find_saveFailure_emitsErrorAndPreservesCache() {
        // Arrange
        ForecastLocalDataSource.Snapshot previous =
                new ForecastLocalDataSource.Snapshot(Collections.emptyList(), now - DAY_MILLIS);
        store.put(Location.TOKYO, previous);
        store.saveFailure = new IllegalStateException("save failed");

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> observer.assertError(store.saveFailure).assertNoValues(),
                () -> assertSame(previous, store.stored(Location.TOKYO)));
    }

    @Test
    void find_readFailure_emitsErrorWithoutFetch() {
        // Arrange
        store.readFailure = new IllegalStateException("read failed");

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> observer.assertError(store.readFailure).assertNoValues(),
                () -> assertEquals(0, dataSource.fetches));
    }

    @Test
    void find_sameStreamSubscribedAgain_rereadsAndFetchesOnceWhileFresh() {
        // Arrange
        Single<List<DailyForecast>> stream = repository.find(Location.TOKYO);

        // Act
        stream.test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        stream.test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Assert
        assertAll(
                () -> assertEquals(2, store.reads),
                () -> assertEquals(1, dataSource.fetches));
    }

    @Test
    void find_otherLocation_doesNotUseTokyoCache() {
        // Arrange
        store.put(Location.TOKYO, new ForecastLocalDataSource.Snapshot(Collections.emptyList(), now));
        Location other = Location.of(34, 135);
        dataSource.response = Single.just(List.of(forecastOnAugust(17)));

        // Act
        repository.find(other).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Assert
        assertAll(
                () -> assertEquals(1, dataSource.fetches),
                () -> assertTrue(store.stored(Location.TOKYO).forecasts().isEmpty()),
                () -> assertEquals(17, dayOfFirst(store.stored(other).forecasts())));
    }

    @Test
    void find_emptyResponse_savesTimestampAndAvoidsNextFetch() {
        // Arrange
        dataSource.response = Single.just(Collections.emptyList());
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> observer.assertComplete().assertValue(List::isEmpty),
                () -> assertEquals(now, store.stored(Location.TOKYO).fetchedAtMillis()),
                () -> assertEquals(1, dataSource.fetches));
    }

    @Test
    void find_clockAdvancesWhileFetching_savesTimeTakenImmediatelyBeforeSave() {
        // Arrange
        long fetchDuration = TimeUnit.SECONDS.toMillis(5);
        long start = now;
        dataSource.response = Single.fromCallable(() -> {
            now += fetchDuration;
            return Collections.emptyList();
        });

        // Act
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();

        // Assert
        assertEquals(start + fetchDuration, store.stored(Location.TOKYO).fetchedAtMillis());
    }

    @Test
    void find_freshNonemptyCache_returnsStoredForecast() {
        // Arrange
        dataSource.response = Single.just(List.of(forecastOnAugust(16)));
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        List<DailyForecast> saved = store.stored(Location.TOKYO).forecasts();
        now += DAY_MILLIS - 1;

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> observer.assertComplete().assertValue(saved),
                () -> assertEquals(1, dataSource.fetches));
    }

    @Test
    void find_expiredNonemptyCache_refreshesForecasts() {
        // Arrange
        dataSource.response = Single.just(List.of(forecastOnAugust(16)));
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        now += DAY_MILLIS + 1;
        dataSource.response = Single.just(List.of(forecastOnAugust(17)));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> observer.assertComplete().assertValue(forecasts -> dayOfFirst(forecasts) == 17),
                () -> assertEquals(2, dataSource.fetches),
                () -> assertEquals(17, dayOfFirst(store.stored(Location.TOKYO).forecasts())),
                () -> assertEquals(now, store.stored(Location.TOKYO).fetchedAtMillis()));
    }

    @Test
    void find_savePending_emitsResultOnlyAfterSaveCompletes() {
        // Arrange
        CompletableSubject gate = CompletableSubject.create();
        store.saveGate = gate;
        dataSource.response = Single.just(List.of(forecastOnAugust(16)));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        int valuesBeforeSave = observer.values().size();
        boolean saving = gate.hasObservers();
        gate.onComplete();
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> assertEquals(0, valuesBeforeSave),
                () -> assertTrue(saving),
                () -> observer.assertComplete().assertValue(forecasts -> dayOfFirst(forecasts) == 16),
                () -> assertEquals(1, store.saves));
    }

    @Test
    void find_pendingSaveFails_emitsErrorWithoutValue() {
        // Arrange
        CompletableSubject gate = CompletableSubject.create();
        store.saveGate = gate;
        IllegalStateException failure = new IllegalStateException("delayed save failed");

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        gate.onError(failure);
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> observer.assertError(failure).assertNoValues(),
                () -> assertEquals(0, store.saves));
    }

    @Test
    void find_disposedWhileSaving_disposesSaveAndPublishesNothing() {
        // Arrange
        CompletableSubject gate = CompletableSubject.create();
        store.saveGate = gate;

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        boolean savingBeforeDispose = gate.hasObservers();
        observer.dispose();
        boolean savingAfterDispose = gate.hasObservers();
        gate.onComplete();

        // Assert
        assertAll(
                () -> assertTrue(savingBeforeDispose),
                () -> assertFalse(savingAfterDispose),
                () -> observer.assertNoValues().assertNoErrors().assertNotComplete(),
                () -> assertEquals(0, store.saves),
                () -> assertNull(store.stored(Location.TOKYO)));
    }

    @Test
    void find_readPending_judgesExpirationWhenReadCompletes() {
        // Arrange
        MaybeSubject<ForecastLocalDataSource.Snapshot> read = MaybeSubject.create();
        store.readOverride = read;
        long fetchedAtMillis = now;

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        int fetchesBeforeRead = dataSource.fetches;
        now += DAY_MILLIS;
        read.onSuccess(new ForecastLocalDataSource.Snapshot(Collections.emptyList(), fetchedAtMillis));
        observer.awaitDone(5, TimeUnit.SECONDS);

        // Assert
        assertAll(
                () -> assertEquals(0, fetchesBeforeRead),
                () -> observer.assertComplete(),
                () -> assertEquals(1, dataSource.fetches));
    }

    private static DailyForecast forecastOnAugust(int day) {
        return new DailyForecast(
                ForecastDate.of(2026, 8, day),
                WeatherCondition.CLEAR,
                TemperatureRange.of(Temperature.ofCelsius(30), Temperature.ofCelsius(20)),
                WindSpeed.ofLevel(2));
    }

    private static int dayOfFirst(List<DailyForecast> forecasts) {
        return forecasts.get(0).getDate().getDay();
    }
}
