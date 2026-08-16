---
name: test-implementation
description: テストコードを追加・修正する依頼で使う。東京の天気の JVM Unit Test（JUnit Jupiter）を担当する。
---

# 目的

東京の天気の変更した振る舞いを、JVM で検証できる範囲で押さえる。

# 最初に確認するもの

- [`docs/unit-test.md`](../../../docs/unit-test.md)
- [`app/src/test/java/com/example/rxjava/`](../../../app/src/test/java/com/example/rxjava/) の既存テスト
- 対象コードと、対応する層の docs

# 手順

1. 変更した振る舞いを洗い出し、`docs/unit-test.md` の「JVM で検証できる範囲」で対象かを判断する。
2. 対象外のものは、何をエミュレータで確認すべきかを [`docs/status.md`](../../../docs/status.md) の未検証に残す。
3. AAA でテストを追加・修正する。Arrange / Act / Assert それぞれにコメントを置く。
4. `./gradlew testDebugUnitTest` を実行する。
5. 追加・修正したテスト、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- 書き方（AAA とコメント、命名、`assertAll`、`TestObserver`、手書き fake、
  時間依存を書かない方針）は `docs/unit-test.md` を正本とする。規約本文はここで複製しない。
- テストのために本番コードの可視性を広げない。テストしたいロジックは、
  Android やシングルトンから切り離した位置へ寄せる。
- テスト依存を足したくなったら、追加せずに理由とともに報告する。
- 実装変更に付随するテストは、対応する層の Skill と併用する。
- 差分全体のレビューは `implementation-review` を優先する。
