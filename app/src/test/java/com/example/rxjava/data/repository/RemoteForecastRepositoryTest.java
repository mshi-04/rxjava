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
        repository = new RemoteForecastRepository(retrofit.create(ForecastApi.class));
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
