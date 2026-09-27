# data 層

7Timer! API との通信と、`ForecastRepository` の実装を置く。
外部 API の語彙はすべてこの層に閉じる。

## 依存

| import してよい | してはならない |
| --- | --- |
| `domain`、`javax.inject`、RxJava 全般、Retrofit、Moshi、OkHttp。`local` のみ Room・AndroidX SQLite・AndroidX annotation | Android SDK、その他の AndroidX、RxAndroid、Hilt 固有の型、`presentation` |

Repository 実装はコンストラクタに `@Inject` を付ける。
インタフェースとの対応は `di/RepositoryModule` の `@Binds` が結ぶ。
`@Module` や `@Provides` のような Hilt 固有の型はこの層に書かない。

`AndroidSchedulers` は使わない。表示のためのスレッド移送は `presentation` が付ける。

Retrofit と Moshi の型は `remote/` の外へ出さない。
DTO や JSON の項目名が Repository の戻り値に現れたら、境界が漏れている。

## クラス

| クラス | 役割 |
| --- | --- |
| `remote/ForecastApi` | Retrofit のインタフェース |
| `remote/ForecastResponse` | API のレスポンス DTO |
| `remote/ForecastMapper` | DTO から `domain` のモデルへの写像 |
| `repository/RemoteForecastRepository` | `ForecastRepository` の実装 |

## 7Timer! との通信

```
https://www.7timer.info/bin/api.pl?product=civillight&output=json&lat=&lon=
```

認証もキーもない。現行の公式 Wiki では、無料・有料のソフトウェアで利用でき、
利用許可の取得は不要と案内されている。利用時は最新の記載を確認する。
仕様と利用条件は https://github.com/Yeqzids/7timer-issues/wiki/Wiki にある。
`civillight` は 7 日分を日単位で返す。1 日分の形は次のとおり。

```json
{"date":20260816,"weather":"lightrain","temp2m":{"max":26,"min":25},"wind10m_max":2}
```

`product` と `output` は固定なので、Retrofit のメソッド引数ではなく URL に埋める。
呼び出し側が指定できるのは緯度経度だけにする。

### DTO の数値は箱型にする

`date`・`temp2m` の `max` / `min`・`wind10m_max` は `Integer` で受ける。
`int` にすると、キーが欠落したときに Moshi が `0` を残し、
「値が無い」と「本当に 0」を区別できなくなる。

区別できないと診断が壊れる。`date` の欠落は「月が範囲外: 0」に化け、
`temp2m` の片方の欠落は「最高気温が最低気温を下回る」に化け、
`wind10m_max` の欠落は正当な風速階級 0 として画面に出る。
`ForecastMapper` は `null` を欠落として検知し、何が無いのかを述べる例外にする。

`wind10m_max` は m/s ではなく階級である。単位付きで表示しない。

## ForecastMapper

DTO から `domain` のモデルへ写す。ここが唯一、API の語彙を知る場所。

- `date` の `20260816` を `ForecastDate` の年月日へ分解する
- `weather` の文字列コードを `WeatherCondition` へ写す。
  未知のコードと `null` は `UNKNOWN` に落とし、例外にしない。
  API が値を増やしたときにアプリが落ちるより、その日だけ天気が不明になる方がよい
- `temp2m` を `TemperatureRange` へ、`wind10m_max` を `WindSpeed` へ写す。
  範囲外の値はここで例外になる。壊れた値を画面まで運ばない
- **1 日分の失敗で全体を落とさない。** 写せなかった日は飛ばし、残りを返す。
  7 日のうち 1 日が壊れているだけで全部を捨てるのは、未知の天気コードを
  `UNKNOWN` に落とす方針と釣り合わない
- ただし `dataseries` に要素があるのに 1 件も写せなかった場合は例外にする。
  空リストを返すと、画面が「取得できたが 0 件」と区別できなくなる。
  その例外の cause には最初の日別変換例外を残し、欠けた項目などの診断情報を失わない
- `dataseries` が欠けていたら空リストにせず例外にする。
  形が変わったことを `onError` として上げる

検査は入口の早期リターンで書き、正常系を分岐の外に出す。

## 通信の設定

組み立ては `di/NetworkModule` に置く。

`OkHttpClient` のタイムアウトは接続・読み取りとも 10 秒。
待ちが長いと、画面が「読み込み中」のまま固まったのか通信しているのか区別できない。

`RxJava3CallAdapterFactory` は素の `create()` を使う。
`createAsync()` は独自にスレッドを起こし、`presentation` が付けた `subscribeOn` の
効果が見えなくなる。Scheduler の指定を呼び出し側から追えることを優先する。

## RemoteForecastRepository

`RemoteForecastRepository` が Room のキャッシュ判定、API 通信、保存を担当する。
`find(Location)` の購読時に最終取得日時と予報をそれぞれのテーブルから読み取り、
日時が存在して現在以下かつ経過時間が 24 時間未満なら保存済み予報を返す。
未取得、24 時間以上経過、未来の日時の場合は API へ通信する。

`local/` に Entity、DAO、Database、`RoomForecastStore` を置く。
Room と AndroidX SQLite への依存はこのパッケージだけに許可し、
Android SDK と Context は持ち込まない。DB の生成は `di/DatabaseModule` が行う。
Repository は Android 非依存の `ForecastStore` と `TimeProvider` を受け取る。

`forecasts` は緯度・経度・一覧内の順序を複合主キーとし、変換済みの
日付、天気区分、最高／最低気温、風速階級を保存する。
`last_fetched_at` は緯度・経度を複合主キーとし、最終取得成功日時を
Unix 時刻のミリ秒で保存する。空一覧も日時があれば取得済みとして扱う。
対象地点の予報置換・日時更新は同じトランザクションで行う。
日時は API 取得と写像の成功後、保存直前に取得する。
通信・写像・保存の失敗時は古い予報へのフォールバックを行わず、
保存済み予報と日時を維持する。読み取りの失敗も onError として返す。

cold であり、購読するまで通信しない。Scheduler も指定しない。
失敗はそのまま `onError` として上げ、握り潰さない。

## 変更するとき

API を差し替えるときは `ForecastRepository` の別実装を作り、
`di/RepositoryModule` の 1 行を変える。`domain` と `presentation` は触らない。

`remote/` の DTO と Mapper はその実装専用であり、他から参照させない。

## Room の非同期アクセス

`ForecastStore.read` は cold な `Single<Snapshot>`、`save` は cold な `Completable` を返す。
DAO は LastFetchedAt を Maybe、Forecast の一覧を Single で個別に読む。
日時が存在しない場合は未取得の Snapshot、存在する場合は予報一覧を読み取った Snapshot を返す。
取得済みの空一覧と未取得は日時の有無で区別する。読み取りは二つのクエリであり、
両クエリの間に別の保存が完了した場合にはその更新後の予報を読むことがある。
Room-rxjava3 が読み取りを Room の実行基盤へ移す。保存は Completable.fromAction で
既存の同期 DAO トランザクションを包み、Room の transaction executor 上で実行する。
この DB 内部の実行指定だけは local の責務とし、通信開始と UI 通知は presentation が指定する。
Repository は flatMap で日時判定と通信をつなぎ、保存完了後にだけ予報を返す。
保存日時は保存ストリームの購読直前に取得する。本番コードで blocking 呼び出しは使わない。
