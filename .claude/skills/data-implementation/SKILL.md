---
name: data-implementation
description: 7Timer! API との通信、DTO と写像、Repository 実装を変える依頼で使う。東京の天気の data 層の Retrofit インタフェース、Moshi DTO、ForecastMapper、RemoteForecastRepository を担当する。
---

# 目的

東京の天気の 7Timer! API との通信と `ForecastRepository` の実装を、
外部 API の語彙が漏れない data 層へ閉じる。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/data.md`](../../../docs/data.md)
- [`app/src/main/java/com/example/rxjava/data/`](../../../app/src/main/java/com/example/rxjava/data/)
- [`app/src/main/java/com/example/rxjava/di/`](../../../app/src/main/java/com/example/rxjava/di/)（通信の組み立て）
- 関連する [`app/src/test/java/com/example/rxjava/data/`](../../../app/src/test/java/com/example/rxjava/data/)

# 役割クラスと参照先

変更対象の役割クラスを見分け、該当する reference だけを読んでから実装する。

| 役割クラス | 参照 |
| --- | --- |
| Retrofit インタフェース（`ForecastApi`） | [`references/retrofit-api.md`](references/retrofit-api.md) |
| DTO と写像（`ForecastResponse` / `ForecastMapper`） | [`references/dto-and-mapper.md`](references/dto-and-mapper.md) |
| Repository implementation（`RemoteForecastRepository`） | [`references/repository-implementation.md`](references/repository-implementation.md) |

# 手順

1. 変更対象の役割クラスを判定し、該当 reference を読む。
2. `presentation -> domain <- data` の依存方向を保つ。data から presentation を参照しない。
3. reference の観点に沿って実装する。
4. 変更した振る舞いを JVM Unit Test で押さえる。実ネットワーク呼び出しを伴う経路は対象外とし、
   `ForecastMapper` のような Android 非依存の写像に寄せて検証する。
5. `./gradlew testDebugUnitTest` と `./gradlew assembleDebug` を実行する。task 一覧の正本は [`docs/development.md`](../../../docs/development.md)。
6. 変更内容、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- data 層に `Context`、`View`、RxAndroid（`AndroidSchedulers`）を持ち込まない。
  表示のためのスレッド移送は presentation が付ける。
- Retrofit と Moshi の型を `data/remote/` の外へ出さない。`ForecastResponse` や JSON の項目名が
  Repository の戻り値に現れたら境界が漏れている。
- API の語彙（天気コードの文字列、JSON のキー名）を知ってよいのは `ForecastMapper` だけにする。
- Repository が返すストリームは cold に保つ。呼ばれただけで通信が動き出す実装にしない。
- 通信の失敗は `onError` としてそのまま上げる。`onErrorReturn` などで握り潰さない。
- 契約を変えるときは `domain` の `ForecastRepository` を先に見直し、`domain-implementation` を併用する。
