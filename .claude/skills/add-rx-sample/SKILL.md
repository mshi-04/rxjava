---
name: add-rx-sample
description: RxJava のサンプルを 1 本追加する依頼で使う。「サンプルを追加」「〜のサンプルを作って」「〜の挙動を見たい」と言われたとき、オペレータ・Scheduler・Subject・backpressure・エラーハンドリングなど RxJava の挙動を画面で観察したい要望はすべて対象。data 層の仕組みそのものを変えるときは data-implementation を使う。
---

# 目的

RxJava Samples に観察対象を 1 本足す。触るのは `data/catalog/SampleCatalog.java` だけで、
画面・レイアウト・マニフェスト・`domain`・`presentation` は変更しない。

# 最初に確認するもの

- [`docs/adding-samples.md`](../../../docs/adding-samples.md)
- [`docs/data.md`](../../../docs/data.md)
- [`app/src/main/java/com/example/rxjava/data/catalog/SampleCatalog.java`](../../../app/src/main/java/com/example/rxjava/data/catalog/SampleCatalog.java)
- [`docs/status.md`](../../../docs/status.md) の登録済み一覧

# 手順

1. 何を観察させるかを 1 文で言えるまで決める。「値が何になるか」ではなく「どう流れるか」を見せる。
2. `SampleCatalog` に `private static Observable<SampleLogLine> xxx()` を足す。変換の道具は下表の 2 つだけ。
3. `DEFINITIONS` の末尾に `new SampleDefinition(...)` を 1 行足す。`SampleId` は並び順に依存しない安定した文字列にする。
4. 有限で同期的なサンプルなら `app/src/test/` にテストを足す。無限・時間依存ならテストせず、エミュレータで確認する。
5. `./gradlew assembleDebug` と `./gradlew testDebugUnitTest` を実行し、必要なら `android run` で動かす。task 一覧の正本は [`docs/development.md`](../../../docs/development.md)。
6. [`docs/status.md`](../../../docs/status.md) のサンプル一覧に 1 行足す。
7. 変更内容、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# ログ行の作り方

| 作り方 | 使いどころ |
| --- | --- |
| `SampleLogs.materialize(source)` | オペレータの結果を見せる。`onNext` / `onError` / `onComplete` が 1 行ずつ出る |
| `SampleLogs.create(work)` | 手続き的に、書いた順にログを出す |
| `map` の中で `SampleLogLine.onCurrentThread` を直に呼ぶ | 上の 2 つでは表せないとき |

まず上の 2 つで書けないかを確認する。
3 つ目が要るのは、終端しないストリーム（`interval` など）や、
値そのものではなく値に付随する状態（破棄件数など）を見せたいときに限る。
既存の `intervalWithDisposable` と `flowableBackpressure` がその例。

同じ元ストリームを条件を変えて 2 回流して比べたいときは `concatWith` でつなぐ。
元が cold なら 2 回購読しても副作用は起きない。

# 守ること

| 規則 | 理由 |
| --- | --- |
| cold な `Observable` を返す。関数の中で `subscribe()` を呼ばない | 購読と破棄は `SampleRunViewModel` が持つ |
| `subscribeOn` / `observeOn` はサンプル本体に書く | 観察対象そのものなので隠さない |
| `AndroidSchedulers.mainThread()` は書かない | 表示のための移送は presentation が付ける |
| スレッド名をメッセージに書かない | `SampleLogLine.onCurrentThread()` が放出時に付ける |
| `android.util.Log` と `System.out` を使わない | 画面に出ないログは存在しないのと同じ |
| `Context` や `View` を触らない | data 層は Android 非依存 |
| 実行ごとの状態はメソッド内のローカル変数で持つ | `Observable.defer` で購読ごとに作り直される |

## Subject を立てて合流させない

`PublishSubject` を別に用意して `mergeWith` で本流に合流させる書き方は使わない。
Subject は hot なため、合流先が購読を始める前に流れた行が落ちる。
本流を非同期にすると、この取りこぼしはタイミング次第で再現しなくなる。
手続き的にログを出したいときは `SampleLogs.create` を使う。

# タイトルと説明

タイトルは検索できるようオペレータ名を含める。
説明は「何が観察できるか」を 1 文で書く。実装の説明は書かない。

| 良い | 悪い |
| --- | --- |
| `"Flowable と backpressure"` / `"生産が消費より速いときに onBackpressureDrop が値を捨てる様子を見る"` | `"Flowable のサンプル"` / `"Flowable.interval に onBackpressureDrop と observeOn をつなぐ"` |

悪い例はコードを読めば分かることしか言っていない。

# 注意事項

- 無限に流れるサンプルは、停止ボタンで実際に止まること、画面を戻ったときに購読が残らないことを
  エミュレータで確認してから完了とする。
- `SampleLogs` に新しい道具を足したくなったら、それは `data-implementation` の領分。
  サンプル追加の手順を分岐させる前に、既存の書き方で表せないかを確認する。
- サンプルの追加で `domain` や `presentation` を触る必要が出たら、設計の前提が崩れている。
  先に [`docs/architecture.md`](../../../docs/architecture.md) を見直す。
