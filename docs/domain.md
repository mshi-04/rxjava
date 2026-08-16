# domain 層

アプリの語彙を定義する最内層。何が「予報」で、何が「気温」かをここで決める。

## 依存

| import してよい | してはならない |
| --- | --- |
| `io.reactivex.rxjava3.core`、標準 Java | Android SDK、RxAndroid、Retrofit、Moshi、`data`、`presentation` |

RxJava core だけは例外的に許す。
予報の取得は待ちを伴う操作であり、`Single` という型はこの層の語彙に属する。
ここを自前の抽象で包むと、RxJava を包み直すインタフェースが増えるだけになる。

RxAndroid は入れない。Scheduler の指定は domain の関心事ではない。

## クラス

| クラス | 役割 |
| --- | --- |
| `model/Location` | 緯度経度。`TOKYO` を定数で持つ |
| `model/ForecastDate` | 年月日。書式は持たない |
| `model/Temperature` | 摂氏の気温 |
| `model/TemperatureRange` | 最高と最低の組 |
| `model/WindSpeed` | 風速階級 |
| `model/WeatherCondition` | 天気の区分。未知のコード用に `UNKNOWN` を持つ |
| `model/DailyForecast` | 1 日分の予報 |
| `repository/ForecastRepository` | 予報の取得の契約 |
| `usecase/GetForecastUseCase` | 指定した地点の予報を返す |

## 値オブジェクト

生の `int` や `String` を層をまたいで運ばない。
`DailyForecast` が持つのは `int` ではなく `Temperature` と `WindSpeed` である。

| 型 | 不変条件 |
| --- | --- |
| `Temperature` | 摂氏 -100〜100 |
| `TemperatureRange` | 最高が最低を下回らない |
| `WindSpeed` | 階級 0〜12 |
| `ForecastDate` | 月 1〜12、日 1〜31 |
| `Location` | 緯度 -90〜90、経度 -180〜180 |

`Temperature` と `WindSpeed` の範囲は API の契約そのものではなく、
値が壊れていることを検知するための下限・上限である。
API は気温 -76〜60、風速階級 1〜8 を返すと文書化しているが、
そこに合わせて厳しく縛ると、API 側の小さな変更でアプリ全体が落ちる。

書き方は共通で、`private` コンストラクタと `static` ファクトリ、
入口での早期リターンによる検査、`equals` を必要とする型だけ `equals` と `hashCode` を持つ。

### 表示形式を持たせない

`ForecastDate` は年月日の整数 3 つだけを持ち、書式を知らない。
`WeatherCondition` は enum で、日本語のラベルを持たない。
`Temperature` は摂氏の数値を持つが、`℃` という記号は持たない。

いずれも `presentation` が `strings.xml` と組み合わせて組み立てる。

`java.time` を使わないのは `minSdk = 24` のためで、
core library desugaring を足す価値がこの用途にはない。

## WeatherCondition

API が返す `"lightrain"` のような文字列コードは持たない。
コードから enum への写像は `data` の責務である。

未知のコードは `UNKNOWN` に落とし、通信そのものは成功として扱う。
API が値を増やしたときにアプリが落ちるより、その日だけ天気が不明になる方がよい。

## ForecastRepository

インタフェースを **domain が所有** し、`data` が実装する。
これで依存が内向きに反転し、`domain` は予報の出所を知らずに済む。

`Single<List<DailyForecast>> find(Location location)` を返す。

cold であり、購読するまで通信しない。Scheduler も指定しない。
通信の失敗は `onError` として上げ、空リストや `null` に潰さない。
潰すと、待っている状態と失敗した状態が画面から区別できなくなる。

## GetForecastUseCase

1 つの操作につき 1 クラス、公開メソッドは `invoke` のみ。

現状は Repository への委譲 1 行しか持たない。
それでも置くのは、`presentation` が Repository のインタフェースへ直接依存すると、
地点の既定値や取得の再試行といった規則の置き場が `presentation` へ流れるためである。

## 変更するとき

エンティティを増やす前に、それが本当にアプリの語彙かを確認する。
表示のための値（整形済み文字列、色、アイコン）は `presentation` に置く。
API の都合の値は `data` に置く。
