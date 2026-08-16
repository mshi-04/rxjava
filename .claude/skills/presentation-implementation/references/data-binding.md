# DataBinding

レイアウト側で表示と操作を完結させ、Activity にコードを書かせない。

## 確認する対象

- `app/src/main/res/layout/activity_forecast.xml` と `item_forecast.xml`、それぞれの `<data>` ブロック
- `presentation/util/ForecastFormatter`（`item_forecast.xml` が呼ぶ `@StringRes int` の変換）
- 対応する binding クラスを使っている `ForecastActivity` / `ForecastAdapter`

## レイアウトと変数

| レイアウト | 受け取る変数 |
| --- | --- |
| `activity_forecast.xml` | `viewModel` |
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

- `ObservableBoolean` / `ObservableField` は `@{viewModel.loading}` の形で読む。`.get()` は書かない。
- 読み込み中・一覧・失敗の 3 つの表示は、それぞれ独立した `View` の `visibility` を
  `viewModel.loading` / `viewModel.hasError` の組み合わせで出し分ける。1 つの `View` で兼ねない。
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
- 読み込み中・一覧・失敗のいずれか 2 つ以上が同時に見える組み合わせが `visibility` の式に残っていないか。
