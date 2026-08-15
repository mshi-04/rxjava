# UseCase

1 操作 1 UseCase で、取得や実行の規則を presentation / data へ漏らさない。

## 確認する対象

- `domain/usecase/` の既存 UseCase と命名（`Get` / `Run` など）
- 依存する `SampleRepository` のシグネチャ

## 実装時の注意

- 1 つの UseCase は 1 つの明確な操作を表す。
- 公開メソッドは `invoke` だけにする。名前で操作を表し、引数で対象を表す。
- コンストラクタで `SampleRepository` を受け取る。`ServiceLocator` を UseCase の中から呼ばない。
- ストリームを返す UseCase は cold なまま返す。`subscribeOn` / `observeOn` を付けない。
  Scheduler の指定は presentation と、観察対象としてのサンプル本体に限る。
- 現状の 3 つは委譲 1 行で構わない。絞り込みや並べ替えの規則が要るときに、
  それを presentation へ流さずここへ置くための場所として維持する。

## テスト判断

- 委譲だけの UseCase に単体テストは不要。Repository 実装のテストで足りる。
- 分岐・順序・失敗経路を持たせたときに JVM Unit Test を足す。

## 検証観点

- 委譲だけの UseCase を新たに増やしていないか（結合を避ける必要があるときだけ挟む）。
- presentation が `SampleRepository` を直接参照していないか。
