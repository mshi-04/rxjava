# 現状

最終更新: 2026-08-15

## リポジトリの状態

コミット済みは `.gitignore` / `.gitattributes` の初回コミットのみ。
それ以外はすべて未追跡のまま作業ツリーにある。

## 実装済み

| 層 | クラス |
| --- | --- |
| `domain/model` | `SampleId`, `Sample`, `SampleLogLine` |
| `domain/repository` | `SampleRepository` |
| `domain/usecase` | `GetSampleListUseCase`, `GetSampleUseCase`, `RunSampleUseCase` |
| `data/catalog` | `SampleDefinition`, `SampleLogs`, `SampleCatalog` |
| `data/repository` | `InMemorySampleRepository` |
| `presentation/list` | `MainActivity`, `SampleListViewModel`, `SampleListAdapter` |
| `presentation/run` | `SampleActivity`, `SampleRunViewModel` |
| `di` | `ServiceLocator` |

レイアウト 3 枚（`activity_main.xml` / `item_sample.xml` / `activity_sample.xml`）、
`AndroidManifest.xml`、`strings.xml`、`themes.xml`、Gradle 構成も作成済み。

登録済みのサンプル 7 本:

| SampleId | タイトル |
| --- | --- |
| `basic-observable` | Observable の基本 |
| `scheduler-switching` | Scheduler の切り替え |
| `interval-disposable` | interval と Disposable |
| `flowable-backpressure` | Flowable と backpressure |
| `behavior-subject` | BehaviorSubject の購読タイミング |
| `error-handling` | エラーハンドリングと retry |
| `zip-streams` | zip による合成 |

## 検証済み

| 事項 | 結果 |
| --- | --- |
| `./gradlew assembleDebug` | 通る。AGP 9.0.1 + DataBinding + 純 Java の組み合わせで問題なし |
| `configuration-cache` 有効のままの DataBinding ビルド | 通る（`Configuration cache entry stored` を確認） |
| 層の境界 | `domain` と `data` に `android.*` / `androidx.*` の import がないことを確認 |

## 未検証

いずれも実機・エミュレータでの確認をしていない。

- アプリが起動して一覧が表示されるか
- 各サンプルのログが期待どおり出るか。特に
  `flowable-backpressure` の破棄件数の伸び方と、
  `scheduler-switching` で 2 ブロックのスレッド名が実際に異なるか
- `interval-disposable` を実行したまま画面回転させたときに購読が重複しないか
- 停止ボタンで無限ストリームが実際に止まるか
- release ビルド（`assembleRelease`）

## 次の一手

1. エミュレータで起動し、上の「未検証」を潰す
2. 潰れたところでコミットする
