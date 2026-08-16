# アーキテクチャ

Clean Architecture の 3 層。`presentation` の内部は MVVM。
各層の詳細は層ごとの文書にある。

| 層 | 文書 |
| --- | --- |
| `domain` | [domain.md](domain.md) |
| `data` | [data.md](data.md) |
| `presentation` | [presentation.md](presentation.md) |

## このアプリが解きたいこと

7Timer! の API から東京の 7 日予報を取得して表示する。画面は 1 枚。

小さなアプリだが、非同期処理が持つ問題は一通り出る。
待っている状態、失敗した状態、再試行、画面回転をまたぐ購読の扱いである。
これを RxJava と Clean Architecture でどう配置するかがこのアプリの主題になる。

この目的から、設計の軸は 3 つになる。

- 通信の詳細（Retrofit、Moshi、JSON の形）を `data` に閉じ、
  `domain` から上には RxJava の型と自前のモデルしか出さない
- 取得結果を cold なストリームとして扱い、Scheduler の指定を `presentation` に集める
- 読み込み中・成功・失敗を別々の状態として公開し、1 つの値で兼ねない

## 層と依存の向き

```
        presentation                          data
   (Activity / ViewModel / Adapter)      (Repository 実装 / 通信)
                  │                             │
                  └──────────┐     ┌────────────┘
                             ▼     ▼
                           domain
              (モデル / UseCase / Repository インタフェース)
```

依存は常に外側から内側へ向かう。`domain` は `presentation` も `data` も知らない。
`data` が `domain` の Repository インタフェースを実装することで、依存が内向きに反転する。

| 層 | import してよい | してはならない |
| --- | --- | --- |
| `domain` | `io.reactivex.rxjava3.core`、標準 Java | Android SDK、RxAndroid、Retrofit、Moshi、`data`、`presentation` |
| `data` | `domain`、RxJava 全般、Retrofit、Moshi、OkHttp | Android SDK、RxAndroid、`presentation` |
| `presentation` | `domain`、`di`、Android SDK、RxAndroid、DataBinding | `data`、Retrofit、Moshi、OkHttp |

```
com.example.rxjava/
  domain/       model/ repository/ usecase/
  data/         remote/ repository/
  presentation/ activity/ adapter/ viewmodel/ util/
  di/           ServiceLocator
```

Retrofit と Moshi の型が `data/remote/` の外へ出たら、それは設計の誤りである。
`ForecastRepository` が返すのは `domain` のモデルだけで、
API のレスポンス DTO や JSON の項目名は境界の内側に残す。

## 中心にある設計

取得結果を `Single<List<DailyForecast>>` として表現する。

- **cold である。** `ForecastRepository.find()` も UseCase も、購読するまで通信しない。
  Scheduler の指定もしない。`subscribeOn(Schedulers.io())` と
  `observeOn(AndroidSchedulers.mainThread())` は `presentation` が付ける
- **失敗を握り潰さない。** 通信の失敗は `onError` として上げる。
  空リストや `null` に潰すと、待ちと失敗の区別が画面から消える
- **API の語彙を持ち込まない。** `weather` の `"lightrain"` のような文字列コードは
  `data` で `WeatherCondition` へ写す。未知のコードは `UNKNOWN` に落とし、
  通信自体は成功として扱う
- **表示形式を知らない。** 日付の書式も気温の単位表記も `presentation` の仕事

地点は `Location` 値オブジェクトで表し、`Location.TOKYO` を定数として持つ。
固定であることを型の外に出しておくと、増やすときに Repository の署名を変えずに済む。

## 値オブジェクト

生の `int` や `String` を層をまたいで運ばない。
`DailyForecast` が持つのは `int` ではなく `Temperature` と `WindSpeed` である。

不変条件は生成時に検査し、壊れた値を画面まで運ばずその場で止める。
検査は入口の早期リターンで書き、正常系を分岐の外に出す。
一覧と範囲は [domain.md](domain.md) にある。

## 依存の解決

`di/ServiceLocator` が Repository と UseCase を組み立てる遅延シングルトン。
ViewModel はここから UseCase を取る。

OkHttp と Retrofit と Moshi の組み立てもここに置く。
`OkHttpClient` は生成コストが高く接続プールを持つため、シングルトンで 1 つだけ作る。

依存グラフは Repository 1 つと UseCase 1 つしかなく、
結線が目で追えることに価値があるため、DI ライブラリは使わない。
API の実装を差し替えるときは、ここの 1 行を変える。

---

この文書は完成形の設計を書いている。
実装がどこまで追いついているかは [status.md](status.md) を参照。
