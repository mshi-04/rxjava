# 現状

最終更新: 2026-08-16

## この版で変わったこと

RxJava のサンプルカタログ機能を全廃した。
アプリは 7Timer! の API から東京の 7 日予報を取得して表示する 1 画面になった。

削除したもの: `data/catalog/`、`InMemorySampleRepository`、`Sample` 系のモデルと UseCase、
`presentation/list/`、`presentation/run/`、対応するレイアウトとテスト、
`docs/adding-samples.md`、`add-rx-sample` スキル。

## 実装済み

| 層 | クラス |
| --- | --- |
| `domain/model` | `Location`, `ForecastDate`, `Temperature`, `TemperatureRange`, `WindSpeed`, `WeatherCondition`, `DailyForecast` |
| `domain/repository` | `ForecastRepository` |
| `domain/usecase` | `GetForecastUseCase` |
| `data/remote` | `ForecastApi`, `ForecastResponse`, `ForecastMapper` |
| `data/repository` | `RemoteForecastRepository` |
| `presentation` | `activity/ForecastActivity`, `viewmodel/ForecastViewModel`, `adapter/ForecastAdapter`, `util/ForecastFormatter` |
| ルート | `ForecastApplication`（`@HiltAndroidApp`） |
| `di` | `NetworkModule`, `RepositoryModule` |

レイアウト 2 枚（`activity_forecast.xml` / `item_forecast.xml`）、
`AndroidManifest.xml`（`INTERNET` 権限）、`strings.xml`、`themes.xml`、Gradle 構成。

テストは 2 系統。書き方は [unit-test.md](unit-test.md) と [ui-test.md](ui-test.md)。

| 種別 | 内容 |
| --- | --- |
| JVM Unit Test | `app/src/test/` に 6 クラス 31 件。JUnit Jupiter 6.1.3 |
| UI テスト | `app/src/androidTest/` に `ForecastActivityTest` 3 件。JUnit 4 + Espresso + Hilt |

UI テストは `@TestInstallIn` で `RepositoryModule` を fake へ置き換え、通信を使わない。

## 検証済み

| 事項 | 結果 |
| --- | --- |
| `./gradlew assembleDebug` | 通る |
| `./gradlew testDebugUnitTest` | 31 件すべて成功 |
| `./gradlew connectedDebugAndroidTest` | 3 件すべて成功（Pixel_10 / API 37）。下記の注意あり |
| 7Timer! API の応答 | `civillight` で東京の 7 日分が返ることを確認済み。認証・キーは不要 |
| 層の境界 | `domain` と `data` に `android.*` / `androidx.*` の import がないことを確認 |

## エミュレータで検証済み

Pixel_10（API 37.1）で確認した。

| 事項 | 結果 |
| --- | --- |
| 起動と表示 | 東京の 7 日予報が表示される。API の値と一致 |
| Hilt の注入 | `@AndroidEntryPoint` と `@HiltViewModel` が実行時に結線される |
| 失敗状態 | 通信断で「天気予報を取得できませんでした」と再試行ボタンが出る |
| 再試行 | 通信を戻して再試行を押すと一覧が出る |
| 画面回転で再通信しないか | 通信断のまま回転しても一覧が残る。再取得していない |
| 回転後の一覧保持 | 一度空になる不具合を修正済み（下記） |
| レイアウトの余白 | 先頭の項目がシステムバーと重なる不具合を修正済み（下記） |
| ダーク / ライト | ライトで確認。ダークは未確認 |
| 読み込み中の表示 | 起動直後に出ることを確認。成功時に消えることは UI テストが押さえている |
| 成功・失敗・再試行 | UI テストで自動検証するようにした |
| DataBinding の書式と `ForecastFormatter` | 日付・気温・風速・天気ラベルすべて表示される |

### 見つけて直した不具合

**その 1: 先頭の項目がシステムバーと重なっていた。**
`targetSdk` 36 で edge-to-edge が強制され、コンテンツがシステムバーの裏から
描画されていた。さらに ActionBar の `android:label` と画面内のタイトルが二重に出ていた。
テーマを `NoActionBar` にし、ルートへ `fitsSystemWindows` を付け、
`windowLightStatusBar` を明暗で出し分けるようにした。

**その 2: 回転すると一覧が消えていた。**
`OnPropertyChangedCallback` は値が変わったときにしか発火しないが、
回転では ViewModel が値を保持したままなので変更が起きず、
作り直された Adapter へ何も届いていなかった。

登録直後に現在値を流し込むようにし、あわせて `onDestroy()` で解除するようにした。
解除しないと古い Adapter への参照が回転のたびに積み上がる。

## UI テストが全件落ちるとき

`RootViewWithoutFocusException`（ウィンドウがフォーカスを得られない）で
3 件とも落ちることがある。この症状はコードではなくエミュレータ側である。

見分け方は 2 つ。

- `android screen capture` が真っ黒なのに `android layout -p` は正常な階層を返す
- `git stash` して直前のコミットで実行しても同じ 3 件が同じ理由で落ちる

キーガード解除、画面の常時点灯、エミュレータの再起動では直らなかった。
スナップショットを破棄したコールドブートか、AVD の作り直しが要る。

## 未検証

- 失敗したときに `loading` が戻るか。成功時は UI テストが押さえている
  （一覧の表示条件が `loading` と `hasError` の両方を見るため）が、
  失敗時はエラー表示だけを検証しており `ProgressBar` が消えたかを見ていない
- ダークテーマの表示。`windowLightStatusBar` を `values-night/` で出し分けたが未確認

`release` ビルドは対象外。理由は [development.md](development.md)。

## 次の一手

1. 成功後の再取得手段。再試行ボタンは失敗時にしか出ないため、
   一度表示できると force-stop するまで更新できない
2. 失敗時に `ProgressBar` が消えることを UI テストの検証に足す
3. ダークテーマで表示を確認する
