# RxJava Samples

RxJava 3 の挙動を画面上のログで観察する学習用 Android アプリ。

サンプルを一覧から選び、実行すると各イベントがスレッド名つきの時系列ログとして流れる。
オペレータの結果だけでなく、「どのスレッドで」「どの順に」「いつ終わるか」を見ることが目的。

## 動かす

```bash
./gradlew assembleDebug
```

```bash
android emulator start Pixel_10
```

```bash
android run
```

## 構成

Java 17 + View (XML) + ViewDataBinding。Clean Architecture の 3 層で、
`presentation` の内部は MVVM。

| 層 | 役割 |
| --- | --- |
| `domain` | エンティティ、UseCase、Repository インタフェース。Android 非依存 |
| `data` | Repository 実装と、サンプル定義 |
| `presentation` | Activity、ViewModel、Adapter |
| `di` | ServiceLocator |

サンプルを 1 本増やすときに触るのは `data/catalog/SampleCatalog.java` だけ。

## 文書

| 文書 | 扱う内容 |
| --- | --- |
| [docs/status.md](docs/status.md) | 実装済み・未検証。作業前にこれを読む |
| [docs/architecture.md](docs/architecture.md) | 層と依存の向き、全体設計 |
| [docs/domain.md](docs/domain.md) | domain 層の詳細 |
| [docs/data.md](docs/data.md) | data 層の詳細 |
| [docs/presentation.md](docs/presentation.md) | presentation 層の詳細 |
| [docs/adding-samples.md](docs/adding-samples.md) | サンプルを 1 本足す手順 |
| [docs/development.md](docs/development.md) | 環境、ビルド、依存更新、git 運用 |

[CLAUDE.md](CLAUDE.md) は AI 向けの地図で、内容はこの README と文書群への案内に限る。
