package com.example.rxjava.presentation.activity;

import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

final class ForecastUiTestSupport {

    private ForecastUiTestSupport() {
    }

    static ViewAction scrollToForecast(int position) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return allOf(isDisplayed(), isAssignableFrom(RecyclerView.class));
            }

            @Override
            public String getDescription() {
                return "予報の " + position + " 行目へスクロールする";
            }

            @Override
            public void perform(UiController uiController, View view) {
                RecyclerView list = (RecyclerView) view;
                assertTrue("予報一覧の LayoutManager", list.getLayoutManager() instanceof LinearLayoutManager);
                ((LinearLayoutManager) list.getLayoutManager()).scrollToPositionWithOffset(position, 0);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    static ViewAssertion hasForecastCount(int expected) {
        return (view, noViewFound) -> {
            if (noViewFound != null) {
                throw noViewFound;
            }
            RecyclerView list = (RecyclerView) view;
            assertNotNull("一覧の Adapter が設定されている", list.getAdapter());
            assertEquals("表示する予報の件数", expected, list.getAdapter().getItemCount());
        };
    }

    static ViewAssertion hasForecastRow(int position, String date, String weather,
            int max, int min, int wind) {
        return (view, noViewFound) -> {
            if (noViewFound != null) {
                throw noViewFound;
            }
            RecyclerView.ViewHolder row = ((RecyclerView) view)
                    .findViewHolderForAdapterPosition(position);
            assertNotNull("指定した位置の予報行が描画されている: " + position, row);
            assertThat(row.itemView, allOf(
                    hasDescendant(allOf(withText(date), isDisplayed())),
                    hasDescendant(allOf(withText(weather), isDisplayed())),
                    hasDescendant(allOf(withText("最高 " + max + "℃ / 最低 " + min + "℃"), isDisplayed())),
                    hasDescendant(allOf(withText("風速階級 " + wind), isDisplayed()))));
        };
    }

    static void captureScreen(String state) throws IOException {
        String run = InstrumentationRegistry.getArguments().getString("uiValidationRun");
        if (run == null) {
            return;
        }
        String outputDirectory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir");
        assertNotNull("Gradle の端末テストが検証画像の回収先を指定している", outputDirectory);
        File directory = new File(outputDirectory,
                "ui-validation/" + run.replaceAll("[^A-Za-z0-9_-]", "_"));
        assertTrue("検証画像の保存先を作成できる", directory.isDirectory() || directory.mkdirs());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        // メインスレッドが idle でも、スクロール後のフレームと accessibility 通知は反映途中になり得る。
        try {
            automation.waitForIdle(100, 5000);
        } catch (TimeoutException notIdle) {
            throw new IOException("検証画像の取得前に画面が安定しなかった", notIdle);
        }
        Bitmap screenshot = automation.takeScreenshot();
        assertNotNull("画面のスクリーンショットを取得できる", screenshot);
        try (FileOutputStream output = new FileOutputStream(new File(directory, state + ".png"))) {
            assertTrue("検証画像を保存できる", screenshot.compress(Bitmap.CompressFormat.PNG, 100, output));
        } finally {
            screenshot.recycle();
        }
    }
}
