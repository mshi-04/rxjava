package com.example.rxjava.presentation.activity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.rxjava.R;
import com.example.rxjava.domain.model.DailyForecast;
import com.example.rxjava.domain.model.ForecastDate;
import com.example.rxjava.domain.model.Location;
import com.example.rxjava.domain.model.Temperature;
import com.example.rxjava.domain.model.TemperatureRange;
import com.example.rxjava.domain.model.WeatherCondition;
import com.example.rxjava.domain.model.WindSpeed;
import com.example.rxjava.fake.FakeForecastRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.junit.Assert.assertEquals;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class ForecastActivityTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject
    FakeForecastRepository repository;

    private ActivityScenario<ForecastActivity> scenario;

    @Before
    public void setUp() {
        hiltRule.inject();
        // Espresso は RxJava のスレッドを待たないため、io を購読元スレッドで同期実行させる
        RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
        // reset() はプロセス全体のフックを消すため、setUp で入れたものだけ戻す
        RxJavaPlugins.setIoSchedulerHandler(null);
    }

    @Test
    public void forecastScreen_loadSucceeds_showsForecastRow() {
        // Arrange
        repository.respondWith(Collections.singletonList(lightRainOnAugust16()));

        // Act
        scenario = ActivityScenario.launch(ForecastActivity.class);

        // Assert
        onView(withText("2026年8月16日")).check(matches(isDisplayed()));
        onView(withText("小雨")).check(matches(isDisplayed()));
        onView(withText("最高 26℃ / 最低 25℃")).check(matches(isDisplayed()));
    }

    @Test
    public void forecastScreen_loadFails_showsErrorAndHidesList() {
        // Arrange
        repository.failWith(new IOException("network down"));

        // Act
        scenario = ActivityScenario.launch(ForecastActivity.class);

        // Assert
        onView(withText(R.string.forecast_error_message)).check(matches(isDisplayed()));
        onView(withText(R.string.forecast_retry)).check(matches(isDisplayed()));
        onView(withId(R.id.forecastList)).check(matches(not(isDisplayed())));
        onView(withId(R.id.loadingIndicator)).check(matches(not(isDisplayed())));
    }

    @Test
    public void forecastScreen_loadSucceeds_hidesLoadingIndicator() {
        // Arrange
        repository.respondWith(Collections.singletonList(lightRainOnAugust16()));

        // Act
        scenario = ActivityScenario.launch(ForecastActivity.class);

        // Assert
        onView(withId(R.id.loadingIndicator)).check(matches(not(isDisplayed())));
        onView(withId(R.id.errorContainer)).check(matches(not(isDisplayed())));
    }

    @Test
    public void forecastScreen_loadIsPending_showsLoadingIndicator() {
        // Arrange
        repository.remainPending();

        // Act
        scenario = ActivityScenario.launch(ForecastActivity.class);

        // Assert
        onView(withId(R.id.loadingIndicator)).check(matches(isDisplayed()));
        onView(withId(R.id.forecastList)).check(matches(not(isDisplayed())));
        onView(withId(R.id.errorContainer)).check(matches(not(isDisplayed())));
    }

    @Test
    public void forecastScreen_load_requestsTokyo() {
        // Arrange
        repository.respondWith(Collections.singletonList(lightRainOnAugust16()));

        // Act
        scenario = ActivityScenario.launch(ForecastActivity.class);

        // Assert
        assertEquals(Location.TOKYO, repository.requestedLocation());
    }

    @Test
    public void forecastScreen_recreated_keepsForecast() {
        // Arrange
        repository.respondWith(Collections.singletonList(lightRainOnAugust16()));
        scenario = ActivityScenario.launch(ForecastActivity.class);
        onView(withText("2026年8月16日")).check(matches(isDisplayed()));

        // Act
        scenario.recreate();

        // Assert
        onView(withText("2026年8月16日")).check(matches(isDisplayed()));
    }

    @Test
    public void forecastScreen_retryAfterFailure_showsForecastRow() {
        // Arrange
        repository.failWith(new IOException("network down"));
        scenario = ActivityScenario.launch(ForecastActivity.class);
        onView(withText(R.string.forecast_error_message)).check(matches(isDisplayed()));
        repository.respondWith(Collections.singletonList(lightRainOnAugust16()));

        // Act
        onView(withText(R.string.forecast_retry)).perform(click());

        // Assert
        onView(withText("2026年8月16日")).check(matches(isDisplayed()));
        onView(withText(R.string.forecast_error_message)).check(matches(not(isDisplayed())));
    }

    private static DailyForecast lightRainOnAugust16() {
        return new DailyForecast(
                ForecastDate.of(2026, 8, 16),
                WeatherCondition.LIGHT_RAIN,
                TemperatureRange.of(Temperature.ofCelsius(26), Temperature.ofCelsius(25)),
                WindSpeed.ofLevel(2));
    }
}
