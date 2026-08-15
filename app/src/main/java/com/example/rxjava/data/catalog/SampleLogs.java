package com.example.rxjava.data.catalog;

import com.example.rxjava.domain.model.SampleLogLine;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Notification;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;

public final class SampleLogs {

    public interface LogEmitter {

        void log(String message);
    }

    private SampleLogs() {
    }

    public static <T> Observable<SampleLogLine> materialize(Observable<T> source) {
        return source.materialize().map(SampleLogs::toLogLine);
    }

    public static <T> Observable<SampleLogLine> materialize(Flowable<T> source) {
        return source.materialize().map(SampleLogs::toLogLine).toObservable();
    }

    public static Observable<SampleLogLine> create(Consumer<LogEmitter> work) {
        return Observable.<SampleLogLine>create(emitter -> {
            work.accept(message -> emitter.onNext(SampleLogLine.onCurrentThread(message)));
            emitter.onComplete();
        });
    }

    private static <T> SampleLogLine toLogLine(Notification<T> notification) {
        if (notification.isOnNext()) {
            return SampleLogLine.onCurrentThread("onNext: " + notification.getValue());
        }
        if (notification.isOnError()) {
            return SampleLogLine.onCurrentThread("onError: " + notification.getError());
        }
        return SampleLogLine.onCurrentThread("onComplete");
    }
}
