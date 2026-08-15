---
name: data-implementation
description: サンプルの保持・検索・実行の仕組みや、ログ変換の道具を変える依頼で使う。RxJava Samples の data 層の Repository implementation、SampleDefinition、SampleLogs を担当する。サンプルを 1 本追加するだけなら add-rx-sample を使う。
---

# 目的

RxJava Samples のサンプル定義の置き場と `SampleRepository` の実装を、
Android Framework に依存しない data 層へ閉じる。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/data.md`](../../../docs/data.md)
- [`app/src/main/java/com/example/rxjava/data/`](../../../app/src/main/java/com/example/rxjava/data/)
- 関連する [`app/src/test/java/com/example/rxjava/data/`](../../../app/src/test/java/com/example/rxjava/data/)

# 役割クラスと参照先

変更対象の役割クラスを見分け、該当する reference だけを読んでから実装する。

| 役割クラス | 参照 |
| --- | --- |
| Repository implementation | [`references/repository-implementation.md`](references/repository-implementation.md) |
| サンプル定義の器（`SampleDefinition` / `SampleCatalog` の構造） | [`references/sample-definition.md`](references/sample-definition.md) |
| ログ変換の道具（`SampleLogs`） | [`references/sample-logs.md`](references/sample-logs.md) |
| サンプル本体を 1 本足す | この Skill ではなく `add-rx-sample` を使う |

# 手順

1. 変更対象の役割クラスを判定し、該当 reference を読む。
2. `presentation -> domain <- data` の依存方向を保つ。data から presentation を参照しない。
3. reference の観点に沿って実装する。
4. 変更した振る舞いを JVM Unit Test で押さえる。data 層は Android に依存しないため、ほぼすべてが JVM で検証できる。
5. `./gradlew testDebugUnitTest` と `./gradlew assembleDebug` を実行する。task 一覧の正本は [`docs/development.md`](../../../docs/development.md)。
6. 変更内容、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- data 層に `Context`、`View`、RxAndroid（`AndroidSchedulers`）を持ち込まない。
  表示のためのスレッド移送は presentation が付ける。
- `SampleDefinition` を domain や presentation へ露出させない。境界は `toSample()`。
- Repository が返すストリームは cold に保つ。呼ばれただけで動き出す実装にしない。
- サンプル本体に書く `subscribeOn` / `observeOn` は観察対象なので隠さない。
  この層の「Android 非依存」は RxAndroid と Android SDK を指し、Scheduler 一般ではない。
- 契約を変えるときは `domain` の `SampleRepository` を先に見直し、`domain-implementation` を併用する。
