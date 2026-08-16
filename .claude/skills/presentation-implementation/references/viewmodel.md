# ViewModel

状態を持ち、DataBinding の式から直接読ませる。Android の View を知らない。

## 確認する対象

- `presentation/forecast/ForecastViewModel`
- 対応する `activity_forecast.xml` の binding 式
- `di/ServiceLocator` の UseCase 取得口

## 実装時の注意

- `View`、`Context`、`Activity` を import した時点で誤り。
- 状態は `ObservableField` / `ObservableBoolean` の `public final` フィールドで公開する。
  `LiveData` は使わない。ライフサイクル連動の恩恵はあるが、実行中に画面を離れる想定がなく、
  購読時にメインスレッドへ移送済みのため二重の仕組みになる。
- 読み込み中・成功・失敗を別々のフィールドで公開し、1 つの値で兼ねない。
  `loading`（`ObservableBoolean`）、`forecasts`（`ObservableField<List<DailyForecast>>`）、
  `hasError`（`ObservableBoolean`）の 3 つを独立に持つ。空リストで「読み込み中」と
  「結果 0 件」を兼ねると、利用者にも実装にも区別がつかなくなる。
- 失敗時のメッセージは ViewModel が文字列として持たない。ViewModel は `Context` を持てないため
  `getString(...)` を呼べない。代わりに `hasError` の真偽だけを公開し、実際の文言は
  `strings.xml`（`forecast_error_message`）からレイアウト側が解決する。例外の `getMessage()` を
  そのまま状態に載せない。読み手に意味がなく、端末の言語設定とも無関係な英文が出る。
- UseCase は `ServiceLocator` から取る。コンストラクタ引数を持たせない。
  持たせると `ViewModelProvider.Factory` の実装が要る。
- フィールド名とメソッド名を衝突させない。`load()` メソッドと `ObservableField<...> load` が
  同居すると `@{viewModel.load}` の解決先が曖昧になる。
- 資源を持つ ViewModel は `onCleared()` で必ず解放する。`ForecastViewModel` が持つ購読については
  `subscription.md` を正本とする。破棄の順序と Scheduler の指定はここで重複させない。

## テスト判断

- Android に依存しない判断（状態遷移の条件）を持たせたときは JVM Unit Test を足す。
- `ObservableField` / `ObservableBoolean` と `ServiceLocator`、`AndroidSchedulers.mainThread()` に
  触れる経路は JVM では検証しにくいため、エミュレータでの確認に回す。

## 検証観点

- `data` 層のクラスを参照していないか。
- レイアウトから参照している名前と、公開しているフィールド名が一致しているか。
- 読み込み中・成功・失敗の 3 状態が独立したフィールドのまま保たれているか。
- 失敗時の文言を `Context` なしで組み立てようとしていないか。
