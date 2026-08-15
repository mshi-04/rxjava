---
name: implementation-review
description: 実装差分をレビューする依頼で使う。RxJava Samples の依存方向、層の責務、ストリームの cold 性、購読の破棄、テスト不足を確認して指摘する。
---

# 目的

RxJava Samples の差分が既存方針に沿っているかを確認し、
コミット前に直すべき問題を具体的に洗い出す。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- 差分が触れている層の docs（[`domain.md`](../../../docs/domain.md) / [`data.md`](../../../docs/data.md) / [`presentation.md`](../../../docs/presentation.md)）
- [`docs/status.md`](../../../docs/status.md)
- 差分と関連コード

# 手順

1. 比較基準を確定する（作業ブランチの差分か、直近コミットからの差分か）。
2. 差分の目的、変更範囲、既存実装との関係を確認する。
3. 下の観点を順に見る。
4. 指摘を分類し、根拠となるファイルと行を添えて報告する。
5. 問題がない場合も、残るリスクと未検証項目を明記する。

# 観点

## 依存方向

- `presentation -> domain <- data` が崩れていないか。
- `presentation` が `data` のクラスを参照していないか。
- `domain` に Android SDK、RxAndroid、DataBinding が入っていないか。
- `data` に Android SDK、RxAndroid が入っていないか。
- 実体の組み立てが `di/ServiceLocator` の外へ漏れていないか。

## ストリームの契約

- Repository と UseCase が返すストリームが cold か。呼ばれただけで動き出していないか。
- 実行ごとの状態を持つサンプルが `Observable.defer` の内側にあるか。
- `AndroidSchedulers.mainThread()` が presentation の外に現れていないか。
- `Subject` を立てて `mergeWith` で合流させる書き方が入っていないか（取りこぼしが出る）。
- `SampleLogLine` のスレッド名が、放出と同じスレッドで確定しているか。
- `SampleLogs` を使わず `onCurrentThread` を直に呼ぶ書き方が、
  `materialize` / `create` で書ける場面に広がっていないか。

## 購読の寿命

- 購読が ViewModel の 1 つに集約されているか。
- 実行状態を落とす経路が 3 つとも塞がれているか（停止・正常終了・エラー終了）。
- `onCleared()` で破棄されているか。
- Activity 側で購読し直していないか（画面回転で二重になる）。

## 責務

- Activity が UseCase や `ServiceLocator` を直接呼んでいないか。
  domain へのアクセスは ViewModel を通す。
- Activity に条件分岐・整形・購読が染み出していないか。
- ViewModel に `View` / `Context` が入っていないか。
- 表示用の整形が domain の model へ移っていないか。
- サンプル本体が `SampleCatalog` の外へ散っていないか。

## 過剰

- 委譲だけの UseCase を新たに増やしていないか。
- `SampleLogs` の公開メソッドが増えていないか。
- 既存 contract で足りるのに新しい公開 API を足していないか。
- 呼び出し側のない public メソッドや overload が残っていないか。

## テストと検証

- 変更した振る舞いのうち、JVM で検証できるものにテストがあるか。
- エミュレータでしか確認できない項目が `docs/status.md` の未検証に残っているか。
- 実行した task と結果が報告されているか。

# 指摘の形式

指摘 1 件につき、次を書く。

- 分類（`bug` / `依存方向` / `責務` / `過剰` / `テスト不足` / `文書`）
- 対象のファイルと行
- 何が問題か（現象、または崩れている前提）
- どう直すか

# 注意事項

- 好みではなく、バグ、依存方向、責務分離、保守性、テスト不足を優先する。
- テストコードだけの追加・修正は `test-implementation` を優先する。
- 設計そのものを変える必要があると判断したときは、コードではなく
  `docs/architecture.md` と該当層の docs を先に直すよう指摘する。設計が正で、コードがそれに従う。
