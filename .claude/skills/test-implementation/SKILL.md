---
name: test-implementation
description: テストコードを追加・修正する依頼で使う。RxJava Samples の JVM Unit Test（JUnit Jupiter + TestObserver）を担当する。
---

# 目的

RxJava Samples の変更した振る舞いを、JVM で検証できる範囲で押さえる。

# 最初に確認するもの

- [`app/src/test/java/com/example/rxjava/`](../../../app/src/test/java/com/example/rxjava/) の既存テスト
- 対象コードと、対応する層の docs
- [`app/build.gradle.kts`](../../../app/build.gradle.kts)（利用できる依存の確認）

# JVM で検証できる範囲

| 対象 | 判断 |
| --- | --- |
| `domain` の value object、entity | できる |
| `data` の Repository implementation、`SampleLogs`、カタログのメタデータ | できる |
| 有限で同期的なサンプル本体 | できる |
| 無限ストリーム・実時間の `sleep` を含むサンプル | しない。エミュレータで確認する |
| `AndroidSchedulers.mainThread()` を通る経路 | できない。エミュレータで確認する |
| Activity、DataBinding、`ObservableField` | できない。エミュレータで確認する |

# 手順

1. 変更した振る舞いを洗い出し、上の表で JVM Unit Test の対象かを判断する。
2. 対象外のものは、何をエミュレータで確認すべきかを [`docs/status.md`](../../../docs/status.md) の未検証に残す。
3. AAA（Arrange / Act / Assert）でテストを追加・修正する。
4. `./gradlew testDebugUnitTest` を実行する。
5. 追加・修正したテスト、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 書き方

- JUnit Jupiter。`org.junit.jupiter.api.Test` と `org.junit.jupiter.api.Assertions` を使う。
  JUnit 4 の `org.junit.Test` / `org.junit.Assert` は残っていない。
- テストクラスとテストメソッドは package-private にする。`public` は付けない。
- テストメソッド名は ASCII の英語で、何がどうなるかを平叙文で書く。
- 同じ対象に対する複数の検証は `assertAll` でまとめ、最初の失敗で打ち切らない。
  要素ごとに同じ検証を繰り返すときは `assertAll(stream.map(...))` を使う。
- ストリームの検証は `.test()`（`TestObserver`）を使う。
  終了を待つときは `awaitDone(5, TimeUnit.SECONDS)` のように必ず上限を付ける。
- cold であることを確かめるテストは、購読前後でカウンタが増えないこと・増えることを見る。
- 依存の差し替えは手書きの fake で行う。モックライブラリは導入していない。
- テスト依存を足したくなったら、追加せずに理由とともに報告する。

# 注意事項

- テストのために本番コードの可視性を広げない。テストしたいロジックは、
  Android やシングルトンから切り離した位置へ寄せる。
- 実装変更に付随するテストは、対応する層の Skill と併用する。
- 差分全体のレビューは `implementation-review` を優先する。
- 時間に依存するテストを書かない。`Thread.sleep` で待つテストは環境で落ちる。
