# サンプルを追加する

触るのは `data/catalog/SampleCatalog.java` の 1 つだけ。
domain・presentation・レイアウト・マニフェストの変更は要らない。

## 手順

### 1. 実行関数を書く

`SampleCatalog` に private static メソッドを足す。
シグネチャは全サンプル共通で固定。

```java
private static Observable<SampleLogLine> myNewSample() {
    return SampleLogs.materialize(
            Observable.just("a", "b"));
}
```

`SampleLogs.materialize` が `onNext` / `onError` / `onComplete` を
ログ 1 行ずつに変換する。素の値を流すだけならこれで足りる。

### 2. 一覧に登録する

`DEFINITIONS` の末尾に 1 行足す。

```java
new SampleDefinition(
        SampleId.of("my-new-sample"),
        "サンプルのタイトル",
        "何を観察するサンプルなのかを 1 文で",
        SampleCatalog::myNewSample)
```

以上。ビルドすれば一覧に出る。

## 手続き的にログを出したいとき

オペレータの結果ではなく、書いた順にログを出したいサンプルは
`SampleLogs.create` を使う。

```java
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
```

`create` が返すのは cold な `Observable` で、購読ごとにブロックが最初から実行される。

`PublishSubject` を別に立てて `mergeWith` で合流させる書き方は使わない。
Subject は hot なため、合流先が購読を始める前に流れた行が落ちる。
本流を非同期にすると、この取りこぼしはタイミング次第で再現しなくなる。

## どちらの道具でも表せないとき

`map` の中で `SampleLogLine.onCurrentThread` を直に呼ぶ。

これが要るのは 2 つの場合に限る。
終端しないストリーム（`interval` など）で `materialize` が意味を持たないとき、
または値そのものではなく値に付随する状態（破棄件数など）を見せたいとき。
既存の `intervalWithDisposable` と `flowableBackpressure` がその例。

道具を増やす代わりの逃げ道なので、先に `materialize` か `create` で書けないかを確認する。

## 2 つのストリームを並べて見せたいとき

同じ元ストリームを条件を変えて 2 回流し、`concatWith` でつなぐ。
元が cold なら 2 回購読しても副作用は起きない。

```java
private static Observable<SampleLogLine> schedulerSwitching() {
    Observable<String> onIo = Observable.fromCallable(() -> "値")
            .subscribeOn(Schedulers.io());

    return SampleLogs.materialize(onIo)
            .concatWith(SampleLogs.materialize(onIo.observeOn(Schedulers.computation())));
}
```

## 守ること

### cold な Observable を返す

`SampleCatalog` の関数は、呼ばれた時点では何も起こしてはならない。
購読して初めて動くストリームを返す。
関数の中で `subscribe()` を呼んだり、スレッドを起こしたりしない。

購読と破棄は `SampleRunViewModel` が引き受ける。
サンプル側が `Disposable` を扱う必要はない。

### Scheduler はサンプルの中に書く

`subscribeOn` / `observeOn` はこのアプリの観察対象そのものなので、
サンプル本体に書いて隠さない。

```java
.subscribeOn(Schedulers.io())
```

一方で `AndroidSchedulers.mainThread()` は書かない。
表示のためのスレッド移送は presentation 側が最後に付ける。
サンプルに書くと、観察したい `observeOn` と表示の都合が混ざる。

### スレッド名は自分で書かない

`SampleLogLine.onCurrentThread()` が放出時のスレッド名を確定させる。
メッセージ側に書くと二重になる。

```java
SampleLogLine.onCurrentThread("onNext: " + value);
```

```java
// 冗長。スレッド名は自動で付く
SampleLogLine.onCurrentThread("[" + Thread.currentThread().getName() + "] onNext: " + value);
```

### 出力は SampleLogLine だけを使う

`android.util.Log` と `System.out` は使わない。
画面に出ないログはこのアプリでは存在しないのと同じになる。

### Android SDK を参照しない

`data/catalog` から `Context` や `View` を触らない。
`AndroidSchedulers` も参照しない。

### 無限に流れるサンプルは停止を確認する

`interval` や `repeat` を使うサンプルは、停止ボタンで実際に止まること、
画面を戻ったときに購読が残らないことを実機かエミュレータで確認してから足す。

### SampleId は安定させる

`SampleId` は画面遷移で受け渡す識別子である。
一覧の並び順に依存しない文字列を使い、一度公開したら変えない。

## 説明文の書き方

タイトルは検索しやすいようオペレータ名を含める。
説明は「何が観察できるか」を 1 文で書く。実装の説明は書かない。

```
"Flowable と backpressure"
"生産が消費より速いときに onBackpressureDrop が値を捨てる様子を見る"
```

```
"Flowable のサンプル"
"Flowable.interval に onBackpressureDrop と observeOn をつなぐ"
```

上が良い例、下が悪い例。下はコードを読めば分かることしか言っていない。
