# ViewModel

状態を持ち、DataBinding の式から直接読ませる。Android の View を知らない。

## 確認する対象

- `presentation/viewmodel/ForecastViewModel` と `ForecastUiState`
- 対応する `activity_forecast.xml` の binding 式
- `di/` の Module と、UseCase のコンストラクタ注入

## 実装時の注意

- `View`、`Context`、`Activity` を import した時点で誤り。
- 状態は `ReadOnlyForecastState state` の `public final` フィールドで公開する。
  `ReadOnlyForecastState` は `Observable` を実装し、公開 API は `get()` と
  `OnPropertyChangedCallback` の登録・解除だけにする。更新は ViewModel に閉じる。
  同じ参照への更新は通知せず、値を先に差し替えてから `PropertyChangeRegistry` で同期通知する。
  sender は公開した `state` 自身、property ID は `0` にする。
  `LiveData` は使わない。ライフサイクル連動の恩恵はあるが、実行中に画面を離れる想定がなく、
  購読時にメインスレッドへ移送済みのため二重の仕組みになる。
- 読み込み中・成功・失敗は sealed interface `ForecastUiState` の別々の record として表す。
  真偽値のフィールドを並べて持たない。並べると「読み込み中かつ失敗」のような矛盾した組み合わせを型が防げない。
  一覧を持つのは `Success` だけ。`Success` の空リストだけが「結果 0 件」である。
- `ForecastUiState` に `isLoading()` のような判定メソッドを生やさない。どの相かは型が表す。
- 初回の取得はコンストラクタで始める。回転では ViewModel が残るので取り直さず、
  プロセス death からの復帰では作り直されるので取り直す。
- 失敗時のメッセージは ViewModel が文字列として持たない。ViewModel は `Context` を持てないため
  `getString(...)` を呼べない。代わりに `Failure` の相だけを公開し、実際の文言は
  `strings.xml`（`forecast_error_message`）からレイアウト側が解決する。例外の `getMessage()` を
  そのまま状態に載せない。読み手に意味がなく、端末の言語設定とも無関係な英文が出る。
- UseCase と `UiSchedulers` は Hilt が注入する。`@HiltViewModel` を付け、コンストラクタに `@Inject` を書く。
  `ViewModelProvider.Factory` は書かない。Hilt が生成する。
  Activity 側の `@AndroidEntryPoint` を忘れると実行時に落ちる。
- フィールド名とメソッド名を衝突させない。`load()` メソッドと状態フィールド `load` が
  同居すると `@{viewModel.load}` の解決先が曖昧になる。
- 資源を持つ ViewModel は `onCleared()` で必ず解放する。`ForecastViewModel` が持つ購読については
  `subscription.md` を正本とする。破棄の順序と Scheduler の指定はここで重複させない。

## テスト判断

- 状態遷移を変えたときは `ForecastViewModelTest` を足す・直す。
  `UiSchedulers` に trampoline の組を渡せば JVM で同期的に検証できる。
- `UiSchedulers.android()` と binding 式はエミュレータでの確認に回す。

## 検証観点

- `data` 層のクラスを参照していないか。
- レイアウトから参照している名前と、公開しているフィールド名が一致しているか。
- 画面の状態が `ForecastUiState` の 1 つの値に保たれ、真偽値のフィールドが増えていないか。
- 相を足したとき、Activity の `render` が追従しているか。
- 失敗時の文言を `Context` なしで組み立てようとしていないか。
