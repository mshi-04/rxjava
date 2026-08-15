# SampleLogs

任意のストリームを `SampleLogLine` の列へ変換する道具。サンプル本体が唯一使う補助。

## 確認する対象

- `data/catalog/SampleLogs` の `materialize` と `create`
- それぞれを使っているサンプル本体

## 実装時の注意

- 道具は 2 系統に保つ。増やすとサンプルを書くときの選択が増え、
  どれを使うべきかの判断コストがサンプル追加のたびに発生する。

| 行の作り方 | 使いどころ |
| --- | --- |
| `materialize(source)` | オペレータの結果を見せる。`onNext` / `onError` / `onComplete` を 1 行ずつに変換する |
| `create(work)` | 手続き的に、書いた順にログを出す |
| `SampleLogLine.onCurrentThread` を `map` の中で直に呼ぶ | 上の 2 つでは表せないとき |

3 つ目は道具ではなく、道具を使わない書き方である。
`intervalWithDisposable` と `flowableBackpressure` が該当する。
前者は終端しないため `materialize` が意味を持たず、
後者はメッセージに破棄件数という副次的な情報を混ぜる必要がある。
この形が要るのは、値そのものではなく値に付随する状態を見せたいときに限る。
新しいサンプルで使う前に、`materialize` か `create` で書けないかを確認する。

- `materialize` の `Flowable` 版は、`materialize()` を先に適用してから `toObservable()` する。
  順序を逆にすると上流へ無制限の要求が伝わり、backpressure の観察ができなくなる。
- `create` が返すのも cold な `Observable` に保つ。購読ごとにブロックを最初から実行する。
- どの作り方でも、行の生成は `SampleLogLine.onCurrentThread` を通す。スレッド名を手で組み立てない。

## Subject を立てて合流させない

`PublishSubject` を別に用意して `mergeWith` で本流に合流させる道具を足さない。
Subject は hot なため、合流先が購読を始める前に流れた行が落ちる。
本流を非同期にすると、この取りこぼしはタイミング次第で再現しなくなり、
教材として誤ったパターンを示すことになる。

手続き的にログを出したいときは `create` を使う。

## テスト判断

- `onNext` / `onError` / `onComplete` が期待どおりの文言になることを JVM Unit Test で押さえる。
- `materialize` がエラーを下流へ伝播せず、行として運んで正常終了することを確認する。
- 生成された行のスレッド名が空でないことを確認する。

## 検証観点

- 道具（`SampleLogs` の public メソッド）が 3 つ目に増えていないか。
- 直に `onCurrentThread` を呼ぶ書き方が、`materialize` / `create` で書ける場面に広がっていないか。
- 使われていない overload が残っていないか。呼び出し側がないと、
  そこに書いた不変条件（`Flowable` 版の演算子順など）を検証する手段がなくなる。
