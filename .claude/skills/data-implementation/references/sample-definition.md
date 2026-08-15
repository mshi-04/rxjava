# SampleDefinition / SampleCatalog

サンプル 1 本を表す器と、その一覧。サンプル本体の中身ではなく構造を扱う。

## 確認する対象

- `data/catalog/SampleDefinition` の持ち物（id / タイトル / 説明 / `Body`）と `toSample()`
- `data/catalog/SampleCatalog` の `DEFINITIONS` の形
- `domain/model/Sample` との差（実行関数を持つかどうか）

## 実装時の注意

- `SampleDefinition` と `Sample` の差は実行関数の有無だけに保つ。
  この分離により、presentation は一覧を描くのにサンプルの実装を一切参照しない。
- `Body` の署名は全サンプル共通の 1 種類に固定する。
  種類を増やすとサンプル追加の手順が分岐し、追加コストが上がる。
- `DEFINITIONS` は `Collections.unmodifiableList` で包んだ静的な不変リストにする。
  登録順が一覧の表示順になる。
- サンプル本体は private static メソッドとして同じファイルに置く。
  ファイルを分けると「1 ファイルの編集で完結する」という設計目標が崩れる。
- 表示のための属性（アイコン、色、分類バッジ）を足したくなったら、
  それが presentation で導けないかを先に確認する。

## テスト判断

- id とタイトルの一意性、`toSample()` が id / タイトル / 説明を保つことを JVM Unit Test で押さえる。
- 個々のサンプル本体の挙動は、有限で同期的なものだけテストする。

## 検証観点

- `Body` の種類が増えていないか。
- 一覧の順序が登録順以外の要因で決まるようになっていないか。
