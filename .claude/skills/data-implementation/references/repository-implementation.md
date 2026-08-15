# Repository implementation

domain が所有する `SampleRepository` を満たし、サンプルの出所を隠す。

## 確認する対象

- `data/repository/InMemorySampleRepository` の保持構造と `run` の実装
- `domain/repository/SampleRepository` の契約
- `di/ServiceLocator` での組み立て

## 実装時の注意

- 定義は `LinkedHashMap` で保持する。登録順がそのまま一覧の表示順になるため、
  順序が保証されない `Map` に替えない。
- `findAll` は `Sample` へ落として返し、`SampleDefinition` を外へ出さない。
  返すリストは `Collections.unmodifiableList` で包む。
- `run(id)` は実行関数を `Observable.defer` で包む。
  実行ごとにカウンタなどの状態を持つサンプルがあるため、
  購読のたびに作り直されることを保証する必要がある。defer を外すと 2 回目以降が壊れる。
- 未登録の識別子は `IllegalArgumentException` にする。
  画面遷移は自前の識別子でしか起きないため、回復対象ではなく実装の誤りである。
- 別の出所（ファイル、リモート）を足すときは、このクラスを書き換えず別実装を作り、
  `ServiceLocator` の 1 行を変える。

## テスト判断

- 登録順が保たれること、未登録 id で例外になることを JVM Unit Test で押さえる。
- `run()` が cold であること（購読するまで実行されない）と、
  購読ごとに実行関数が呼び直されることを、カウンタを持つ fake で確認する。

## 検証観点

- `Observable.defer` が外れていないか。
- `SampleDefinition` が戻り値や公開メソッドに漏れていないか。
