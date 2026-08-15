package com.example.rxjava.data.catalog;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.rxjava.domain.model.SampleLogLine;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;

class SampleLogsTest {

    @Test
    void materializeObservableTurnsOnNextAndOnCompleteIntoLogLines() {
        TestObserver<SampleLogLine> observer =
                SampleLogs.materialize(Observable.just(10)).test();

        observer.awaitDone(5, TimeUnit.SECONDS);
        observer.assertNoErrors();
        observer.assertComplete();

        List<SampleLogLine> values = observer.values();
        assertAll(
                () -> assertEquals(2, values.size()),
                () -> assertEquals("onNext: 10", values.get(0).getMessage()),
                () -> assertEquals("onComplete", values.get(1).getMessage()));
    }

    @Test
    void materializeObservableTurnsAnErrorIntoALogLine() {
        TestObserver<SampleLogLine> observer =
                SampleLogs.materialize(Observable.error(new RuntimeException("boom"))).test();

        observer.awaitDone(5, TimeUnit.SECONDS);
        observer.assertNoErrors();
        observer.assertComplete();

        List<SampleLogLine> values = observer.values();
        assertAll(
                () -> assertEquals(1, values.size()),
                () -> assertTrue(values.get(0).getMessage().startsWith("onError: ")));
    }

    @Test
    void createEmitsOneLinePerLogCallThenCompletes() {
        TestObserver<SampleLogLine> observer = SampleLogs.create(log -> {
            log.log("first");
            log.log("second");
            log.log("third");
        }).test();

        observer.awaitDone(5, TimeUnit.SECONDS);
        observer.assertNoErrors();
        observer.assertComplete();

        List<SampleLogLine> values = observer.values();
        assertAll(
                () -> assertEquals(3, values.size()),
                () -> assertEquals("first", values.get(0).getMessage()),
                () -> assertEquals("second", values.get(1).getMessage()),
                () -> assertEquals("third", values.get(2).getMessage()));
    }

    @Test
    void everyProducedLineHasANonEmptyThreadName() {
        List<SampleLogLine> values = new ArrayList<>();
        values.addAll(SampleLogs.materialize(Observable.just(1, 2)).test().values());
        values.addAll(SampleLogs.create(log -> {
            log.log("a");
            log.log("b");
        }).test().values());

        assertFalse(values.isEmpty());
        assertAll(values.stream()
                .map(value -> () -> assertFalse(value.getThreadName().isEmpty())));
    }
}
