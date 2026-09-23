# Retrofit インタフェース

7Timer! API の HTTP 契約だけを表し、それ以上のロジックを持たない。

## 確認する対象

- `data/remote/ForecastApi` の現在のメソッドと `@GET` のパス
- `di/NetworkModule`（`OkHttpClient` とタイムアウト、Retrofit と Moshi の組み立て）

## 実装時の注意

- `product` と `output` のように呼び出しごとに変わらない値は、メソッド引数ではなく `@GET` の URL に固定で埋める。
  呼び出し側が指定できるのは実際に変わる値（緯度経度）だけにする。
- 戻り値は `Single<ForecastResponse>` のように `data/remote/` の DTO を返してよいが、
  この DTO を `remote/` の外（Repository 実装より上）へそのまま渡さない。写像は `ForecastMapper` の仕事。
- `RxJava3CallAdapterFactory` は `create()` を使う。`createAsync()` は独自にスレッドを起こし、
  presentation が付けた `subscribeOn` の効果が見えなくなるため使わない。
- `OkHttpClient` の接続・読み取りタイムアウトは 10 秒に揃える（`di/NetworkModule` で設定）。
  待ちが長いと、画面が「読み込み中」のまま固まったのか通信しているのか区別できなくなる。
- `OkHttpClient` は生成コストが高く接続プールを持つため、`di/NetworkModule` で `@Singleton` として 1 つだけ作る。
  新しい API 呼び出しを増やすときも、この `OkHttpClient` と `Retrofit` インスタンスを使い回す。

## テスト判断

- インタフェースの宣言そのものはテストしない。
- 実際の通信は JVM Unit Test では検証しない。`ForecastMapper` へ渡すデータの形を変えたときは
  `dto-and-mapper.md` 側のテストで押さえる。

## 検証観点

- 呼び出しごとに変わらない値がメソッド引数として露出していないか。
- `createAsync()` に置き換わっていないか。
- DTO が `remote/` の外（Repository 実装の戻り値、presentation）に漏れていないか。
