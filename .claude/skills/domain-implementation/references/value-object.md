# Value object

識別子や制約つきの値を型で表し、生の `String` を層をまたいで運ばない。

## 確認する対象

- `domain/model/SampleId` の実装と、`equals` / `hashCode` / `value()` の形
- その値を `Map` のキーや `Intent` の extra として使っている箇所

## 実装時の注意

- `private` コンストラクタと `static of(...)` を組み合わせ、生成経路を 1 つにする。
- `null` は `Objects.requireNonNull` で入口で弾く。
- `equals` と `hashCode` を必ず対で実装する。`Map` のキーになるため片方だけでは壊れる。
- `toString` は値そのものを返す。ログや例外メッセージで読めることを優先する。
- 表示用の整形を持たせない。整形は presentation の仕事。
- 一度公開した値は変えない。画面遷移で運ばれる識別子は永続的な契約になる。

## テスト判断

- 同値性（同じ値なら `equals` が真、`hashCode` が一致）を JVM Unit Test で押さえる。
- `null` を渡したときに例外になることを確認する。

## 検証観点

- 生の `String` のまま受け渡している箇所が残っていないか。
- 値の妥当性検査が呼び出し側に散っていないか。
