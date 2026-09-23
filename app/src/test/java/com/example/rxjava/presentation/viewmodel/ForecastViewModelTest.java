package com.example.rxjava.presentation.viewmodel;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import androidx.databinding.Observable;

import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.model.Temperature;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;
import com.example.rxjava.domain.model.WindSpeed;
import com.example.rxjava.domain.repository.ForecastRepository;
import com.example.rxjava.domain.usecase.GetForecastUseCase;
import com.example.rxjava.presentation.util.UiSchedulers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.SingleSubject;

class ForecastViewModelTest {

    private FakeForecastRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FakeForecastRepository();
    }

    @Test
    void constructor_responsePending_publishesLoading() {
        // Arrange
        repository.enqueue(SingleSubject.create());

        // Act
        ForecastViewModel viewModel = createViewModel();

        // Assert
        assertInstanceOf(ForecastUiState.Loading.class, viewModel.state.get());
    }

    @Test
    void constructor_requestsTokyo() {
        // Arrange
        repository.enqueue(SingleSubject.create());

        // Act
        createViewModel();

        // Assert
        assertEquals(Collections.singletonList(Location.TOKYO), repository.requestedLocations);
    }

    @Test
    void constructor_responseSucceeds_publishesSuccessWithForecasts() {
        // Arrange
        List<DailyForecast> forecasts = Collections.singletonList(lightRainOnAugust16());
        repository.enqueue(Single.just(forecasts));

        // Act
        ForecastViewModel viewModel = createViewModel();

        // Assert
        ForecastUiState.Success success =
                assertInstanceOf(ForecastUiState.Success.class, viewModel.state.get());
        assertEquals(forecasts, success.forecasts());
    }

    @Test
    void constructor_responseFails_publishesFailure() {
        // Arrange
        repository.enqueue(Single.error(new IOException("network down")));

        // Act
        ForecastViewModel viewModel = createViewModel();

        // Assert
        assertInstanceOf(ForecastUiState.Failure.class, viewModel.state.get());
    }

    @Test
    void constructor_pendingResponseCompletesLater_movesFromLoadingToSuccess() {
        // Arrange
        SingleSubject<List<DailyForecast>> response = SingleSubject.create();
        repository.enqueue(response);
        ForecastViewModel viewModel = createViewModel();

        // Act
        response.onSuccess(Collections.singletonList(lightRainOnAugust16()));

        // Assert
        assertInstanceOf(ForecastUiState.Success.class, viewModel.state.get());
    }

    @Test
    void load_publishingSuccessThrows_publishesFailure() {
        // Arrange
        repository.enqueue(SingleSubject.create());
        repository.enqueue(Single.just(Collections.singletonList(lightRainOnAugust16())));
        ForecastViewModel viewModel = createViewModel();
        // Activity の render から Adapter までの連鎖が落ちた場合を再現する
        viewModel.state.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (viewModel.state.get() instanceof ForecastUiState.Success) {
                    throw new IllegalStateException("adapter failed");
                }
            }
        });

        // Act
        viewModel.load();

        // Assert
        assertInstanceOf(ForecastUiState.Failure.class, viewModel.state.get());
    }

    @Test
    void load_retryAfterFailure_publishesSuccess() {
        // Arrange
        repository.enqueue(Single.error(new IOException("network down")));
        List<DailyForecast> forecasts = Collections.singletonList(lightRainOnAugust16());
        repository.enqueue(Single.just(forecasts));
        ForecastViewModel viewModel = createViewModel();

        // Act
        viewModel.load();

        // Assert
        ForecastUiState.Success success =
                assertInstanceOf(ForecastUiState.Success.class, viewModel.state.get());
        assertEquals(forecasts, success.forecasts());
    }

    @Test
    void load_calledWhilePending_disposesPreviousSubscription() {
        // Arrange
        SingleSubject<List<DailyForecast>> first = SingleSubject.create();
        SingleSubject<List<DailyForecast>> second = SingleSubject.create();
        repository.enqueue(first);
        repository.enqueue(second);
        ForecastViewModel viewModel = createViewModel();

        // Act
        viewModel.load();

        // Assert
        assertAll(
                () -> assertFalse(first.hasObservers()),
                () -> assertTrue(second.hasObservers()));
    }

    @Test
    void load_previousResponseArrivesAfterReload_doesNotOverwriteState() {
        // Arrange
        SingleSubject<List<DailyForecast>> first = SingleSubject.create();
        repository.enqueue(first);
        repository.enqueue(SingleSubject.create());
        ForecastViewModel viewModel = createViewModel();
        viewModel.load();

        // Act
        first.onError(new IOException("late failure"));

        // Assert
        assertInstanceOf(ForecastUiState.Loading.class, viewModel.state.get());
    }

    @Test
    void onCleared_whilePending_disposesSubscription() {
        // Arrange
        SingleSubject<List<DailyForecast>> response = SingleSubject.create();
        repository.enqueue(response);
        ForecastViewModel viewModel = createViewModel();

        // Act
        viewModel.onCleared();

        // Assert
        assertFalse(response.hasObservers());
    }

    // コンストラクタが取得を始めるため、応答を enqueue してから呼ぶ
    private ForecastViewModel createViewModel() {
        // trampoline で購読したスレッドのまま同期実行させ、呼び出しが戻った時点で結果を見られるようにする
        UiSchedulers synchronous = new UiSchedulers(Schedulers.trampoline(), Schedulers.trampoline());
        return new ForecastViewModel(new GetForecastUseCase(repository), synchronous);
    }

    private static DailyForecast lightRainOnAugust16() {
        return new DailyForecast(
                ForecastDate.of(2026, 8, 16),
                WeatherCondition.LIGHT_RAIN,
                TemperatureRange.of(Temperature.ofCelsius(26), Temperature.ofCelsius(25)),
                WindSpeed.ofLevel(2));
    }

    private static final class FakeForecastRepository implements ForecastRepository {

        final List<Location> requestedLocations = new ArrayList<>();

        private final List<Single<List<DailyForecast>>> responses = new ArrayList<>();

        void enqueue(Single<List<DailyForecast>> response) {
            responses.add(response);
        }

        @Override
        public Single<List<DailyForecast>> find(Location location) {
            requestedLocations.add(location);
            return responses.remove(0);
        }
    }
}
