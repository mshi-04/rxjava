package com.example.rxjava.presentation.viewmodel;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;
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
    void state_responseCompletes_notifiesWithLatestValueAndPublicSender() {
        // Arrange
        SingleSubject<List<DailyForecast>> response = SingleSubject.create();
        repository.enqueue(response);
        ForecastViewModel viewModel = createViewModel();
        List<ForecastUiState> observed = new ArrayList<>();
        List<Observable> senders = new ArrayList<>();
        List<Integer> propertyIds = new ArrayList<>();
        viewModel.state.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                observed.add(viewModel.state.get());
                senders.add(sender);
                propertyIds.add(propertyId);
            }
        });

        // Act
        response.onSuccess(Collections.singletonList(lightRainOnAugust16()));

        // Assert
        assertAll(
                () -> assertEquals(1, observed.size()),
                () -> assertSame(viewModel.state.get(), observed.get(0)),
                () -> assertSame(viewModel.state, senders.get(0)),
                () -> assertEquals(0, propertyIds.get(0)));
    }

    @Test
    void state_callbackRegisteredAndRemoved_readsCurrentWithoutNotification() {
        // Arrange
        SingleSubject<List<DailyForecast>> response = SingleSubject.create();
        repository.enqueue(response);
        ForecastViewModel viewModel = createViewModel();
        AtomicInteger notifications = new AtomicInteger();
        Observable.OnPropertyChangedCallback callback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                notifications.incrementAndGet();
            }
        };

        // Act
        viewModel.state.addOnPropertyChangedCallback(callback);
        ForecastUiState initialState = viewModel.state.get();
        int notificationsAtRegistration = notifications.get();
        viewModel.state.removeOnPropertyChangedCallback(callback);
        response.onSuccess(Collections.singletonList(lightRainOnAugust16()));

        // Assert
        assertAll(
                () -> assertInstanceOf(ForecastUiState.Loading.class, initialState),
                () -> assertEquals(0, notificationsAtRegistration),
                () -> assertInstanceOf(ForecastUiState.Success.class, viewModel.state.get()),
                () -> assertEquals(0, notifications.get()));
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

    @Test
    void constructor_backgroundNotTriggered_delaysStreamSubscription() {
        // Arrange
        TestScheduler background = new TestScheduler();
        TestScheduler main = new TestScheduler();
        AtomicInteger subscriptions = new AtomicInteger();
        SingleSubject<List<DailyForecast>> response = SingleSubject.create();
        repository.enqueue(countSubscriptions(response, subscriptions));

        // Act
        ForecastViewModel viewModel = createViewModel(background, main);
        int beforeBackground = subscriptions.get();
        background.triggerActions();

        // Assert
        assertAll(
                () -> assertEquals(1, repository.requestedLocations.size()),
                () -> assertEquals(0, beforeBackground),
                () -> assertEquals(1, subscriptions.get()),
                () -> assertTrue(response.hasObservers()),
                () -> assertInstanceOf(ForecastUiState.Loading.class, viewModel.state.get()));
    }

    @Test
    void constructor_successQueuedOnMain_waitsForMainTrigger() {
        // Arrange
        TestScheduler background = new TestScheduler();
        TestScheduler main = new TestScheduler();
        repository.enqueue(Single.just(Collections.singletonList(lightRainOnAugust16())));
        ForecastViewModel viewModel = createViewModel(background, main);

        // Act
        background.triggerActions();
        ForecastUiState beforeMain = viewModel.state.get();
        main.triggerActions();

        // Assert
        ForecastUiState.Success success =
                assertInstanceOf(ForecastUiState.Success.class, viewModel.state.get());
        assertAll(
                () -> assertInstanceOf(ForecastUiState.Loading.class, beforeMain),
                () -> assertEquals(1, success.forecasts().size()));
    }

    @Test
    void constructor_errorQueuedOnMain_waitsForMainTrigger() {
        // Arrange
        TestScheduler background = new TestScheduler();
        TestScheduler main = new TestScheduler();
        repository.enqueue(Single.error(new IOException("network down")));
        ForecastViewModel viewModel = createViewModel(background, main);

        // Act
        background.triggerActions();
        ForecastUiState beforeMain = viewModel.state.get();
        main.triggerActions();

        // Assert
        assertAll(
                () -> assertInstanceOf(ForecastUiState.Loading.class, beforeMain),
                () -> assertInstanceOf(ForecastUiState.Failure.class, viewModel.state.get()));
    }

    @Test
    void load_successQueuedOnMain_cancelsPreviousNotification() {
        // Arrange
        TestScheduler background = new TestScheduler();
        TestScheduler main = new TestScheduler();
        repository.enqueue(Single.just(Collections.singletonList(lightRainOnAugust16())));
        SingleSubject<List<DailyForecast>> next = SingleSubject.create();
        repository.enqueue(next);
        ForecastViewModel viewModel = createViewModel(background, main);
        background.triggerActions();
        ForecastUiState queuedState = viewModel.state.get();

        // Act
        viewModel.load();
        background.triggerActions();
        main.triggerActions();

        // Assert
        assertAll(
                () -> assertInstanceOf(ForecastUiState.Loading.class, queuedState),
                () -> assertInstanceOf(ForecastUiState.Loading.class, viewModel.state.get()),
                () -> assertTrue(next.hasObservers()));
    }

    @Test
    void load_errorQueuedOnMain_cancelsPreviousNotification() {
        // Arrange
        TestScheduler background = new TestScheduler();
        TestScheduler main = new TestScheduler();
        repository.enqueue(Single.error(new IOException("old error")));
        SingleSubject<List<DailyForecast>> next = SingleSubject.create();
        repository.enqueue(next);
        ForecastViewModel viewModel = createViewModel(background, main);
        background.triggerActions();
        ForecastUiState queuedState = viewModel.state.get();

        // Act
        viewModel.load();
        background.triggerActions();
        main.triggerActions();

        // Assert
        assertAll(
                () -> assertInstanceOf(ForecastUiState.Loading.class, queuedState),
                () -> assertInstanceOf(ForecastUiState.Loading.class, viewModel.state.get()),
                () -> assertTrue(next.hasObservers()));
    }

    @Test
    void onCleared_successQueuedOnMain_cancelsNotification() {
        // Arrange
        TestScheduler background = new TestScheduler();
        TestScheduler main = new TestScheduler();
        repository.enqueue(Single.just(Collections.singletonList(lightRainOnAugust16())));
        ForecastViewModel viewModel = createViewModel(background, main);
        background.triggerActions();
        ForecastUiState queuedState = viewModel.state.get();

        // Act
        viewModel.onCleared();
        main.triggerActions();

        // Assert
        assertAll(
                () -> assertInstanceOf(ForecastUiState.Loading.class, queuedState),
                () -> assertInstanceOf(ForecastUiState.Loading.class, viewModel.state.get()));
    }

    @Test
    void onCleared_errorQueuedOnMain_cancelsNotification() {
        // Arrange
        TestScheduler background = new TestScheduler();
        TestScheduler main = new TestScheduler();
        repository.enqueue(Single.error(new IOException("old error")));
        ForecastViewModel viewModel = createViewModel(background, main);
        background.triggerActions();
        ForecastUiState queuedState = viewModel.state.get();

        // Act
        viewModel.onCleared();
        main.triggerActions();

        // Assert
        assertAll(
                () -> assertInstanceOf(ForecastUiState.Loading.class, queuedState),
                () -> assertInstanceOf(ForecastUiState.Loading.class, viewModel.state.get()));
    }

    @Test
    void load_beforeBackgroundSubscription_cancelsOldScheduledSubscription() {
        // Arrange
        TestScheduler background = new TestScheduler();
        TestScheduler main = new TestScheduler();
        AtomicInteger firstSubscriptions = new AtomicInteger();
        AtomicInteger secondSubscriptions = new AtomicInteger();
        repository.enqueue(countSubscriptions(SingleSubject.create(), firstSubscriptions));
        SingleSubject<List<DailyForecast>> second = SingleSubject.create();
        repository.enqueue(countSubscriptions(second, secondSubscriptions));
        ForecastViewModel viewModel = createViewModel(background, main);

        // Act
        viewModel.load();
        background.triggerActions();

        // Assert
        assertAll(
                () -> assertEquals(0, firstSubscriptions.get()),
                () -> assertEquals(1, secondSubscriptions.get()),
                () -> assertTrue(second.hasObservers()),
                () -> assertInstanceOf(ForecastUiState.Loading.class, viewModel.state.get()));
    }

    @Test
    void onCleared_beforeBackgroundSubscription_cancelsScheduledSubscription() {
        // Arrange
        TestScheduler background = new TestScheduler();
        TestScheduler main = new TestScheduler();
        AtomicInteger subscriptions = new AtomicInteger();
        repository.enqueue(countSubscriptions(SingleSubject.create(), subscriptions));
        ForecastViewModel viewModel = createViewModel(background, main);

        // Act
        viewModel.onCleared();
        background.triggerActions();

        // Assert
        assertAll(
                () -> assertEquals(0, subscriptions.get()),
                () -> assertInstanceOf(ForecastUiState.Loading.class, viewModel.state.get()));
    }

    // コンストラクタが取得を始めるため、応答を enqueue してから呼ぶ
    private ForecastViewModel createViewModel() {
        // trampoline で購読したスレッドのまま同期実行させ、呼び出しが戻った時点で結果を見られるようにする
        UiSchedulers synchronous = new UiSchedulers(Schedulers.trampoline(), Schedulers.trampoline());
        return new ForecastViewModel(new GetForecastUseCase(repository), synchronous);
    }

    private ForecastViewModel createViewModel(TestScheduler background, TestScheduler main) {
        return new ForecastViewModel(new GetForecastUseCase(repository),
                new UiSchedulers(background, main));
    }

    private static Single<List<DailyForecast>> countSubscriptions(
            Single<List<DailyForecast>> response, AtomicInteger count) {
        return Single.defer(() -> {
            count.incrementAndGet();
            return response;
        });
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
