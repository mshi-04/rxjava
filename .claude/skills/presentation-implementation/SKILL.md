---
name: presentation-implementation
description: 画面の表示、操作、遷移、表示文言を変える依頼で使う。RxJava Samples の presentation 層の Activity、ViewModel、RecyclerView Adapter、DataBinding レイアウト、strings.xml を担当する。
---

# 目的

RxJava Samples の presentation 層を、既存の View + ViewDataBinding + MVVM 構成に沿って変更する。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/presentation.md`](../../../docs/presentation.md)
- [`app/src/main/java/com/example/rxjava/presentation/`](../../../app/src/main/java/com/example/rxjava/presentation/)
- [`app/src/main/res/layout/`](../../../app/src/main/res/layout/)
- [`app/src/main/res/values/strings.xml`](../../../app/src/main/res/values/strings.xml)

# 役割クラスと参照先

変更対象の役割クラスを見分け、該当する reference だけを読んでから実装する。

| 役割クラス | 参照 |
| --- | --- |
| Activity（View） | [`references/activity.md`](references/activity.md) |
| ViewModel | [`references/viewmodel.md`](references/viewmodel.md) |
| ログの購読と Scheduler | [`references/subscription.md`](references/subscription.md) |
| レイアウトと binding 式 | [`references/data-binding.md`](references/data-binding.md) |
| RecyclerView Adapter | reference なし。`data-binding.md` の binding 使い回しの項に従う |
| strings | reference なし。`docs/presentation.md` の方針に従う |

# 手順

1. 変更対象の役割クラスを判定し、該当 reference を読む。
2. `presentation -> domain <- data` の依存方向と、ViewModel 経由の domain アクセスを保つ。
3. reference の観点に沿って実装する。
4. ViewModel に判断が入ったときは JVM Unit Test の要否を判断する。View と DataBinding は JVM では検証できないため、エミュレータでの確認に回す。
5. `./gradlew assembleDebug` と、レイアウト / resource 変更があれば `./gradlew lintDebug` を実行する。task 一覧の正本は [`docs/development.md`](../../../docs/development.md)。
6. 変更内容、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- `presentation` から `data` のクラスを直接参照しない。実体の組み立ては `di/ServiceLocator` に閉じる。
- ViewModel に `View`、`Context`、`Activity` を持ち込まない。
- `findViewById` と `setOnClickListener` を使わない。押下はレイアウト側で ViewModel へ束ねる。
- 固定色や固定文字サイズの直書きを避け、Material 3 の theme 属性を使う。
- 表示文言は `strings.xml` に置く。サンプルのタイトルと説明だけは `SampleCatalog` に直接書く例外とする。
- domain / data へ波及するときは、対応する Skill を併用する。
