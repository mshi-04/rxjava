# UI テスト

`app/src/androidTest/` に置く instrumented test の正本。
エミュレータ上で画面の振る舞いを検証する。

JVM で完結するテストは [unit-test.md](unit-test.md) を参照。

## 構成

| 項目 | 値 |
| --- | --- |
| フレームワーク | JUnit 4 + Espresso |
| 実行 | `./gradlew connectedDebugAndroidTest` |
| DI | Hilt。`@TestInstallIn` で Repository を fake へ差し替える |

**JUnit 4 を使う。** JVM 側は Jupiter だが、Espresso と `AndroidJUnit4` ランナーが
JUnit 4 のルール機構に依存しており、揃えられない。
ディレクトリで使い分けが決まるため、混乱は起きない。

## 通信を実際に行わない

`ForecastRepository` を fake に差し替え、ネットワークを一切使わない。

実 API を叩くと、回線の状態と API の可用性でテストが落ちる。
検証したいのは画面の振る舞いであって、通信の成否ではない。

`androidTest` の `FakeRepositoryModule` が `@TestInstallIn` で
`RepositoryModule` を置き換える。fake は `@Singleton` で、
テストから注入して応答を差し替える。

応答は `Single.defer` で包み、購読時点の設定を読む。
再試行のテストで「1 回目は失敗、2 回目は成功」を作るために要る。

## Scheduler を同期させる

本番コードは `subscribeOn(Schedulers.io())` を通る。
Espresso は RxJava のスレッドを知らないため、そのままだと待ってくれない。

`RxJavaPlugins.setIoSchedulerHandler` で `io` を `trampoline` に差し替え、
購読したスレッドで同期実行させる。`@After` では、このテストが設定したフックだけを
`RxJavaPlugins.setIoSchedulerHandler(null)` で解除する。`RxJavaPlugins.reset()` は
他のテストやライブラリが設定したフックまで消すため使わない。

`AndroidSchedulers.mainThread()` は差し替えない。
差し替えると View の更新がメインスレッド外で走って落ちる。
`observeOn(mainThread)` の post は Espresso がメインの Looper と同期して待つ。

## 何を検証するか

画面から見える振る舞いだけを見る。内部状態は見ない。

| 対象 | 見るもの |
| --- | --- |
| 読み込み中 | 応答が完了しない間、進捗表示が出て一覧とエラー表示が出ない |
| 成功 | 予報の日付・天気・気温が表示される |
| 失敗 | エラー文言と再試行ボタンが表示され、一覧が出ない |
| 再試行 | 失敗表示から再試行を押すと一覧に変わる |

`ObservableBoolean` の値や `Disposable` の状態は検証しない。
それらは実装の詳細であり、画面から見えるものが契約である。

## 書き方

命名は [unit-test.md](unit-test.md) と同じ `対象_条件_期待結果` の 3 部構成。
AAA のコメントも同じく置く。

`ActivityScenario.launch` は fake の応答を設定したあとに呼ぶ。
起動時に取得が走るため、順序を逆にすると意図した応答にならない。

## 全件が落ちるとき

`RootViewWithoutFocusException`（ウィンドウがフォーカスを得られない）で
全件落ちることがある。この症状はコードではなくエミュレータ側である。

見分け方は 2 つ。

- `android screen capture` が真っ黒なのに `android layout -p` は正常な階層を返す
- `git stash` して直前のコミットで実行しても同じテストが同じ理由で落ちる

一度の遭遇では、キーガード解除・画面の常時点灯・エミュレータの再起動では直らなかった。
復旧したのは AVD の作り直しである。

```bash
android emulator remove Pixel_10
```

```bash
android emulator create Pixel_10
```

`android emulator start --cold <名前>` でスナップショットを捨てて起動する手もあるが、
この症状に効くかは確かめていない。
