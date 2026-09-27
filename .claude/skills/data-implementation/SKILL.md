---
name: data-implementation
description: 7Timer! API との通信、DTO と写像、Room による保存、Repository 実装を変える依頼で使う。東京の天気の data 層の Retrofit インタフェース、Moshi DTO、ForecastMapper、ForecastDataSourceImpl、Room の Entity・DAO・ForecastStoreImpl、CachingForecastRepository を担当する。
---

# 目的

東京の天気の 7Timer! API との通信、予報の保存、`ForecastRepository` の実装を、
外部 API の語彙と DB の行の形が漏れない data 層へ閉じる。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/data.md`](../../../docs/data.md)
- [`app/src/main/java/com/example/rxjava/data/`](../../../app/src/main/java/com/example/rxjava/data/)
- [`app/src/main/java/com/example/rxjava/di/`](../../../app/src/main/java/com/example/rxjava/di/)（通信と DB の組み立て、実装の選択）
- 関連する [`app/src/test/java/com/example/rxjava/data/`](../../../app/src/test/java/com/example/rxjava/data/)

# 役割クラスと参照先

変更対象の役割クラスを見分け、該当する reference だけを読んでから実装する。

| 役割クラス | 参照 |
| --- | --- |
| Retrofit インタフェースと通信の口（`ForecastApi` / `ForecastDataSourceImpl`） | [`references/retrofit-api.md`](references/retrofit-api.md) |
| DTO と写像（`ForecastResponse` / `ForecastMapper`） | [`references/dto-and-mapper.md`](references/dto-and-mapper.md) |
| Room による保存（Entity / `ForecastDao` / `ForecastEntityMapper` / `ForecastStoreImpl`） | [`references/room-store.md`](references/room-store.md) |
| Repository implementation（`CachingForecastRepository`） | [`references/repository-implementation.md`](references/repository-implementation.md) |

# 手順

1. 変更対象の役割クラスを判定し、該当 reference を読む。
2. `presentation -> domain <- data` の依存方向を保つ。data から presentation を参照しない。
3. reference の観点に沿って実装する。
4. 変更した振る舞いをテストで押さえる。通信は MockWebServer、キャッシュ判定は手書きの fake で JVM Unit Test にし、
   実際の DB は instrumented test にする。外部 API には接続しない。
   どちらで検証するかは [`docs/unit-test.md`](../../../docs/unit-test.md) の「JVM で検証できる範囲」に従う。
5. `./gradlew testDebugUnitTest` と `./gradlew assembleDebug` を実行する。task 一覧の正本は [`docs/development.md`](../../../docs/development.md)。
6. 変更内容、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- data 層に `Context`、`View`、RxAndroid（`AndroidSchedulers`）を持ち込まない。
  表示のためのスレッド移送は presentation が付ける。
- Retrofit と Moshi の型を `data/remote/` の外へ、Room の Entity を `data/local/` の外へ出さない。
  `data/remote/` を参照してよいのは `ForecastDataSourceImpl`（`data/datasource/`）と `di/` だけにする。
  `ForecastResponse` や JSON の項目名、テーブルの行が Repository の戻り値に現れたら境界が漏れている。
- DB 実行基盤への `subscribeOn` は `data/local/` だけに許す。`repository/`・`datasource/`・`remote/` は Scheduler を指定しない。
- API の語彙（天気コードの文字列、JSON のキー名）を知ってよいのは `ForecastMapper` だけにする。
- `dataseries` のうち写せない 1 日分だけを飛ばし、利用できる日を返す。
  `dataseries` 自体が欠けている場合、または要素があるのに 1 件も写せない場合だけ例外にする。
  全件を写せない例外には最初の日別変換例外を cause として残し、診断情報を捨てない。
- Repository、`ForecastDataSource`、`ForecastStore` が返すストリームは cold に保つ。
  呼ばれただけで通信や DB 操作が動き出す実装にしない。
- 通信の失敗は `onError` としてそのまま上げる。`onErrorReturn` などで握り潰さない。
- 契約を変えるときは `domain` の `ForecastRepository` を先に見直し、`domain-implementation` を併用する。
