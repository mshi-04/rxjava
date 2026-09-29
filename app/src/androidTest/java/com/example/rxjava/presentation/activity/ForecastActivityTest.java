package com.example.rxjava.presentation.activity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.rxjava.presentation.activity.ForecastUiTestSupport.captureScreen;
import static com.example.rxjava.presentation.activity.ForecastUiTestSupport.hasForecastCount;
import static com.example.rxjava.presentation.activity.ForecastUiTestSupport.hasForecastRow;
import static com.example.rxjava.presentation.activity.ForecastUiTestSupport.scrollToForecast;
import static org.hamcrest.Matchers.not;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.fragment.app.Fragment;

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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        repository.resetSubscriptionCount();
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
    public void forecastScreen_loadFails_showsErrorAndHidesList() throws IOException {
        // Arrange
        repository.failWith(new IOException("network down"));

        // Act
        scenario = ActivityScenario.launch(ForecastActivity.class);

        // Assert
        onView(withText(R.string.forecast_error_message)).check(matches(isDisplayed()));
        onView(withText(R.string.forecast_retry)).check(matches(isDisplayed()));
        onView(withId(R.id.forecastList)).check(matches(not(isDisplayed())));
        onView(withId(R.id.loadingIndicator)).check(matches(not(isDisplayed())));
        captureScreen("failure");
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
    public void forecastScreen_loadIsPending_showsLoadingIndicator() throws IOException {
        // Arrange
        repository.remainPending();

        // Act
        scenario = ActivityScenario.launch(ForecastActivity.class);

        // Assert
        onView(withId(R.id.loadingIndicator)).check(matches(isDisplayed()));
        onView(withId(R.id.forecastList)).check(matches(not(isDisplayed())));
        onView(withId(R.id.errorContainer)).check(matches(not(isDisplayed())));
        captureScreen("loading");
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
        assertEquals(1, repository.subscriptionCount());
    }

    @Test
    public void forecastScreen_fragmentViewRecreated_keepsForecastWithoutNewRequest() {
        // Arrange
        repository.respondWith(Collections.singletonList(lightRainOnAugust16()));
        scenario = ActivityScenario.launch(ForecastActivity.class);
        onView(withText("2026年8月16日")).check(matches(isDisplayed()));
        AtomicReference<Fragment> fragment = new AtomicReference<>();

        // Act
        scenario.onActivity(activity -> {
            fragment.set(activity.getSupportFragmentManager()
                    .findFragmentById(R.id.forecastFragmentContainer));
            activity.getSupportFragmentManager().beginTransaction().detach(fragment.get()).commitNow();
            activity.getSupportFragmentManager().beginTransaction().attach(fragment.get()).commitNow();
        });

        // Assert
        onView(withText("2026年8月16日")).check(matches(isDisplayed()));
        assertEquals(1, repository.subscriptionCount());
    }

    @Test
    public void forecastScreen_responseWhileViewDestroyed_rendersAfterAttachWithoutNewRequest() {
        // Arrange
        repository.remainPending();
        scenario = ActivityScenario.launch(ForecastActivity.class);
        onView(withId(R.id.loadingIndicator)).check(matches(isDisplayed()));
        AtomicReference<Fragment> fragment = new AtomicReference<>();
        scenario.onActivity(activity -> {
            fragment.set(activity.getSupportFragmentManager()
                    .findFragmentById(R.id.forecastFragmentContainer));
            activity.getSupportFragmentManager().beginTransaction().detach(fragment.get()).commitNow();
        });

        // Act
        repository.completePending(Collections.singletonList(lightRainOnAugust16()));
        // View が無い間に main の通知を処理し、古い callback が残る不具合を隠さない
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(activity -> activity.getSupportFragmentManager()
                .beginTransaction().attach(fragment.get()).commitNow());

        // Assert
        onView(withText("2026年8月16日")).check(matches(isDisplayed()));
        assertEquals(1, repository.subscriptionCount());
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

    @Test
    public void forecastScreen_sevenDays_scrollsInOrderAndReturnsToFirstRow() throws IOException {
        // Arrange
        repository.respondWith(List.of(
                forecastOnAugustDay(16, WeatherCondition.CLEAR, 30, 21, 0),
                forecastOnAugustDay(17, WeatherCondition.LIGHT_RAIN, 26, 25, 2),
                forecastOnAugustDay(18, WeatherCondition.CLOUDY, 23, 18, 4),
                forecastOnAugustDay(19, WeatherCondition.SNOW, 1, -3, 6),
                forecastOnAugustDay(20, WeatherCondition.RAIN_SNOW, 3, -1, 8),
                forecastOnAugustDay(21, WeatherCondition.THUNDERSTORM_WITH_RAIN, 22, 15, 10),
                forecastOnAugustDay(22, WeatherCondition.UNKNOWN, 20, 12, 12)));

        // Act
        scenario = ActivityScenario.launch(ForecastActivity.class);

        // Assert
        onView(withId(R.id.forecastList)).check(matches(isDisplayed())).check(hasForecastCount(7));
        onView(withId(R.id.forecastList))
                .check(hasForecastRow(0, "2026年8月16日", "晴れ", 30, 21, 0));
        captureScreen("success-first");
        onView(withId(R.id.forecastList)).perform(scrollToForecast(1))
                .check(hasForecastRow(1, "2026年8月17日", "小雨", 26, 25, 2));
        onView(withId(R.id.forecastList)).perform(scrollToForecast(2))
                .check(hasForecastRow(2, "2026年8月18日", "曇り", 23, 18, 4));
        onView(withId(R.id.forecastList)).perform(scrollToForecast(3))
                .check(hasForecastRow(3, "2026年8月19日", "雪", 1, -3, 6));
        onView(withId(R.id.forecastList)).perform(scrollToForecast(4))
                .check(hasForecastRow(4, "2026年8月20日", "みぞれ", 3, -1, 8));
        onView(withId(R.id.forecastList)).perform(scrollToForecast(5))
                .check(hasForecastRow(5, "2026年8月21日", "雷を伴う雨", 22, 15, 10));
        onView(withId(R.id.forecastList)).perform(scrollToForecast(6))
                .check(hasForecastRow(6, "2026年8月22日", "不明", 20, 12, 12));
        captureScreen("success-last");
        onView(withId(R.id.forecastList)).perform(scrollToForecast(0))
                .check(hasForecastRow(0, "2026年8月16日", "晴れ", 30, 21, 0));
        captureScreen("success-returned");
    }

    @Test
    public void forecastScreen_unknownWeather_showsUnknownWithRemainingFields() {
        // Arrange
        repository.respondWith(Collections.singletonList(
                forecastOnAugustDay(22, WeatherCondition.UNKNOWN, 20, 12, 12)));

        // Act
        scenario = ActivityScenario.launch(ForecastActivity.class);

        // Assert
        onView(withId(R.id.forecastList)).check(hasForecastCount(1))
                .check(hasForecastRow(0, "2026年8月22日", "不明", 20, 12, 12));
        onView(withText("不明")).check(matches(isDisplayed()));
        onView(withId(R.id.errorContainer)).check(matches(not(isDisplayed())));
    }

    @Test
    public void forecastScreen_emptySuccess_showsNoRowsLoadingOrError() {
        // Arrange
        repository.respondWith(Collections.emptyList());

        // Act
        scenario = ActivityScenario.launch(ForecastActivity.class);

        // Assert
        onView(withId(R.id.forecastList)).check(matches(isDisplayed())).check(hasForecastCount(0));
        onView(withId(R.id.loadingIndicator)).check(matches(not(isDisplayed())));
        onView(withId(R.id.errorContainer)).check(matches(not(isDisplayed())));
    }

    private static DailyForecast lightRainOnAugust16() {
        return forecastOnAugustDay(16, WeatherCondition.LIGHT_RAIN, 26, 25, 2);
    }

    private static DailyForecast forecastOnAugustDay(int day, WeatherCondition weather,
            int max, int min, int wind) {
        return new DailyForecast(
                ForecastDate.of(2026, 8, day),
                weather,
                TemperatureRange.of(Temperature.ofCelsius(max), Temperature.ofCelsius(min)),
                WindSpeed.ofLevel(wind));
    }
}
