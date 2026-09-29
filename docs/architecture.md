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
- 取得結果を cold なストリームとして扱い、通信開始と UI 通知の Scheduler 指定を `presentation` に集める。
  Scheduler は注入し、状態遷移を JVM で検証できるようにする
- 画面の状態を sealed interface で表し、読み込み中・成功・失敗を別々の型として持つ。
  真偽値を並べて矛盾した組み合わせを作れる形にしない。View は状態を 1 か所で描画する

## 層と依存の向き

```
        presentation                          data
   (Activity / Fragment / ViewModel / Adapter) (Repository 実装 / 通信)
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
| `domain` | `io.reactivex.rxjava3.core`、`javax.inject`、標準 Java | Android SDK、RxJava の `core` 以外（Scheduler・Plugins など）、Retrofit、Moshi、`data`、`presentation`、`di` |
| `data` | `domain`、`javax.inject`、RxJava 全般、Retrofit、Moshi、OkHttp。`db` と `datasource` のみ Room・AndroidX SQLite・AndroidX annotation | Android SDK、その他の AndroidX、RxAndroid、Hilt の Module 注釈（`@Module` `@Provides` `@Binds`）、`presentation`、`di` |
| `presentation` | `domain`、Android SDK、RxAndroid、DataBinding、Hilt | `data`、Retrofit、Moshi、OkHttp |
| `di` | すべて | — |

`data` の内側にも境界がある。

| package | 置くもの | 参照してよいのは |
| --- | --- | --- |
| `data/remote` | Retrofit の `ForecastApi`、Moshi の DTO `ForecastResponse` | `data/remote`、`data/util`、`data/datasource`、`di` |
| `data/util` | DTO から domain への写像 `RemoteForecastMapper` | `data/datasource` |
| `data/db` | `ForecastDatabase`、`ForecastEntityMapper`。`dao/` に DAO、`entity/` に Entity | `data/db`、`data/datasource`、`di` |
| `data/datasource` | DataSource のインタフェースと `〜Impl`。Local と Remote に分ける | `data/repository` と `di` |
| `data/repository` | `ForecastRepositoryImpl`、`TimeProvider` | `di` |

DataSource は provider（Retrofit、Room）を閉じ込める。
インタフェースは domain の型だけで宣言し、`〜Impl` がその内側で写像を適用する。
`ForecastRemoteDataSourceImpl` は `remote` と `util` を、`ForecastLocalDataSourceImpl` は `db` を使う。
`ForecastRepositoryImpl` は 2 つの DataSource のインタフェースだけに依存し、`remote` も `db` も知らない。
組み合わせは `di` が決める。

単一モジュールのため、これらの表はコンパイラでは守られない。
「してはならない」列、`data` 内の境界、Retrofit と Moshi の型を `data/remote/` と `di/` の外へ出さない規則、
`AndroidSchedulers` を `presentation` の外で使わない規則は
`app/src/test/java/com/example/rxjava/ArchitectureTest.java` が ArchUnit で検査する。
`domain`・`data` から組み立てを担当する `di` への参照と、`domain` から RxJava の `core` 以外への参照も検査する。
Hilt の Module 注釈は生成コードが参照する `dagger.internal` と区別するため、
package ではなく注釈を対象に検査する。
正本はこの表で、テストはその写しである。表を変えたらテストも合わせて変える。

```
com.example.rxjava/
  ForecastApplication
  domain/       model/ repository/ usecase/
  data/         datasource/ db/（dao/ entity/） remote/ repository/ util/
  presentation/ activity/ fragment/ adapter/ viewmodel/ util/
  di/           NetworkModule DatabaseModule RepositoryModule SchedulerModule
```

Retrofit と Moshi の型が `data/remote/` の外へ出たら、それは設計の誤りである。
Room の Entity と DAO が `data/db/` と `data/datasource/` の外へ出た場合も同じである。
`ForecastRepository` が返すのは `domain` のモデルだけで、
API のレスポンス DTO、JSON の項目名、テーブルの行は境界の内側に残す。

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

Hilt を使う。`di/` の Module が生成手順を宣言し、注入は Hilt が行う。

| 置き場所 | 内容 |
| --- | --- |
| `ForecastApplication` | `@HiltAndroidApp`。コンポーネントの起点 |
| `di/NetworkModule` | `OkHttpClient`、`Moshi`、`Retrofit`、`ForecastApi` |
| `di/DatabaseModule` | `ForecastDatabase`。`Context` を使う生成はここだけに置く |
| `di/RepositoryModule` | `ForecastRepository`・`ForecastRemoteDataSource`・`ForecastLocalDataSource` の実装の選択と `TimeProvider` |
| `di/SchedulerModule` | `UiSchedulers`。中身は `UiSchedulers.android()` に任せる |

`OkHttpClient` は生成コストが高く接続プールを持つため `@Singleton` で 1 つだけ作る。
API の実装を差し替えるときは `RepositoryModule` の `ForecastRemoteDataSource` の 1 行を変える。
キャッシュ判定はそのまま残る。

### コンストラクタ注入を使う

自分たちが書いたクラスは、コンストラクタに `@Inject` を付けて Hilt に組み立てさせる。
`ForecastRepositoryImpl`、`ForecastRemoteDataSourceImpl`、`ForecastLocalDataSourceImpl`、
`GetForecastUseCase`、`ForecastViewModel` が該当する。

インタフェースと実装の対応は `RepositoryModule` の `@Binds` で結ぶ。
`@Binds` は abstract メソッドの宣言だけで済み、`@Provides` より生成コードが小さい。

`@Provides` が要るのは自分で `new` できない他社製の型だけで、
`OkHttpClient`・`Moshi`・`Retrofit`・`ForecastApi` を組み立てる `NetworkModule` がそれにあたる。
`UiSchedulers` はアプリ用とテスト用で中身が変わる値なので、コンストラクタ注入ではなく
`SchedulerModule` の `@Provides` で作る。
`ForecastDatabase` は `Room.databaseBuilder` でしか作れないため `DatabaseModule` の `@Provides` で作る。
`TimeProvider` は `System::currentTimeMillis` を渡すだけなので、実装クラスを作らず
`RepositoryModule` の static `@Provides` で作る。

`javax.inject` は JSR-330 の標準アノテーションで、Dagger の実行時にも Android にも依存しない。
層を守る目的は Android と Retrofit を内側へ入れないことなので、ここには抵触しない。

## 永続化とキャッシュ

取得した予報は Room に保存し、24 時間はそれを返す。
配置は 3 つに分ける。

| 役割 | インタフェース | 実装 |
| --- | --- | --- |
| 通信と写像 | `datasource/ForecastRemoteDataSource` | `datasource/ForecastRemoteDataSourceImpl` |
| 保存と復元 | `datasource/ForecastLocalDataSource` | `datasource/ForecastLocalDataSourceImpl` |
| キャッシュ判定 | `ForecastRepository`（domain） | `repository/ForecastRepositoryImpl` |

`ForecastRepositoryImpl` は `ForecastRemoteDataSource`・`ForecastLocalDataSource`・`TimeProvider` だけに依存し、
Retrofit も Room も知らない。キャッシュ判定は fake だけで JVM で検証できる。

### 非同期境界

`ForecastLocalDataSource` の読み取りは `Maybe`、保存は `Completable` とし、どちらも cold にする。
`Maybe` の empty が「未取得」を表す。未取得を `null` や空リストで表さない。
Room-rxjava3 の単発クエリと、Room の transaction executor 上の保存で DB 操作を完結させる。
`ForecastLocalDataSourceImpl` に限り DB 実行基盤への `subscribeOn` を許可する。
それ以外の `data` は Scheduler を指定しない。
通信開始と UI 通知の Scheduler は引き続き presentation が指定する。

保存した値が domain へ写せないとき、読み取りは `onError` になり通信へは進まない。
壊れたキャッシュを未取得として扱うと、データの異常に気づけなくなる。
