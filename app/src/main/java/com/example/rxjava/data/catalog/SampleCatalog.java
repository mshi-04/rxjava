package com.example.rxjava.data.catalog;

import com.example.rxjava.domain.model.SampleId;
import com.example.rxjava.domain.model.SampleLogLine;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public final class SampleCatalog {

    private static final List<SampleDefinition> DEFINITIONS = Collections.unmodifiableList(Arrays.asList(
            new SampleDefinition(
                    SampleId.of("basic-observable"),
                    "Observable の基本",
                    "just / filter / map をつないで onNext・onComplete を観察する",
                    SampleCatalog::basicObservable),
            new SampleDefinition(
                    SampleId.of("scheduler-switching"),
                    "Scheduler の切り替え",
                    "subscribeOn だけの場合と observeOn を足した場合で、値が流れるスレッドがどう変わるかを見る",
                    SampleCatalog::schedulerSwitching),
            new SampleDefinition(
                    SampleId.of("interval-disposable"),
                    "interval と Disposable",
                    "無限に流れ続けるストリームを停止ボタンで dispose する",
                    SampleCatalog::intervalWithDisposable),
            new SampleDefinition(
                    SampleId.of("flowable-backpressure"),
                    "Flowable と backpressure",
                    "生産が消費より速いときに onBackpressureDrop が値を捨てる様子を見る",
                    SampleCatalog::flowableBackpressure),
            new SampleDefinition(
                    SampleId.of("behavior-subject"),
                    "BehaviorSubject の購読タイミング",
                    "購読開始が遅れた側に直前の値が届くことを確認する",
                    SampleCatalog::behaviorSubject),
            new SampleDefinition(
                    SampleId.of("error-handling"),
                    "エラーハンドリングと retry",
                    "2 回失敗してから成功する処理を retry で拾い、onErrorReturnItem で保険をかける",
                    SampleCatalog::errorHandling),
            new SampleDefinition(
                    SampleId.of("zip-streams"),
                    "zip による合成",
                    "有限のリストと interval を zip して、遅い側のペースで揃えて流す",
                    SampleCatalog::zipStreams)));

    private SampleCatalog() {
    }

    public static List<SampleDefinition> definitions() {
        return DEFINITIONS;
    }

    private static Observable<SampleLogLine> basicObservable() {
        return SampleLogs.materialize(
                Observable.just(1, 2, 3, 4, 5)
                        .filter(value -> value % 2 == 1)
                        .map(value -> value * 10));
    }

    private static Observable<SampleLogLine> schedulerSwitching() {
        Observable<String> onIo = Observable.fromCallable(() -> "値")
                .subscribeOn(Schedulers.io());

        return SampleLogs.materialize(onIo)
                .concatWith(SampleLogs.materialize(onIo.observeOn(Schedulers.computation())));
    }

    private static Observable<SampleLogLine> intervalWithDisposable() {
        return Observable.interval(0, 500, TimeUnit.MILLISECONDS)
                .map(tick -> SampleLogLine.onCurrentThread("tick " + tick));
    }

    private static Observable<SampleLogLine> flowableBackpressure() {
        AtomicLong dropped = new AtomicLong();
        return Flowable.interval(1, TimeUnit.MILLISECONDS)
                .onBackpressureDrop(value -> dropped.incrementAndGet())
                .observeOn(Schedulers.computation(), false, 1)
                .map(value -> {
                    Thread.sleep(300);
                    return SampleLogLine.onCurrentThread(
                            "処理した値: " + value + " / 破棄済み: " + dropped.get());
                })
                .toObservable();
    }

    private static Observable<SampleLogLine> behaviorSubject() {
        return SampleLogs.create(log -> {
            BehaviorSubject<String> subject = BehaviorSubject.createDefault("初期値");
            CompositeDisposable disposables = new CompositeDisposable();

            disposables.add(subject.subscribe(value -> log.log("購読A: " + value)));
            subject.onNext("1 回目の更新");
            disposables.add(subject.subscribe(value -> log.log("購読B: " + value)));
            subject.onNext("2 回目の更新");

            disposables.dispose();
        });
    }

    private static Observable<SampleLogLine> errorHandling() {
        AtomicInteger attempt = new AtomicInteger();
        return SampleLogs.materialize(
                Observable.fromCallable(() -> {
                            int count = attempt.incrementAndGet();
                            if (count < 3) {
                                throw new IllegalStateException("一時的な失敗 (" + count + " 回目)");
                            }
                            return "成功 (" + count + " 回目)";
                        })
                        .retry(3)
                        .onErrorReturnItem("フォールバック値")
                        .subscribeOn(Schedulers.io()));
    }

    private static Observable<SampleLogLine> zipStreams() {
        Observable<String> labels = Observable.just("赤", "緑", "青");
        Observable<Long> ticks = Observable.interval(400, TimeUnit.MILLISECONDS);
        return SampleLogs.materialize(
                Observable.zip(labels, ticks, (label, tick) -> tick + ": " + label));
    }
}
