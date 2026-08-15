---
name: domain-implementation
description: アプリの語彙、公開 contract、UseCase を変える依頼で使う。RxJava Samples の domain 層の value object、entity、UseCase、Repository interface を担当する。
---

# 目的

RxJava Samples の語彙と公開 contract を、Android Framework に依存しない domain 層へ閉じる。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/domain.md`](../../../docs/domain.md)
- [`app/src/main/java/com/example/rxjava/domain/`](../../../app/src/main/java/com/example/rxjava/domain/)
- 関連する [`app/src/test/java/com/example/rxjava/`](../../../app/src/test/java/com/example/rxjava/)

# 役割クラスと参照先

変更対象の役割クラスを見分け、該当する reference だけを読んでから実装する。
薄い役割は reference を作らず docs に委ねる。

| 役割クラス | 参照 |
| --- | --- |
| value object（`SampleId`） | [`references/value-object.md`](references/value-object.md) |
| ログ 1 行（`SampleLogLine`） | [`references/log-line.md`](references/log-line.md) |
| UseCase | [`references/usecase.md`](references/usecase.md) |
| Repository interface | [`references/repository-interface.md`](references/repository-interface.md) |
| 単純な entity（`Sample`） | reference なし。`docs/domain.md` の contract 方針に従う |

# 手順

1. 変更対象の役割クラスを判定し、該当 reference を読む。
2. 既存 contract で足りるか、新しい interface / UseCase / model が必要か決める。
3. reference の観点に沿って実装する。
4. 変更した rule を JVM Unit Test で押さえる。
5. `./gradlew testDebugUnitTest` と `./gradlew assembleDebug` を実行する。domain は Android resource を持たないため lint は通常不要。task 一覧の正本は [`docs/development.md`](../../../docs/development.md)。
6. 変更内容、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- domain 層に `Context`、`View`、`Handler`、RxAndroid、DataBinding を持ち込まない。
- RxJava core（`io.reactivex.rxjava3.core`）だけは例外として許す。理由は `docs/domain.md`。
- domain contract の変更が data、presentation へ波及するときは、対応する Skill を併用する。
- Repository implementation の都合で entity を歪めない。
- 表示のための値（整形済み文字列、色）は presentation へ、サンプルの実装都合の値は data へ置く。
- 新しい公開 API は、既存 contract で足りない理由がある場合だけ追加する。
