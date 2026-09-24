# DataBinding

レイアウト側で表示と操作を完結させ、Fragment に余分な操作コードを書かせない。

## 確認する対象

- `app/src/main/res/layout/activity_forecast.xml`、`fragment_forecast.xml` と `item_forecast.xml`
- `presentation/util/ForecastFormatter`（`item_forecast.xml` が呼ぶ `@StringRes int` の変換）
- 対応する binding クラスを使っている `ForecastActivity` / `ForecastFragment` / `ForecastAdapter`

## レイアウトと変数

| レイアウト | 受け取る変数 |
| --- | --- |
| `activity_forecast.xml` | なし。固定 ID の `FragmentContainerView` を置く |
| `fragment_forecast.xml` | `viewModel` |
| `item_forecast.xml` | `forecast` |

## 実装時の注意

- ルート要素は `<layout>` にする。変数を使わない画面も例外にしない。
  binding クラスの生成が揃わないと、参照側でだけ規約が変わる。
- 押下はレイアウト側で ViewModel のメソッドへ束ねる。

```xml
<Button
    android:onClick="@{() -> viewModel.load()}"
    android:text="@string/forecast_retry" />
```

- `fragment_forecast.xml` の binding 式は押下の束ねだけに使う。状態による `visibility` の
  出し分けは Fragment の `render` が行い、binding 式に状態の判定を書かない。
- 読み込み中・一覧・失敗の 3 つの表示は、それぞれ独立した `View` にして `android:id` を付ける。
  1 つの `View` で兼ねない。
- 天気の表示文言のように model からそのまま文字列にできない値は、`@StringRes int` を返す
  変換関数（`ForecastFormatter.weatherLabelResId(...)`）を `<import>` し、
  binding 式の暗黙の `context` で `context.getString(...)` して解決する。
  `ForecastFormatter` 自身は `Context` を持たない。

```xml
<TextView
    android:text="@{context.getString(ForecastFormatter.weatherLabelResId(forecast.weather))}" />
```

- 数値と単位を組み合わせる文言（日付、気温、風速）は `strings.xml` の書式文字列
  （`@string/forecast_temperature_format` など）へ値オブジェクトのフィールドをそのまま渡す。
  Java 側で文字列を組み立てない。
- 文字サイズや色を直接書かず、Material 3 の theme 属性（`?attr/textAppearanceTitleMedium` など）を使う。
- `paddingVertical` / `paddingHorizontal` は API 26 以降のため使わない。個別に書く。
- Fragment の画面ルートだけに `android:fitsSystemWindows="true"` を付ける。
  `targetSdk` 35 以上では edge-to-edge が強制され、付けないとステータスバーと
  先頭の項目が重なる。テーマは `NoActionBar` 系を使い、タイトルはレイアウト内の
  `TextView` で出す。ActionBar を残すと `android:label` と二重になる。
- Adapter で binding を使い回すときは、値を差し替えたあとに `executePendingBindings()` を呼ぶ。
  忘れると再利用された行に前の値が残る。
- 変数の型は完全修飾名で書く。domain の model を渡し、data の型は渡さない。

## ビルドが通らないとき

DataBinding の失敗は Java 側のエラーだけを見ても分からないことが多い。
原因のレイアウトは `app/build/reports/data_binding/` 以下のログに出る。

## テスト判断

- binding 式は JVM Unit Test で検証できない。エミュレータでの確認に回す。

## 検証観点

- レイアウトが参照している名前と、ViewModel / model が公開しているフィールド名が一致しているか。
- `<layout>` で包まれていないレイアウトが混ざっていないか。
- `fragment_forecast.xml` に状態を判定する binding 式が戻っていないか。
- Fragment の画面ルートに `fitsSystemWindows` が付いているか。先頭の項目がステータスバーに隠れていないか。
