# AGENTS.md

7Timer! の API から東京の 7 日予報を取得して表示する Android アプリ。
Java 17 + View (XML) + ViewDataBinding、Clean Architecture の 3 層で presentation は MVVM。

この文書は作業の入口であり、設計と手順の正本は `docs/` 配下に置く。
作業に関係する文書を読み、未検証の前提に関わる変更では
[development.md](docs/development.md) の「未確認のまま残っていること」を確認する。

| 作業内容 | 読む文書 |
| --- | --- |
| 全体設計、層と依存の向き | [architecture.md](docs/architecture.md) |
| 層ごとの実装 | [domain.md](docs/domain.md) / [data.md](docs/data.md) / [presentation.md](docs/presentation.md) |
| JVM テスト | [unit-test.md](docs/unit-test.md) |
| 画面の振る舞いの検証 | [ui-test.md](docs/ui-test.md) |
| 環境、ビルド、外部 API、依存更新、git 運用 | [development.md](docs/development.md) |

コードは `app/src/main/java/com/example/rxjava/` 配下の
`domain/` `data/` `presentation/` `di/` に分ける。
依存とバージョンは `gradle/libs.versions.toml` にだけ書く。

`presentation -> domain <- data` の依存方向を保つ。
Retrofit と Moshi の型は `data/remote/` の外へ出さない。
`AndroidSchedulers` を書いてよいのは `presentation` だけ。

作業に該当する `.agents/skills/` の Skill を使う。
`domain-implementation`、`data-implementation`、`presentation-implementation` は各層の変更、
`test-implementation` はテストの追加・修正、`implementation-review` は実装差分のレビューに使う。

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

設計を変えるときは、該当する `docs/` を先に更新する。設計が正で、コードがそれに従う。
