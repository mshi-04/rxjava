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
| `di` | `ServiceLocator` |

レイアウト 2 枚（`activity_forecast.xml` / `item_forecast.xml`）、
`AndroidManifest.xml`（`INTERNET` 権限）、`strings.xml`、`themes.xml`、Gradle 構成。

テストは `app/src/test/` に 6 クラス 31 件。JUnit Jupiter 6.1.3。
`ForecastMapperTest` と、値オブジェクト 5 種のテスト。書き方は [unit-test.md](unit-test.md)。

## 検証済み

| 事項 | 結果 |
| --- | --- |
| `./gradlew assembleDebug` | 通る |
| `./gradlew testDebugUnitTest` | 31 件すべて成功 |
| 7Timer! API の応答 | `civillight` で東京の 7 日分が返ることを確認済み。認証・キーは不要 |
| 層の境界 | `domain` と `data` に `android.*` / `androidx.*` の import がないことを確認 |

## 未検証

実機・エミュレータでの確認をしていない。

- アプリが起動して予報が表示されるか
- 読み込み中・成功・失敗の 3 状態が意図どおり出し分けられるか
- 通信断のときに再試行ボタンで取り直せるか
- 画面回転で再通信が走らないか（`savedInstanceState` のガードが効いているか）
- DataBinding の `@string/name(args)` 形式と、暗黙の `context` を使った
  `ForecastFormatter` の解決が実機で動くか
- `release` ビルド（`assembleRelease`）。R8 と Moshi のリフレクションの相性は未確認

## 次の一手

1. エミュレータで起動し、上の「未検証」を潰す
2. 潰れたところでコミットする
