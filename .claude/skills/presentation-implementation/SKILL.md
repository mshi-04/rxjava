---
name: presentation-implementation
description: 画面の表示、操作、遷移、表示文言を変える依頼で使う。東京の天気の presentation 層の Activity、ViewModel、RecyclerView Adapter、DataBinding レイアウト、strings.xml を担当する。
---

# 目的

東京の天気の presentation 層を、既存の View + ViewDataBinding + MVVM 構成に沿って変更する。

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
| Activity（View、`ForecastActivity`） | [`references/activity.md`](references/activity.md) |
| ViewModel（`ForecastViewModel`） | [`references/viewmodel.md`](references/viewmodel.md) |
| 購読と Scheduler | [`references/subscription.md`](references/subscription.md) |
| レイアウトと binding 式（`activity_forecast.xml` / `item_forecast.xml`） | [`references/data-binding.md`](references/data-binding.md) |
| RecyclerView Adapter（`ForecastAdapter`） | reference なし。`data-binding.md` の binding 使い回しの項に従う |
| 表示文言（`ForecastFormatter` / `strings.xml`） | reference なし。`docs/presentation.md` の方針に従う |

# 手順

1. 変更対象の役割クラスを判定し、該当 reference を読む。
2. `presentation -> domain <- data` の依存方向と、ViewModel 経由の domain アクセスを保つ。
3. reference の観点に沿って実装する。
4. ViewModel に判断が入ったときは JVM Unit Test の要否を判断する。View と DataBinding、
   `AndroidSchedulers.mainThread()` を通る経路は JVM では検証できないため、エミュレータでの確認に回す。
5. `./gradlew assembleDebug` と、レイアウト / resource 変更があれば `./gradlew lintDebug` を実行する。task 一覧の正本は [`docs/development.md`](../../../docs/development.md)。
6. 変更内容、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- `presentation` から `data` のクラスを直接参照しない。実体の組み立ては `di/ServiceLocator` に閉じる。
- ViewModel に `View`、`Context`、`Activity` を持ち込まない。表示文言を `strings.xml` の ID
  （`@StringRes int`）で公開し、レイアウト側の暗黙の `context` で解決する形にする。
- `findViewById` と `setOnClickListener` を使わない。押下はレイアウト側で ViewModel へ束ねる。
- 読み込み中・成功・失敗の 3 状態を 1 つの値で兼ねない。空リストで「読み込み中」と「結果 0 件」を兼ねるような実装にしない。
- 固定色や固定文字サイズの直書きを避け、Material 3 の theme 属性を使う。
- 表示文言は `strings.xml` に置く。例外の `getMessage()` をそのまま画面へ出さない。
- domain / data へ波及するときは、対応する Skill を併用する。
