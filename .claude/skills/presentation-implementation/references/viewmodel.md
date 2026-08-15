# ViewModel

状態を持ち、DataBinding の式から直接読ませる。Android の View を知らない。

## 確認する対象

- `presentation/list/SampleListViewModel` と `presentation/run/SampleRunViewModel`
- 対応するレイアウトの binding 式
- `di/ServiceLocator` の UseCase 取得口

## 実装時の注意

- `View`、`Context`、`Activity` を import した時点で誤り。
- 状態は `ObservableField` / `ObservableBoolean` の `public final` フィールドで公開する。
  `LiveData` は使わない。ライフサイクル連動の恩恵はあるが、実行中に画面を離れる想定がなく、
  購読時にメインスレッドへ移送済みのため二重の仕組みになる。
- UseCase は `ServiceLocator` から取る。コンストラクタ引数を持たせない。
  持たせると `ViewModelProvider.Factory` の実装が要る。
- フィールド名とメソッド名を衝突させない。
  `ObservableField<String> log` と `void log(String)` が同居すると
  `@{viewModel.log}` の解決先が曖昧になる。ログのフィールド名は `logText` とする。
- 表示用の整形（`[スレッド名] メッセージ` の組み立てなど）はここで行う。
  domain の model に表示形式を持たせない。
- 資源を持つ ViewModel は `onCleared()` で必ず解放する。
  `SampleRunViewModel` が持つ購読については `subscription.md` を正本とする。
  破棄の順序と Scheduler の指定はここで重複させない。

## テスト判断

- Android に依存しない判断（整形、状態遷移の条件）を持たせたときは JVM Unit Test を足す。
- `ObservableField` と `ServiceLocator` に触れる経路は JVM では検証しにくいため、
  テストしたいロジックはそれらから切り離した private メソッドに寄せる。

## 検証観点

- `data` 層のクラスを参照していないか。
- レイアウトから参照している名前と、公開しているフィールド名が一致しているか。
- 状態の公開が `ObservableField` / `ObservableBoolean` に揃っているか。
