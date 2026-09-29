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
| 7 日分の成功 | 入力順の 7 行に、日付・天気・最高／最低気温・風速が表示される |
| スクロールと再表示 | 最終行までスクロールして先頭へ戻っても、行の内容が混ざらない |
| 未知の天気 | `UNKNOWN` は「不明」と表示され、ほかの項目も表示される |
| 空の成功結果 | 一覧に予報行がなく、読み込み表示とエラー表示も出ない |
| 失敗 | エラー文言と再試行ボタンが表示され、一覧が出ない |
| 再試行 | 失敗表示から再試行を押すと一覧に変わる |
| Activity 再生成 | 表示を維持し、取得を増やさない |
| Fragment の View 再生成 | `detach` / `attach` 後も表示を維持し、取得を増やさない |
| View が無い間の応答 | 応答をメインスレッドで処理してから再表示し、結果を描画する |

`ObservableBoolean` の値や `Disposable` の状態は検証しない。
それらは実装の詳細であり、画面から見えるものが契約である。

## テーマと文字サイズの検証

ライト／ダークと文字サイズ 100％／200％を組み合わせた 4 条件で画面テストを実行する。
画面は既存の fake Repository で作り、実 API の可用性に依存させない。
`RecyclerView` のスクロールはテスト専用の `ViewAction` で行い、Espresso のメインスレッド同期を使う。
行の位置と、その行に描画された全項目を確認する。固定時間の待機やテスト依存の追加はしない。

通常のテストでは画像を保存しない。検証画像が必要な実行だけ、次の引数を渡す。
引数は画像の保存名を指定するもので、テーマと文字サイズは変えない。
検証前に `adb shell cmd uimode night` と `adb shell settings get system font_scale` で元の設定を控え、
各条件の設定を端末へ適用してからテストする。

| 条件 | `adb shell cmd uimode night` に渡す値 | `adb shell settings put system font_scale` に渡す値 | `uiValidationRun` |
| --- | --- | --- | --- |
| ライト・100％ | `no` | `1.0` | `light-100` |
| ライト・200％ | `no` | `2.0` | `light-200` |
| ダーク・100％ | `yes` | `1.0` | `dark-100` |
| ダーク・200％ | `yes` | `2.0` | `dark-200` |

ライト・100％の例:

```bash
adb shell cmd uimode night no
adb shell settings put system font_scale 1.0
./gradlew connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.uiValidationRun=light-100'
```

`uiValidationRun` の値は条件ごとに `light-100`、`light-200`、`dark-100`、`dark-200` とする。
成功（先頭・最終行・先頭へ戻った後）、読み込み中、失敗の画面を `UiAutomation.takeScreenshot()` で保存する。
撮影前は Espresso とメインスレッドの同期に加え、`UiAutomation.waitForIdle(100, 5000)` で
accessibility イベントが静まるのを最大 5 秒待つ。スクロール直後の未反映フレームを保存しないためで、
イベントが続いて画面が安定しない場合は画像取得を失敗させる。
画像は Gradle が runner 引数 `additionalTestOutputDir` で指定する領域の `ui-validation/<条件>/` に置く。
Gradle がテスト後のアプリ削除前に `app/build/outputs/connected_android_test_additional_output/` へ回収するため、
実行ごとに画像とテスト結果を `app/build/reports/ui-validation/<条件>/` へコピーして残す。git 管理しない。
`uiValidationRun` を指定した画像取得は、この回収機構を持つ Gradle の端末テストで実行する。

画像では文字の欠け・重なり、エラー文言、再試行ボタン、ステータスバーの視認性を確認する。
再試行の操作性は既存の再試行テストでも確認する。エミュレータのテーマと文字サイズは、
検証前の値を控え、成功・失敗にかかわらず検証後に戻す。
確認した端末・OS・条件と結果は [development.md](development.md) に記録する。

## ForecastLocalDataSourceImpl を実際の DB で検証する

画面ではないが、実際の SQLite を要するためここに置く。
`ForecastLocalDataSourceImplTest` はテストごとに一意な名前のファイル DB を作り、終了時に消す。
ネットワークは使わず、`AndroidJUnit4` ランナーで実行する。

| 対象 | 見るもの |
| --- | --- |
| 復元 | DB を開き直しても全フィールドと一覧の順序が戻る |
| 置換 | 同じ地点の保存は前の行を消し、他の地点の行は残す |
| 空一覧 | 取得済みの空一覧と未取得（empty）を区別する |
| ロールバック | 日時の書き込みが失敗したら予報の置換も戻る |
| cold | 購読するまで DB を開かない |
| 入力のコピー | `save` を呼んだ後で呼び出し元が一覧を変えても、保存内容は変わらない |
| 開始前の解除 | 実行を保留する transaction executor を渡し、実行前に解除すると何も書かれない |
| 開始後の解除 | トランザクションの途中で解除しても通知はなく、予報と日時が両方とも更新される |

read と save は `TestObserver` で購読し、終了待ちを 5 秒に制限する。

## 書き方

命名は [unit-test.md](unit-test.md) と同じ `対象_条件_期待結果` の 3 部構成。
AAA のコメントも同じく置く。

`ActivityScenario.launch` は fake の応答を設定したあとに呼ぶ。
起動時に取得が走るため、順序を逆にすると意図した応答にならない。
Fragment の View を作り直すテストは、`FragmentManager` の `detach` / `attach` を
それぞれ `commitNow()` で完了させる。View が無い間の応答では、fake を完了した後に
メインスレッドが idle になったことを確認してから `attach` する。
これにより破棄済み View への通知を、再表示が覆い隠さない。

## プロセス death からの復帰は手で確かめる

自動テストでは再現できない。`ActivityScenario.recreate()` は ViewModel を保持するため
設定変更しか再現せず、Bundle があるのに ViewModel が新品になる経路を通らない。

エミュレータで次の順に確かめる。
ビルドとインストールの手順は [development.md](development.md) の「エミュレータでの実行」に従う。

1. アプリを起動し、一覧が出るのを待つ
2. HOME でバックグラウンドへ回す
3. プロセスを殺す。タスクは残る

```bash
adb shell input keyevent KEYCODE_HOME
```

```bash
adb shell am kill com.example.rxjava
```

```bash
adb shell pidof com.example.rxjava
```

`pidof` が何も返さなければ殺せている。

4. ランチャーから起動し直す
5. `dumpsys activity activities` で復元された Activity と task を確認する。
   新しい Activity が元の Activity の上に積まれた場合だけ **BACK を 1 回押す**
6. `pidof` の値が殺す前と異なり、元の Activity で一覧が出ることを確かめる

```bash
adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.example.rxjava/.presentation.activity.ForecastActivity
```

```bash
adb shell input keyevent KEYCODE_BACK
```

```bash
android layout -p
```

`am start` は環境によって元の task をそのまま前面に戻す場合と、
新しい Activity を上に積む場合がある。前者では BACK を押さない。
後者では BACK で上の Activity を閉じると、下に残っていた Activity が
Bundle から作り直され、新品の ViewModel と組み合わさる。

どちらの Activity が前面にいるかは次で見分ける。
`Hist #0` が殺される前からある Activity で、`Resumed` がそれと一致していればよい。
プロセス ID の変化と、表示された一覧も一緒に確認する。

```bash
adb shell dumpsys activity activities
```

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
