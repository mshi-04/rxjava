# presentation 層

MVVM で組む。状態と処理は ViewModel が持ち、View は状態を受け取って描画するだけにする。

## 依存

| import してよい | してはならない |
| --- | --- |
| `domain`、Android SDK、RxAndroid、DataBinding、Hilt | `data`、Retrofit、Moshi、OkHttp |

`presentation` が `data` のクラス名を書いたら設計の誤りである。
実体の組み立ては `di/` の Module に閉じる。

## クラス

| クラス | 役割 |
| --- | --- |
| `activity/ForecastActivity` | ランチャー兼ホスト。`activity_forecast.xml` を `setContentView` で設定する |
| `fragment/ForecastFragment` | 画面の View。状態を受け取り `render` で反映する |
| `viewmodel/ForecastViewModel` | 予報の取得と、画面の状態を公開する |
| `viewmodel/ForecastUiState` | 画面の状態。読み込み中・成功・失敗のどれか 1 つ |
| `adapter/ForecastAdapter` | `item_forecast.xml` に `DailyForecast` を渡す |
| `util/ForecastFormatter` | `WeatherCondition` を文字列リソース ID へ写す |
| `util/UiSchedulers` | 取得と反映に使う Scheduler の組 |

## View の責務

View がしてよいのは次だけ。

- Activity はホストのレイアウトを `DataBindingUtil.setContentView` で設定する
- Fragment は画面の binding を `onCreateView` で作り、`viewModel` を渡す
- `RecyclerView` の `LayoutManager` と `Adapter` を設定する
- `state` を購読し、`render(ForecastUiState)` で画面へ反映する
- 画面遷移の `Intent` を組む

状態による分岐は `render` の 1 か所にだけ書く。
Kotlin の `when` による網羅に相当する場所で、相ごとの表示の出し分けと
一覧の受け渡しがここに集まる。整形と購読は書かない。

```java
private void render(ForecastUiState state) {
    binding.loadingIndicator.setVisibility(visibleIf(state instanceof ForecastUiState.Loading));
    binding.forecastList.setVisibility(visibleIf(state instanceof ForecastUiState.Success));
    binding.errorContainer.setVisibility(visibleIf(state instanceof ForecastUiState.Failure));
    if (state instanceof ForecastUiState.Success success) {
        adapter.submit(success.forecasts());
    }
}
```

Java 17 の `switch` は sealed 型の網羅を検査できない。
相を足したときは `render` を必ず見直す。

`findViewById` と `setOnClickListener` は使わない。
ビューは binding のフィールドで参照し、ボタン押下はレイアウト側で ViewModel のメソッドへ束ねる。

```xml
<Button android:onClick="@{() -> viewModel.load()}" />
```

View は取得を始めない。取得は ViewModel の生成時に始まる。

## ViewModel の責務

ViewModel は Android の View を知らない。
`View`・`Context`・`Activity` を import した時点で誤りである。

状態は `ReadOnlyForecastState state` の public final フィールドで公開する。
この型は `ForecastViewModel` 内の `public static final` なクラスで、`Observable` を実装する。
公開する操作は現在値を返す `get()` と、`OnPropertyChangedCallback` の登録・解除だけにする。
値の更新は ViewModel だけが行い、画面から変更できないようにする。
更新時は同じ参照なら通知せず、値を先に差し替えてから `PropertyChangeRegistry` で同期的に通知する。
通知の sender は公開した `state` 自身、property ID は `0` とする。
`LiveData` は使わない。
ライフサイクル連動の恩恵はあるが、実行中に画面を離れる想定がなく、
購読時にメインスレッドへ移送済みのため二重の仕組みになる。

初回の取得はコンストラクタで始める。
ViewModel は画面回転では生き残り、プロセス death からの復帰では作り直される。
生成時に取得すれば、回転では取り直さず、復帰では取り直す、という区別が自然に付く。
View が `savedInstanceState` や ViewModel の状態を見て取得を判断する必要がない。

UseCase と `UiSchedulers` は Hilt が注入する。`@HiltViewModel` を付け、コンストラクタに `@Inject` を書く。
`ViewModelProvider.Factory` は書かない。Hilt が生成する。
Activity と Fragment の両方に `@AndroidEntryPoint` が要る。付け忘れると実行時に落ちる。
ViewModel は Fragment の `onCreate` で `new ViewModelProvider(this)` から取得する。
Fragment の View が破棄されて再生成されても ViewModel と進行中の取得は保持する。

フィールド名とメソッド名を衝突させない。
同名の状態フィールドとメソッドが同居すると、binding 式の解決先が曖昧になる。

## 画面の状態

状態は sealed interface の `ForecastUiState` で表し、読み取り専用の `state` で公開する。

```java
public sealed interface ForecastUiState {

    record Loading() implements ForecastUiState {}

    record Success(List<DailyForecast> forecasts) implements ForecastUiState { ... }

    record Failure() implements ForecastUiState {}
}
```

相ごとに型を分け、その相が持つ値だけを持たせる。
一覧を持つのは `Success` だけで、「失敗なのに一覧がある」状態は作れない。
空リストで「読み込み中」と「結果 0 件」を兼ねることもない。`Success` の空リストだけが「結果 0 件」である。

真偽値のフィールドを並べて状態を表さない。`loading` と `hasError` が独立していると、
両方が立つ組み合わせを型が防げない。

相を判定するメソッド（`isLoading()` など）を `ForecastUiState` に生やさない。
どの相かは型そのものが表しており、判定は `render` の `instanceof` で行う。

`Success` はコンストラクタで `List.copyOf` を通す。null を拒み、受け取ったリストの
あとからの変更を遮断し、変更不可にする。

失敗した `Throwable` は捨てずに `Log.w` で残す。画面に出さないことと、
原因を追えなくすることは別である。

状態の更新中に例外が出る経路も塞ぐ。`subscribe(onSuccess, onError)` の `onError` は
上流のエラー専用で、`onSuccess` が投げた例外は `RxJavaPlugins.onError` へ回りアプリが落ちる。
`state.set()` は Fragment の `render` と Adapter まで同期的に呼ぶため、
その連鎖の失敗も `Failure` として扱う。

`Failure` はメッセージ文字列を持たない。
ViewModel は `Context` を持てないため文字列リソースを引けず、
例外の `getMessage()` をそのまま出すのは読み手に意味がないうえ、
端末の言語設定とも無関係な英文になる。
表示する文言はレイアウト側で `@string/forecast_error_message` を直接参照する。

再試行は `load()` をそのまま使う。取得と再試行は同じ操作である。

## 購読とスレッド

`ForecastViewModel` が購読を 1 つだけ保持する。
保持には `SerialDisposable` を使い、`set()` で差し替えると直前の購読が破棄される。
`onCleared()` で入れ物ごと破棄した後に `set()` された購読も、その場で破棄される。

| きっかけ | 挙動 |
| --- | --- |
| 生成・再試行 | 既存の購読を破棄し、`Loading` にしてから購読する |
| 成功 | `Success` にして結果を渡す |
| 失敗 | `Failure` にし、原因を logcat へ残す |
| `onCleared()` | 購読を破棄する |

`GetForecastUseCase` が返すのは素の cold ストリームなので、
スレッドの指定はここで付ける。Scheduler は直接書かず、注入された `UiSchedulers` から取る。

```java
getForecastUseCase.invoke(Location.TOKYO)
        .subscribeOn(schedulers.background())
        .observeOn(schedulers.main())
        .subscribe(this::publishSuccess, this::publishError);
```

`UiSchedulers` は `background` と `main` の組を持つだけの値である。
アプリでは `UiSchedulers.android()` が `Schedulers.io()` と
`AndroidSchedulers.mainThread()` の組を返し、`di/SchedulerModule` がそれを提供する。
JVM のテストでは `Schedulers.trampoline()` の組を渡し、状態遷移を同期的に検証する。

`AndroidSchedulers.mainThread()` を書いてよいのはこの層だけで、`UiSchedulers.android()` の 1 か所に置く。
`di/` の Module は `UiSchedulers.android()` を呼ぶだけで、`AndroidSchedulers` を書かない。

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
| ルートに `android:fitsSystemWindows="true"` | `fragment_forecast.xml` の画面ルートのみ |
| テーマは `NoActionBar` 系 | `themes.xml` |
| `android:windowLightStatusBar` を明暗で出し分け | `values/themes.xml` と `values-night/themes.xml` |

ActionBar は使わない。画面のタイトルはレイアウト内の `TextView` で出す。
ActionBar を残すと `android:label` と画面内のタイトルが二重に出る。

`windowLightStatusBar` を出し分けないと、明るい背景に白いアイコンが乗って読めなくなる。

## DataBinding

| レイアウト | 受け取る変数 |
| --- | --- |
| `activity_forecast.xml` | なし。固定 ID の `FragmentContainerView` に `android:name` で `ForecastFragment` を配置する |
| `fragment_forecast.xml` | `viewModel` |
| `item_forecast.xml` | `forecast` |

読み込み中・一覧・失敗の各ビューには `android:id` を付ける。
id が無いと Espresso から名指しできず、その状態がテストで検証できなくなる。

`fragment_forecast.xml` の binding 式は再試行ボタンの押下だけに使う。
表示の出し分けは `render` で行い、binding 式に状態の判定を書かない。
判定が XML と Java に分かれると、相を足したときに片方を見落とす。

`ForecastAdapter` は binding を使い回すため、
値を差し替えたあとに `executePendingBindings()` を呼ぶ。

`state` の変更は `OnPropertyChangedCallback` で受け、`render` へ渡す。

**登録直後に現在の状態を自分で `render` すること。** コールバックは値が変わったときにしか発火しない。
画面回転や Fragment の View 再生成時、ViewModel は値を保持済みで変更が起きないため、
新しい View に何も描画されない。

コールバックは `onDestroyView()` で解除する。RecyclerView の Adapter を外し、
binding を unbind して binding・Adapter・コールバックの参照を消す。
ViewModel と取得の購読はここで破棄しない。View の再生成時に新しい binding と Adapter を作り、
現在の状態を描画する。

一覧の差し替えは `notifyDataSetChanged()` で足りる。
7 件の全面差し替えしか起きないため、`DiffUtil` は要らない。

ただし lint が `NotifyDataSetChanged` を指摘するため、
`@SuppressLint` と理由のコメントを添えて黙らせる。
抑制しないと警告が毎回残り、本当に見るべき指摘が埋もれる。

## 変更するとき

画面を増やすときは `presentation` の下にパッケージを 1 つ足し、
画面の表示は Fragment に置き、Activity はホストに保つ。
`domain` と `data` に手を入れる必要が出たら、その画面の要求が
表示の都合ではなくアプリの語彙の変化であることを先に確認する。
