package com.example.rxjava.data.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.rxjava.data.remote.ForecastApi;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.model.WeatherCondition;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.JsonEncodingException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import okhttp3.OkHttpClient;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import retrofit2.HttpException;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

class RemoteForecastRepositoryTest {

    private static final String FIRST = "{\"date\":20260816,\"weather\":\"lightrain\","
            + "\"temp2m\":{\"max\":26,\"min\":25},\"wind10m_max\":2}";
    private static final String SECOND = "{\"date\":20260817,\"weather\":\"clear\","
            + "\"temp2m\":{\"max\":32,\"min\":24},\"wind10m_max\":3}";

    private MockWebServer server;
    private RemoteForecastRepository repository;
    private long now = 100_000_000L;
    private FakeForecastStore store;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(5, TimeUnit.SECONDS)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(new Moshi.Builder().build()))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build();
        store = new FakeForecastStore();
        repository = new RemoteForecastRepository(retrofit.create(ForecastApi.class), store, () -> now);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    @Test
    void find_tokyo_requestsFixedPathAndCoordinates() throws InterruptedException {
        // Arrange
        server.enqueue(json("{\"dataseries\":[]}"));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();
        RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);

        // Assert
        assertNotNull(request);
        assertAll(
                () -> assertEquals("GET", request.getMethod()),
                () -> assertEquals("/bin/api.pl", request.getUrl().encodedPath()),
                () -> assertEquals("civillight", request.getUrl().queryParameter("product")),
                () -> assertEquals("json", request.getUrl().queryParameter("output")),
                () -> assertEquals("35.69", request.getUrl().queryParameter("lat")),
                () -> assertEquals("139.692", request.getUrl().queryParameter("lon")),
                () -> assertEquals(4, request.getUrl().querySize()));
    }

    @Test
    void find_validEntries_mapsAllDomainFieldsInOrder() {
        // Arrange
        server.enqueue(json(series(FIRST + "," + SECOND)));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();
        List<DailyForecast> forecasts = observer.values().get(0);

        // Assert
        assertAll(
                () -> assertEquals(2, forecasts.size()),
                () -> assertEquals(2026, forecasts.get(0).getDate().getYear()),
                () -> assertEquals(8, forecasts.get(0).getDate().getMonth()),
                () -> assertEquals(16, forecasts.get(0).getDate().getDay()),
                () -> assertEquals(WeatherCondition.LIGHT_RAIN, forecasts.get(0).getWeather()),
                () -> assertEquals(26, forecasts.get(0).getTemperature().getMax().getCelsius()),
                () -> assertEquals(25, forecasts.get(0).getTemperature().getMin().getCelsius()),
                () -> assertEquals(2, forecasts.get(0).getWind().getLevel()),
                () -> assertEquals(17, forecasts.get(1).getDate().getDay()),
                () -> assertEquals(WeatherCondition.CLEAR, forecasts.get(1).getWeather()),
                () -> assertEquals(32, forecasts.get(1).getTemperature().getMax().getCelsius()),
                () -> assertEquals(24, forecasts.get(1).getTemperature().getMin().getCelsius()),
                () -> assertEquals(3, forecasts.get(1).getWind().getLevel()));
    }

    @Test
    void find_http500_emitsHttpException() {
        // Arrange
        server.enqueue(new MockResponse.Builder().code(500).build());

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertNotComplete().assertNoValues();

        // Assert
        observer.assertError(error -> error instanceof HttpException http && http.code() == 500);
    }

    @Test
    void find_malformedJson_emitsParsingError() {
        // Arrange
        server.enqueue(json("{\"dataseries\":INVALID}"));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertNotComplete().assertNoValues();

        // Assert
        observer.assertError(JsonEncodingException.class);
    }

    @Test
    void find_missingDataseries_emitsMappingError() {
        // Arrange
        server.enqueue(json("{}"));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertNotComplete().assertNoValues();

        // Assert
        observer.assertError(error -> error instanceof IllegalStateException
                && error.getMessage().contains("dataseries"));
    }

    @Test
    void find_mixedInvalidAndValidDays_keepsValidDaysInOrder() {
        // Arrange
        server.enqueue(json(series("{\"weather\":\"clear\"}," + FIRST
                + ",{\"date\":20260818,\"weather\":\"clear\",\"temp2m\":{\"max\":20,\"min\":25},\"wind10m_max\":2},"
                + SECOND)));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();
        List<DailyForecast> forecasts = observer.values().get(0);

        // Assert
        assertAll(
                () -> assertEquals(2, forecasts.size()),
                () -> assertEquals(16, forecasts.get(0).getDate().getDay()),
                () -> assertEquals(17, forecasts.get(1).getDate().getDay()));
    }

    @Test
    void find_allDaysInvalid_emitsFirstCause() {
        // Arrange
        server.enqueue(json(series("{\"weather\":\"clear\"},{\"date\":20260816}")));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertNotComplete().assertNoValues();

        // Assert
        observer.assertError(error -> error instanceof IllegalStateException
                && error.getMessage().contains("どの要素も")
                && error.getCause() instanceof IllegalStateException cause
                && cause.getMessage().contains("date"));
    }

    @Test
    void find_emptyDataseries_emitsEmptySuccess() {
        // Arrange
        server.enqueue(json(series("")));

        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();

        // Assert
        assertTrue(observer.values().get(0).isEmpty());
    }

    @Test
    void find_beforeAndAfterSubscriptions_requestsOnlyOnEachSubscription() {
        // Arrange
        server.enqueue(json(series(FIRST)));
        server.enqueue(json(series(SECOND)));
        Single<List<DailyForecast>> stream = repository.find(Location.TOKYO);

        // Act
        int beforeSubscription = server.getRequestCount();
        TestObserver<List<DailyForecast>> first = stream.test();
        first.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();
        int afterFirstSubscription = server.getRequestCount();
        now += 86_400_000L;
        TestObserver<List<DailyForecast>> second = stream.test();
        second.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();

        // Assert
        assertAll(
                () -> assertEquals(0, beforeSubscription),
                () -> assertEquals(1, afterFirstSubscription),
                () -> assertEquals(2, server.getRequestCount()),
                () -> assertEquals(16, first.values().get(0).get(0).getDate().getDay()),
                () -> assertEquals(17, second.values().get(0).get(0).getDate().getDay()));
    }


    @Test
    void find_freshCache_returnsSavedForecastWithoutCommunication() {
        // Arrange
        saveStored(Location.TOKYO, java.util.Collections.emptyList(), now - 86_399_999L);
        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        // Assert
        observer.assertComplete().assertValue(List::isEmpty);
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void find_exactly24HoursOld_fetchesAndSaves() {
        // Arrange
        saveStored(Location.TOKYO, java.util.Collections.emptyList(), now - 86_400_000L);
        server.enqueue(json(series(FIRST)));
        // Act
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        // Assert
        assertAll(() -> assertEquals(1, server.getRequestCount()),
                () -> assertEquals(now, readStored(Location.TOKYO).lastFetchedAt()),
                () -> assertEquals(16, readStored(Location.TOKYO).forecasts().get(0).getDate().getDay()));
    }

    @Test
    void find_futureTimestamp_fetchesAgain() {
        // Arrange
        saveStored(Location.TOKYO, java.util.Collections.emptyList(), now + 1);
        server.enqueue(json(series(FIRST)));
        // Act
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        // Assert
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void find_expiredCacheAndHttpFailure_preservesTimestamp() {
        // Arrange
        long previous = now - 86_400_001L;
        saveStored(Location.TOKYO, java.util.Collections.emptyList(), previous);
        server.enqueue(new MockResponse.Builder().code(500).build());
        // Act
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertError(HttpException.class);
        // Assert
        assertEquals(previous, readStored(Location.TOKYO).lastFetchedAt());
    }

    @Test
    void find_saveFailure_emitsErrorAndPreservesCache() {
        // Arrange
        long previous = now - 86_400_000L;
        saveStored(Location.TOKYO, java.util.Collections.emptyList(), previous);
        store.saveFailure = new IllegalStateException("save failed");
        server.enqueue(json(series(FIRST)));
        // Act
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertError(store.saveFailure);
        // Assert
        assertEquals(previous, readStored(Location.TOKYO).lastFetchedAt());
    }

    @Test
    void find_readFailure_emitsErrorWithoutCommunication() {
        // Arrange
        store.readFailure = new IllegalStateException("read failed");
        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        // Assert
        observer.assertError(store.readFailure);
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void find_sameStreamSubscribedAgain_readsFreshCache() {
        // Arrange
        server.enqueue(json(series(FIRST)));
        Single<List<DailyForecast>> stream = repository.find(Location.TOKYO);
        // Act
        int readsBefore = store.reads;
        stream.test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        stream.test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        // Assert
        assertAll(() -> assertEquals(0, readsBefore),
                () -> assertEquals(2, store.reads),
                () -> assertEquals(1, server.getRequestCount()));
    }

    @Test
    void find_otherLocation_doesNotUseTokyoCache() {
        // Arrange
        saveStored(Location.TOKYO, java.util.Collections.emptyList(), now);
        Location other = Location.of(34, 135);
        server.enqueue(json(series(SECOND)));
        // Act
        repository.find(other).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        // Assert
        assertAll(() -> assertEquals(1, server.getRequestCount()),
                () -> assertTrue(readStored(Location.TOKYO).forecasts().isEmpty()),
                () -> assertEquals(17, readStored(other).forecasts().get(0).getDate().getDay()));
    }

    @Test
    void find_emptyResponse_savesTimestampAndAvoidsNextCommunication() {
        // Arrange
        server.enqueue(json(series("")));
        // Act
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        repository.find(Location.TOKYO).test().assertComplete().assertValue(List::isEmpty);
        // Assert
        assertAll(() -> assertEquals(now, readStored(Location.TOKYO).lastFetchedAt()),
                () -> assertEquals(1, server.getRequestCount()));
    }

    @Test
    void find_completedResponse_usesTimeImmediatelyBeforeSave() {
        // Arrange
        RemoteForecastRepository delayed = new RemoteForecastRepository((latitude, longitude) -> {
            now += 5000;
            com.example.rxjava.data.remote.ForecastResponse response = new com.example.rxjava.data.remote.ForecastResponse();
            response.dataseries = java.util.Collections.emptyList();
            return Single.just(response);
        }, store, () -> now);
        long start = now;
        // Act
        delayed.find(Location.TOKYO).test().assertComplete();
        // Assert
        assertEquals(start + 5000, readStored(Location.TOKYO).lastFetchedAt());
    }


    @Test
    void find_freshNonemptyCache_returnsStoredForecast() {
        // Arrange
        server.enqueue(json(series(FIRST)));
        TestObserver<List<DailyForecast>> first = repository.find(Location.TOKYO).test();
        first.awaitDone(5, TimeUnit.SECONDS).assertComplete();
        List<DailyForecast> saved = first.values().get(0);
        now += 86_399_999L;
        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        // Assert
        observer.assertComplete().assertValue(saved);
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void find_expiredNonemptyCache_refreshesForecasts() {
        // Arrange
        server.enqueue(json(series(FIRST)));
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        now += 86_400_001L;
        server.enqueue(json(series(SECOND)));
        // Act
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        // Assert
        assertAll(() -> assertEquals(2, server.getRequestCount()),
                () -> assertEquals(17, readStored(Location.TOKYO).forecasts().get(0).getDate().getDay()),
                () -> assertEquals(now, readStored(Location.TOKYO).lastFetchedAt()));
    }

    @Test
    void find_expiredCacheAndMappingFailure_preservesPreviousForecastAndTimestamp() {
        // Arrange
        server.enqueue(json(series(FIRST)));
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        long previous = now;
        now += 86_400_000L;
        server.enqueue(json("{}"));
        // Act
        repository.find(Location.TOKYO).test().awaitDone(5, TimeUnit.SECONDS).assertError(IllegalStateException.class);
        // Assert
        assertAll(() -> assertEquals(previous, readStored(Location.TOKYO).lastFetchedAt()),
                () -> assertEquals(16, readStored(Location.TOKYO).forecasts().get(0).getDate().getDay()));
    }



    @Test
    void find_savePending_emitsResultOnlyAfterSaveCompletes() {
        // Arrange
        io.reactivex.rxjava3.subjects.CompletableSubject gate = io.reactivex.rxjava3.subjects.CompletableSubject.create();
        store.saveGate = gate;
        repository = immediateRepository();
        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.assertNoValues().assertNotComplete();
        boolean saving = gate.hasObservers();
        gate.onComplete();
        // Assert
        assertTrue(saving);
        observer.assertComplete().assertNoErrors();
        assertEquals(16, observer.values().get(0).get(0).getDate().getDay());
        assertEquals(1, store.saves);
    }

    @Test
    void find_pendingSaveFails_emitsErrorWithoutSuccess() {
        // Arrange
        io.reactivex.rxjava3.subjects.CompletableSubject gate = io.reactivex.rxjava3.subjects.CompletableSubject.create();
        store.saveGate = gate;
        RuntimeException error = new IllegalStateException("delayed save failed");
        repository = immediateRepository();
        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        gate.onError(error);
        // Assert
        observer.assertError(error).assertNoValues().assertNotComplete();
        assertEquals(0, store.saves);
    }

    @Test
    void find_disposedWhileSaving_disposesSaveAndDoesNotPublishResult() {
        // Arrange
        io.reactivex.rxjava3.subjects.CompletableSubject gate = io.reactivex.rxjava3.subjects.CompletableSubject.create();
        store.saveGate = gate;
        repository = immediateRepository();
        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.dispose();
        boolean stillSubscribed = gate.hasObservers();
        gate.onComplete();
        // Assert
        assertTrue(!stillSubscribed);
        observer.assertNoValues().assertNotComplete();
        assertEquals(0, store.saves);
    }

    @Test
    void find_readPending_checksExpirationWhenReadCompletes() {
        // Arrange
        long timestamp = now;
        io.reactivex.rxjava3.subjects.SingleSubject<ForecastStore.Snapshot> read =
                io.reactivex.rxjava3.subjects.SingleSubject.create();
        store.readOverride = read;
        server.enqueue(json(series(FIRST)));
        // Act
        TestObserver<List<DailyForecast>> observer = repository.find(Location.TOKYO).test();
        observer.assertNoValues().assertNotComplete();
        now += 86_400_000L;
        read.onSuccess(new ForecastStore.Snapshot(java.util.Collections.emptyList(), timestamp));
        // Assert
        observer.awaitDone(5, TimeUnit.SECONDS).assertComplete();
        assertEquals(1, server.getRequestCount());
    }

    @Test
    void readAndSave_beforeSubscription_doesNotAccessFakeStore() {
        // Arrange
        Single<ForecastStore.Snapshot> read = store.read(Location.TOKYO);
        io.reactivex.rxjava3.core.Completable save = store.save(Location.TOKYO, java.util.Collections.emptyList(), now);
        // Act
        int readsBefore = store.reads;
        int savesBefore = store.saves;
        save.test().assertComplete();
        read.test().assertComplete();
        // Assert
        assertAll(() -> assertEquals(0, readsBefore), () -> assertEquals(0, savesBefore),
                () -> assertEquals(1, store.reads), () -> assertEquals(1, store.saves));
    }


    private RemoteForecastRepository immediateRepository() {
        com.example.rxjava.data.remote.ForecastResponse response = new com.example.rxjava.data.remote.ForecastResponse();
        com.example.rxjava.data.remote.ForecastResponse.Entry entry = new com.example.rxjava.data.remote.ForecastResponse.Entry();
        entry.date = 20260816;
        entry.weather = "clear";
        entry.maxWind = 2;
        entry.temperature = new com.example.rxjava.data.remote.ForecastResponse.Temperature();
        entry.temperature.max = 30;
        entry.temperature.min = 20;
        response.dataseries = java.util.Collections.singletonList(entry);
        return new RemoteForecastRepository((latitude, longitude) -> Single.just(response), store, () -> now);
    }

    private ForecastStore.Snapshot readStored(Location location) {
        TestObserver<ForecastStore.Snapshot> observer = store.read(location).test();
        observer.awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();
        return observer.values().get(0);
    }

    private void saveStored(Location location, List<DailyForecast> forecasts, long timestamp) {
        store.save(location, forecasts, timestamp).test()
                .awaitDone(5, TimeUnit.SECONDS).assertComplete().assertNoErrors();
    }

    private static MockResponse json(String body) {
        return new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build();
    }

    private static String series(String entries) {
        return "{\"dataseries\":[" + entries + "]}";
    }
}
