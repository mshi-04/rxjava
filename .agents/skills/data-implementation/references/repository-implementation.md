# Repository implementation

`ForecastRepository` の実装。API 呼び出しに写像をつなぐだけの薄い層に保つ。

## 確認する対象

- `data/repository/RemoteForecastRepository` の現在の実装
- `domain/repository/ForecastRepository` の契約
- `data/remote/ForecastApi` と `data/remote/ForecastMapper`

## 実装時の注意

- `api.dailyForecast(...).map(ForecastMapper::toDailyForecasts)` のように、API 呼び出しへ `map` で
  `ForecastMapper` をつなぐだけに保つ。分岐やリトライのようなロジックをここに増やさない。
- cold であり、購読するまで通信しない。`subscribeOn` / `observeOn` をここで指定しない。
  Scheduler の指定は presentation の仕事。
- 失敗はそのまま `onError` として上げる。`onErrorReturn` や `try-catch` で握り潰さない。
  握り潰すと、待っている状態と失敗した状態が画面から区別できなくなる。
- 地点は `Location` のまま受け取り、呼び出し直前で `getLatitude()` / `getLongitude()` に分解する。
  緯度経度を上位層へ生で運ばせない。

## テスト判断

- 実ネットワーク呼び出しを伴うため、この実装自体は JVM Unit Test で検証しない。
  変換ロジックの検証は `dto-and-mapper.md` の対象である `ForecastMapper` 側のテストで代替する。
- 実際に通信できるかはエミュレータ・実機での確認に回す。

## 検証観点

- `subscribeOn` / `observeOn` がこの層に付いていないか。
- API の失敗を握り潰す経路が増えていないか。
- `map` 以外の分岐・変換ロジックがここに漏れ出していないか。
