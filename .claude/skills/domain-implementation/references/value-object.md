# Value object

生の `int` や `String` を層をまたいで運ばず、不変条件を生成時に検査した型に閉じる。

## 確認する対象

- `domain/model/` の `Temperature`, `TemperatureRange`, `WindSpeed`, `ForecastDate`, `Location`
- それぞれを生成している `data/remote/ForecastMapper` と、`Location.TOKYO` を使う `di/ServiceLocator` / `presentation/viewmodel/ForecastViewModel`

## 実装時の注意

- `private` コンストラクタと `static` ファクトリ（`of` / `ofCelsius` / `ofLevel`）を組み合わせ、生成経路を 1 つにする。
- 検査は入口の早期リターンで書き、不正な値は `IllegalArgumentException` で即座に止める。正常系を分岐の外に出す。
- 数値の上限・下限は API が文書化している範囲そのものではなく、壊れたデータを検知するための緩めの境界にする。
  API の公称範囲に厳しく合わせると、API 側の小さな変更でアプリ全体が落ちる。
  例えば `Temperature` は摂氏 -100〜100、`WindSpeed` は階級 0〜12 とし、
  API が文書化している気温 -76〜60、風速階級 1〜8 よりわざと広く取る。
- `TemperatureRange` のように複数の値を組み合わせる型は、組み合わせ自体の不変条件
  （最高が最低を下回らない）をファクトリで検査する。単体の値の検査は組み合わせ先の型に重複させない。
- `equals` / `hashCode` は、`Map` のキーにする・コレクションで重複排除する・テストで値による比較をするなど、
  必要になった型にだけ追加する。ないからといって機械的に足さない。
- 表示形式を持たせない。`ForecastDate` は年月日の整数だけを持ち書式を知らない。
  `Temperature` は摂氏の数値だけを持ち `℃` の記号を持たない。整形は presentation が `strings.xml` と組み合わせて行う。

## テスト判断

- 生成の成功・境界値・失敗（範囲外で例外になること）を JVM Unit Test で押さえる。
- `equals` / `hashCode` を実装した型は同値性のテストも足す。

## 検証観点

- 生の `int` や `String` のまま受け渡している箇所が残っていないか。
- 値の妥当性検査が呼び出し側（`ForecastMapper` など）に散っていないか。
- 境界値が API の公称範囲そのものになっていないか（意図的に緩めているか）。
