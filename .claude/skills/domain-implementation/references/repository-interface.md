# Repository interface

domain が所有し、data が実装する。これで依存が内向きに反転する。

## 確認する対象

- `domain/repository/ForecastRepository` の現在のシグネチャ
- 実装している `data/repository/RemoteForecastRepository`
- 呼び出している `domain/usecase/GetForecastUseCase`

## 実装時の注意

- インタフェースは domain に置く。data 側に置くと依存の向きが崩れる。
- 戻り値に data 層の型を出さない。`ForecastResponse` のような Retrofit / Moshi の DTO は
  ここに現れてはならない。
- `Single<List<DailyForecast>> find(Location location)` は cold な契約とする。
  購読するまで通信は起きず、Scheduler も指定しない。`subscribeOn` / `observeOn` は呼び出し側（presentation）が付ける。
- 通信の失敗は `onError` として上げる契約にする。空リストや `null` に潰さない。
  潰すと、待っている状態と失敗した状態が画面から区別できなくなる。
- 地点は `Location` 値オブジェクトで受け取る。緯度経度の `double` を生で引数に取らない。

## テスト判断

- インタフェース自体はテストしない。実装側（`data-implementation`）でテストする。
- 契約を変えたときは、既存実装とすべての呼び出し側を確認する。

## 検証観点

- data 層の型やアノテーションが署名に漏れていないか。
- cold であり Scheduler を指定しない前提を、実装側が守れているか。
