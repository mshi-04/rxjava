# CLAUDE.md

7Timer! の API から東京の 7 日予報を取得して表示する Android アプリ。
Java 17 + View (XML) + ViewDataBinding、Clean Architecture の 3 層で presentation は MVVM。

**作業前に [docs/status.md](docs/status.md) を読む。** 未検証の前提が残っている。

この文書は地図であり、詳細を持たない。正本は docs/ 配下。

| 知りたいこと | 読む文書 |
| --- | --- |
| 全体設計、層と依存の向き | [architecture.md](docs/architecture.md) |
| 層ごとの詳細 | [domain.md](docs/domain.md) / [data.md](docs/data.md) / [presentation.md](docs/presentation.md) |
| 環境、ビルド、外部 API、依存更新、git 運用 | [development.md](docs/development.md) |

コードは `app/src/main/java/com/example/rxjava/` 配下に
`domain/` `data/` `presentation/` `di/`。
依存とバージョンは `gradle/libs.versions.toml` にだけ書く。

Retrofit と Moshi の型は `data/remote/` の外へ出さない。
`AndroidSchedulers` を書いてよいのは `presentation` だけ。

作業の種類ごとに `.claude/skills/` にスキルがある。該当するものは必ず使う。

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

設計を変えるときは docs を先に更新する。設計が正で、コードがそれに従う。
