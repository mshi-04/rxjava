# data 層

サンプルの定義と、`SampleRepository` の実装を置く。
RxJava の記述はすべてこの層に集まる。

## 依存

| import してよい | してはならない |
| --- | --- |
| `domain`、RxJava 全般 | Android SDK、RxAndroid、`presentation` |

`AndroidSchedulers` は使わない。
表示のためのスレッド移送は `presentation` が最後に付ける。
ここに書くと、観察したい `observeOn` と表示の都合が混ざる。

## クラス

| クラス | 役割 |
| --- | --- |
| `catalog/SampleDefinition` | id・タイトル・説明・実行関数の組 |
| `catalog/SampleCatalog` | 全サンプルの定義と一覧 |
| `catalog/SampleLogs` | ストリームを `SampleLogLine` 列へ変換する道具 |
| `repository/InMemorySampleRepository` | `SampleRepository` の実装 |

## SampleDefinition

`domain` の `Sample` との違いは、実行関数 (`Body`) を持つこと。
`toSample()` で表示用の `Sample` へ落とす。

この分離により、`presentation` は一覧を描くのに
サンプルの実装を一切参照しない。

## SampleCatalog

サンプルの唯一の定義場所。`DEFINITIONS` は不変リストで、登録順が一覧の表示順になる。

サンプル 1 本は private static メソッド 1 つと、一覧への 1 行で表す。
画面もレイアウトも増やさないのは、追加コストがそのまま学習の回数を決めるためである。

登録済みのサンプルは [status.md](status.md) を参照。
追加手順は [adding-samples.md](adding-samples.md) にある。

### サンプル本体の規則

- 呼ばれた時点では何も起こさない cold な `Observable` を返す。
  関数の中で `subscribe()` を呼んだり、スレッドを起こしたりしない
- `subscribeOn` / `observeOn` は書いてよい。これは観察対象そのものなので隠さない
- `AndroidSchedulers.mainThread()` は書かない
- 出力は `SampleLogLine` だけ。`android.util.Log` と `System.out` は使わない

## SampleLogs

変換の道具を 2 系統だけ持つ。

| メソッド | 使いどころ |
| --- | --- |
| `materialize(source)` | オペレータの結果を見せたいとき。`onNext` / `onError` / `onComplete` を 1 行ずつに変換する |
| `create(work)` | 手続き的に、書いた順にログを出したいとき |

`materialize` は `Observable` と `Flowable` の両方を受ける。
`Flowable` 版は `materialize()` を先に適用してから `toObservable()` するため、
上流の backpressure 制御はそのまま働く。

`create` が返すのも cold な `Observable` で、購読ごとにブロックを最初から実行する。

### Subject を立てて合流させない

`PublishSubject` を別に用意して `mergeWith` で本流に合流させる書き方は使わない。
Subject は hot なため、合流先が購読を始める前に流れた行が落ちる。
本流を非同期にすると、この取りこぼしはタイミング次第で再現しなくなる。

手続き的にログを出したいときは `create` を使う。

## InMemorySampleRepository

`LinkedHashMap` で定義を保持し、登録順を一覧の順序として維持する。

`run(id)` は実行関数を `Observable.defer` で包む。
実行ごとにカウンタなどの状態を持つサンプルがあるため、
購読のたびに作り直されることを保証する必要がある。

未登録の `SampleId` には `IllegalArgumentException` を投げる。
画面遷移は自前の識別子でしか起きないため、これは回復対象ではなく実装の誤りである。

## 変更するとき

サンプルの出所を差し替えたくなったら（ファイルから読む、リモートから取る）、
`SampleRepository` の別実装を作り、`di/ServiceLocator` の 1 行を変える。
`domain` と `presentation` は触らない。
