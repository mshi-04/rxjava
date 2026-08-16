# DTO と写像

API のレスポンス構造を受け取る器（DTO）と、domain のモデルへ写す変換（Mapper）を分けて持つ。

## 確認する対象

- `data/remote/ForecastResponse` の `@Json` フィールドと入れ子クラス（`Temperature`, `Entry`）
- `data/remote/ForecastMapper` の現在の変換処理
- 対応する `app/src/test/java/com/example/rxjava/data/remote/ForecastMapperTest`

## 実装時の注意

- DTO（`ForecastResponse`）は Moshi が要求する `public` フィールドと `@Json(name = ...)` だけを持ち、
  振る舞いを持たせない。JSON のキー名がそのままフィールド名と一致していても `@Json` を省略しない。
- API の語彙を知ってよいのは `ForecastMapper` だけにする。天気コードの文字列や JSON のキー名を
  他のクラスへ持ち出さない。
- `weather` の文字列コードから `WeatherCondition` への対応表はここに持つ。未知のコードと `null` は
  例外にせず `WeatherCondition.UNKNOWN` に落とし、通信自体は成功として扱う。
  API が値を増やしたときにアプリが落ちるより、その日だけ天気が不明になる方がよい。
- `date` の `20260816` のような整数は `ForecastDate.of(year, month, day)` へ分解する。
- `temp2m` は `TemperatureRange.of(Temperature.ofCelsius(max), Temperature.ofCelsius(min))` へ、
  `wind10m_max` は `WindSpeed.ofLevel(...)` へ写す。範囲外の値はここで例外になる。
  壊れた値を画面まで運ぶより、その場で止める。
- `dataseries` が `null` のときや要素が `null` のとき、要素の `temp2m` が `null` のときは
  例外にする（`IllegalStateException`）。形が変わったことを `onError` として上げる。
  一方 `dataseries` が空リストであること自体は正常系で、0 件の結果として扱ってよい。
- 検査は入口の早期リターン（ガード節）で書き、正常系を分岐の外に出す。

## テスト判断

- `ForecastMapper` は Android に依存しないため、変換経路をすべて JVM Unit Test で検証する対象にする。
  `ForecastMapperTest` を型として、正常系・未知コードの `UNKNOWN` 化・`dataseries` 欠落・`temp2m` 欠落の
  それぞれにテストを持たせる。
- 範囲外の気温や風速階級のような、値オブジェクト側で検査する境界は `domain-implementation` の
  `value-object.md` に委ね、ここでは「例外になって止まること」までを確認する。

## 検証観点

- `data/remote/` の外に DTO のフィールド名や JSON のキー文字列が漏れていないか。
- 未知の `weather` コードが例外にならず `UNKNOWN` に落ちるか。
- `dataseries` や `temp2m` の欠落が例外になるか、空リストや `null` へ潰していないか。
