# CLAUDE.md

RxJava 3 の挙動を画面上のログで観察する学習用 Android アプリ。
Java 17 + View (XML) + ViewDataBinding、`applicationId` は `com.example.rxjava`。

この文書は地図であり、詳細は持たない。
プロジェクトの概要は [README.md](README.md)、詳細は [docs/](docs/) を正本とする。

**作業を始める前に [docs/status.md](docs/status.md) を読む。**
未検証の前提が残っている。

## 文書の地図

| 知りたいこと | 読む文書 |
| --- | --- |
| 今どこまで出来ているか、何が未検証か | [docs/status.md](docs/status.md) |
| サンプルを 1 本足したい | [docs/adding-samples.md](docs/adding-samples.md) |
| 層と依存の向き、全体設計 | [docs/architecture.md](docs/architecture.md) |
| domain 層の詳細 | [docs/domain.md](docs/domain.md) |
| data 層の詳細 | [docs/data.md](docs/data.md) |
| presentation 層の詳細 | [docs/presentation.md](docs/presentation.md) |
| 環境、技術構成、ビルド、実行、依存更新、git 運用 | [docs/development.md](docs/development.md) |

## コードの地図

Clean Architecture の 3 層。依存は外から内への一方向。

| 場所 | 役割 |
| --- | --- |
| `.../rxjava/domain/` | エンティティ、UseCase、Repository インタフェース。Android 非依存 |
| `.../rxjava/data/` | Repository 実装と、サンプル定義（`catalog/`） |
| `.../rxjava/presentation/` | Activity、ViewModel、Adapter。内部は MVVM |
| `.../rxjava/di/` | ServiceLocator。実体の組み立てはここだけ |
| `app/src/main/res/layout/` | DataBinding のレイアウト 3 枚 |
| `gradle/libs.versions.toml` | 依存とバージョンの唯一の定義場所 |

サンプルを増やすときに触るのは `data/catalog/SampleCatalog.java` だけで足りる。
手順は `/add-rx-sample` スキルにある。

## 最低限のコマンド

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

## 変更するとき

設計を変えるときは [docs/architecture.md](docs/architecture.md) と
該当する層の文書を先に更新する。設計が正で、コードがそれに従う。
