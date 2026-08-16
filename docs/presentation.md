# presentation 層

MVVM で組む。View はレイアウトを束ねるだけで、状態と処理は ViewModel が持つ。

## 依存

| import してよい | してはならない |
| --- | --- |
| `domain`、Android SDK、RxAndroid、DataBinding、Hilt | `data`、Retrofit、Moshi、OkHttp |

`presentation` が `data` のクラス名を書いたら設計の誤りである。
実体の組み立ては `di/` の Module に閉じる。

## クラス

| クラス | 役割 |
| --- | --- |
| `activity/ForecastActivity` | View。ランチャー。RecyclerView を組み立て、結果をアダプタへ渡す |
| `viewmodel/ForecastViewModel` | 予報の取得と、読み込み中・成功・失敗の状態を公開する |
| `adapter/ForecastAdapter` | `item_forecast.xml` に `DailyForecast` を渡す |
| `util/ForecastFormatter` | `WeatherCondition` を文字列リソース ID へ写す |

## View の責務

View がしてよいのは 3 つだけ。

- レイアウトを `DataBindingUtil` で束ね、変数を渡す
- `RecyclerView` の `LayoutManager` と `Adapter` を設定する
- 画面遷移の `Intent` を組む

条件分岐・整形・購読は書かない。
`findViewById` と `setOnClickListener` は使わない。
ボタン押下はレイアウト側で ViewModel のメソッドへ直接束ねる。

```xml
<Button android:onClick="@{() -> viewModel.load()}" />
```

初回の取得は `savedInstanceState == null` のときだけ呼ぶ。
ViewModel は画面回転をまたいで生存するため、無条件に呼ぶと回転のたびに通信が走る。

## ViewModel の責務

ViewModel は Android の View を知らない。
`View`・`Context`・`Activity` を import した時点で誤りである。

状態は `ObservableField` / `ObservableBoolean` の public final フィールドで公開し、
DataBinding の式から直接読ませる。`LiveData` は使わない。
ライフサイクル連動の恩恵はあるが、実行中に画面を離れる想定がなく、
購読時にメインスレッドへ移送済みのため二重の仕組みになる。

UseCase は Hilt が注入する。`@HiltViewModel` を付け、コンストラクタに `@Inject` を書く。
`ViewModelProvider.Factory` は書かない。Hilt が生成する。
Activity 側には `@AndroidEntryPoint` が要る。付け忘れると実行時に落ちる。

フィールド名とメソッド名を衝突させない。
同名の `ObservableField` とメソッドが同居すると、binding 式の解決先が曖昧になる。

## 画面の状態

状態を 3 つに分けて公開する。

| 状態 | 公開する値 |
| --- | --- |
| 読み込み中 | `ObservableBoolean loading` |
| 成功 | `ObservableField<List<DailyForecast>> forecasts` |
| 失敗 | `ObservableBoolean hasError` |

1 つの値で兼ねない。空リストで「読み込み中」と「結果 0 件」を兼ねると、
利用者にも実装にも区別がつかなくなる。

失敗は真偽値で公開し、メッセージ文字列は ViewModel が持たない。
ViewModel は `Context` を持てないため文字列リソースを引けず、
例外の `getMessage()` をそのまま出すのは読み手に意味がないうえ、
端末の言語設定とも無関係な英文になる。
表示する文言はレイアウト側で `@string/forecast_error_message` を直接参照する。

再試行は `load()` をそのまま使う。取得と再試行は同じ操作である。

## 購読とスレッド

`ForecastViewModel` が購読を 1 つだけ保持する。

| きっかけ | 挙動 |
| --- | --- |
| 初回表示・再試行 | 既存の購読を破棄し、失敗状態を落として読み込み中にしてから購読する |
| 成功 | 読み込み中を落とし、結果を公開する |
| 失敗 | 読み込み中を落とし、失敗状態を立てる |
| `onCleared()` | 購読を破棄する |

`GetForecastUseCase` が返すのは素の cold ストリームなので、
スレッドの指定はここで付ける。

```java
getForecast.invoke(Location.TOKYO)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleSuccess, this::handleError);
```

`AndroidSchedulers.mainThread()` を書いてよいのはこの層だけ。

## 表示への写像

`domain` のモデルは書式を持たないため、組み立ては presentation で行う。

| 対象 | 方法 |
| --- | --- |
| 日付 | `@string/forecast_date_format` に年月日を渡す |
| 気温・風速 | 書式文字列に値オブジェクトの数値を渡す |
| 天気 | `ForecastFormatter.weatherLabelResId` が返す ID を、binding の暗黙の `context` で解決する |

`WeatherCondition` から日本語への写像だけは分岐が要るため Java 側に置く。
`ForecastFormatter` は `Context` を受け取らず `@StringRes int` を返すだけにして、
日本語のラベルが `strings.xml` の外へ出ないようにする。

## 画面の余白とシステムバー

`targetSdk` が 35 以上のため edge-to-edge が強制され、
コンテンツはシステムバーの裏から描画される。
何もしないとステータスバーと先頭の項目が重なる。

| 対処 | 場所 |
| --- | --- |
| ルートに `android:fitsSystemWindows="true"` | 各画面のレイアウト |
| テーマは `NoActionBar` 系 | `themes.xml` |
| `android:windowLightStatusBar` を明暗で出し分け | `values/themes.xml` と `values-night/themes.xml` |

ActionBar は使わない。画面のタイトルはレイアウト内の `TextView` で出す。
ActionBar を残すと `android:label` と画面内のタイトルが二重に出る。

`windowLightStatusBar` を出し分けないと、明るい背景に白いアイコンが乗って読めなくなる。

## DataBinding

| レイアウト | 受け取る変数 |
| --- | --- |
| `activity_forecast.xml` | `viewModel` |
| `item_forecast.xml` | `forecast` |

表示の出し分けは binding 式で書く。
`<import type="android.view.View"/>` を置き、`android:visibility` を
状態から導く。Activity 側で `setVisibility` を呼ばない。

`ForecastAdapter` は binding を使い回すため、
値を差し替えたあとに `executePendingBindings()` を呼ぶ。

一覧の反映は `ObservableField` の `OnPropertyChangedCallback` で受ける。
DataBinding は `List` を `RecyclerView` へ直接流せないため、この橋渡しが要る。

**登録直後に現在値を自分で流すこと。** コールバックは値が変わったときにしか発火しない。
画面回転で Activity だけが作り直されたとき、ViewModel は値を保持済みで変更が起きないため、
新しい Adapter へ何も届かず一覧が空になる。

コールバックは `onDestroy()` で解除する。ViewModel は Activity より長生きするので、
解除しないと回転のたびに古い Adapter への参照が積み上がる。

一覧の差し替えは `notifyDataSetChanged()` で足りる。
7 件の全面差し替えしか起きないため、`DiffUtil` は要らない。

## 変更するとき

画面を増やすときは `presentation` の下にパッケージを 1 つ足し、
Activity と ViewModel を対で置く。
`domain` と `data` に手を入れる必要が出たら、その画面の要求が
表示の都合ではなくアプリの語彙の変化であることを先に確認する。
