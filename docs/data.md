# data 層

7Timer! API との通信、取得結果の保存、`ForecastRepository` の実装を置く。
外部 API の語彙と DB の行の形はすべてこの層に閉じる。

## 依存

| import してよい | してはならない |
| --- | --- |
| `domain`、`javax.inject`、RxJava 全般、Retrofit、Moshi、OkHttp。`db` と `datasource` のみ Room・AndroidX SQLite・AndroidX annotation | Android SDK、その他の AndroidX、RxAndroid、Hilt 固有の型、`presentation`、`di` |

実装クラスはコンストラクタに `@Inject` を付ける。
インタフェースとの対応は `di/RepositoryModule` の `@Binds` が結ぶ。
`@Module` や `@Provides` のような Hilt 固有の型はこの層に書かない。

`AndroidSchedulers` は使わない。表示のためのスレッド移送は `presentation` が付ける。

Retrofit と Moshi の型は `remote/` の外へ出さない。Room の Entity と DAO は `db/` と `datasource/` の外へ出さない。
DTO や行の形が Repository の戻り値に現れたら、境界が漏れている。
package 間の参照の向きは [architecture.md](architecture.md) の「層と依存の向き」にある。

## クラス

| クラス | 役割 |
| --- | --- |
| `datasource/ForecastRemoteDataSource` | 通信して `domain` のモデルを返す口 |
| `datasource/ForecastRemoteDataSourceImpl` | `ForecastRemoteDataSource` の実装。通信に写像をつなぐ |
| `datasource/ForecastLocalDataSource` | 地点ごとの予報と最終取得日時を保存・復元する口 |
| `datasource/ForecastLocalDataSourceImpl` | `ForecastLocalDataSource` の実装。DAO に Entity の写像をつなぐ |
| `db/ForecastDatabase` | Room の Database |
| `db/ForecastEntityMapper` | Entity と `domain` のモデルの相互写像 |
| `db/dao/ForecastDao` | Room の DAO |
| `db/entity/ForecastEntity` / `db/entity/LastFetchedAtEntity` | Room の Entity |
| `remote/ForecastApi` | Retrofit のインタフェース |
| `remote/ForecastResponse` | API のレスポンス DTO |
| `util/RemoteForecastMapper` | DTO から `domain` のモデルへの写像 |
| `repository/ForecastRepositoryImpl` | `ForecastRepository` の実装。キャッシュ判定、通信、保存をつなぐ |
| `repository/TimeProvider` | 現在時刻の取得。テストで差し替える |

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
`RemoteForecastMapper` は `null` を欠落として検知し、何が無いのかを述べる例外にする。

`wind10m_max` は m/s ではなく階級である。単位付きで表示しない。

## RemoteForecastMapper

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

## ForecastRemoteDataSourceImpl

`ForecastRemoteDataSource.fetch(Location)` を実装する。
`api.dailyForecast(...)` に `map(RemoteForecastMapper::toDailyForecasts)` をつなぐだけに保ち、
分岐やキャッシュの知識を持たない。
地点は `Location` のまま受け取り、呼び出し直前で緯度経度に分解する。

cold であり、購読するまで通信しない。Scheduler も指定しない。
HTTP・JSON・写像の失敗はそのまま `onError` として上げる。

## ForecastRepositoryImpl

`ForecastRepository.find(Location)` を実装する。
`ForecastRemoteDataSource`・`ForecastLocalDataSource`・`TimeProvider` だけに依存し、Retrofit も Room も知らない。

1. 購読時に `ForecastLocalDataSource.read` で保存済みの予報を読む
2. 読み取りが完了した時点の現在時刻と最終取得日時を比べ、
   経過時間が 0 以上 24 時間未満なら保存済みの予報を返す
3. 未取得（`Maybe` の empty）、24 時間以上経過、未来の日時の場合は `ForecastRemoteDataSource.fetch` で通信する
4. 通信と写像が成功したら、保存ストリームの購読直前に現在時刻を取り、`ForecastLocalDataSource.save` で保存する
5. 保存が完了してから予報を返す

空一覧も、日時があれば取得済みとして扱う。

通信・写像・保存の失敗時は古い予報へのフォールバックを行わず、`onError` を返す。
保存済みの予報と日時はそのまま残る。
読み取りの失敗も `onError` として返し、通信へは進まない。

cold であり、購読するまで読み取りも通信もしない。Scheduler も指定しない。
再購読のたびにキャッシュ判定をやり直す。

## ForecastLocalDataSource と Room

### 契約

- `read(Location)` は cold な `Maybe<Snapshot>` を返す。empty は未取得を表す
- `Snapshot` は予報一覧と最終取得日時（Unix 時刻のミリ秒、`long`）を持つ。
  一覧は防御的にコピーし、`null` 要素を拒む。空一覧は正当な値である
- `save(Location, List<DailyForecast>, long)` は cold な `Completable` を返す
- `save` は渡された一覧を呼び出した時点でコピーする。呼び出し元が後から一覧を変えても保存内容は変わらない。
  DB 操作は購読するまで始まらない
- 購読を解除すると完了も失敗も通知しない。ただし始まったトランザクションは取り消さない。
  予報と日時は同じトランザクションで書くため、どの時点で解除しても片方だけが更新されることはない

### テーブル

`forecasts` は緯度・経度・一覧内の順序を複合主キーとし、変換済みの
日付、天気区分、最高／最低気温、風速階級を保存する。
`last_fetched_at` は緯度・経度を複合主キーとし、最終取得成功日時を
Unix 時刻のミリ秒で保存する。

配布しないため migration は用意しない。方針は [development.md](development.md) にある。

### 写像

Entity と `domain` のモデルの相互写像は `ForecastEntityMapper` だけが行う。

天気区分は `WeatherCondition` の定数名に依存させず、
`ForecastEntityMapper` の対応表で保存用の文字列と結ぶ。
現在の保存値は定数名と同じ綴り（`CLEAR`、`LIGHT_RAIN` など）であり、これを変えない。
enum を改名しても、保存値は対応表の側で維持する。

保存値が対応表にない、または値オブジェクトの範囲を外れる場合は例外にする。
API の未知コードのように `UNKNOWN` へ落とさない。
保存データの不整合は API の揺れと違い、アプリ側の不具合を示すからである。

### 非同期アクセス

DAO は `LastFetchedAtEntity` を `Maybe`、`ForecastEntity` の一覧を `Single` で個別に読む。
日時が存在しなければ empty、存在すれば予報一覧を読み取った `Snapshot` を返す。
読み取りは二つのクエリであり、両クエリの間に別の保存が完了した場合には
その更新後の予報を読むことがある。
Room-rxjava3 が読み取りを Room の実行基盤へ移す。

保存は DAO の `@Transaction` メソッド `replace` にまとめる。
対象地点の予報の削除・挿入と日時の更新を 1 つのトランザクションで行う。
`ForecastLocalDataSourceImpl` は `Completable.fromAction` でこれを包み、Room の transaction executor 上で実行する。
この DB 内部の実行指定だけは `ForecastLocalDataSourceImpl` の責務とし、通信開始と UI 通知は `presentation` が指定する。
本番コードで blocking 呼び出しは使わない。

### 今回は扱わないこと

- 読み取りの 2 つのクエリを 1 つのトランザクションにまとめること。
  画面は 1 枚で、ViewModel は前の購読を破棄してから次を始める。
  重なり得るのは保存中に再読み込みした場合だけで、そのとき読めるのは期限切れか未取得の日時である。
  判定は通信へ進むので、組み合わせの食い違いが画面に出ることはない
- 同一地点への同時取得の共有や直列化。取得元が複数になった時点で、保存順序と解除の規則を決める
- 期限切れキャッシュへのフォールバック、強制更新、バックグラウンド更新

## 変更するとき

API を差し替えるときは `ForecastRemoteDataSource` の別実装を作り、
`di/RepositoryModule` の 1 行を変える。キャッシュ判定、`domain`、`presentation` は触らない。

保存先を差し替えるときは `ForecastLocalDataSource` の別実装を作り、同じく 1 行を変える。

`remote/` の API・DTO と `util/` の Mapper は `ForecastRemoteDataSourceImpl` 専用、`db/` の Entity・DAO・Mapper は `ForecastLocalDataSourceImpl` 専用であり、他から参照させない。
