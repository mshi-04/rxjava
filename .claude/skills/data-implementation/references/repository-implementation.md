# Repository implementation

`ForecastRepository` の実装 `ForecastRepositoryImpl`。
キャッシュ判定、通信、保存をつなぐ。Retrofit も Room も知らない。

## 確認する対象

- `data/repository/ForecastRepositoryImpl` の現在の実装
- `domain/repository/ForecastRepository` の契約
- `data/datasource/ForecastRemoteDataSource`・`ForecastLocalDataSource`、`data/repository/TimeProvider`
- [`docs/data.md`](../../../../docs/data.md) の「ForecastRepositoryImpl」と「ForecastLocalDataSource と Room」

## 実装時の注意

- 依存は `ForecastRemoteDataSource`・`ForecastLocalDataSource`・`TimeProvider` だけにする。
  `ForecastApi`、`RemoteForecastMapper`、Room の型を直接使わない。
- 判定は `store.read(...).filter(鮮度).map(...).switchIfEmpty(通信して保存)` の形に保つ。
  未取得は `Maybe` の empty で表し、`null` で分岐しない。
- 鮮度は「経過時間が 0 以上 24 時間未満」だけで判定する。24 時間は定数に置く。
- 現在時刻は読み取り完了時と、保存ストリームの購読直前に取る。組み立て時に取らない。
- 保存が完了してから予報を返す。保存の失敗は `onError` にする。
- cold であり、購読するまで読み取りも通信もしない。`subscribeOn` / `observeOn` をここで指定しない。
- 失敗はそのまま `onError` として上げる。古い予報へのフォールバックや `onErrorReturn` で握り潰さない。

## テスト判断

- JVM Unit Test で検証する。`ForecastRemoteDataSource` と `ForecastLocalDataSource` は手書きの fake、時刻は可変のラムダを渡す。
  通信の配線は `ForecastRemoteDataSourceImpl` 側のテストで押さえ、ここでは MockWebServer を使わない。
- 観点の一覧は [`docs/unit-test.md`](../../../../docs/unit-test.md) の「キャッシュ判定の検証」を正本とする。

## 検証観点

- Retrofit や Room の型、`RemoteForecastMapper` を直接参照していないか。
- `subscribeOn` / `observeOn` がこの層に付いていないか。
- 失敗を握り潰す経路や、古い予報へのフォールバックが増えていないか。
- 時刻を購読前に取っていないか。
