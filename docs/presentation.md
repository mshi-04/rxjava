# presentation 層

MVVM で組む。View はレイアウトを束ねるだけで、状態と処理は ViewModel が持つ。

## 依存

| import してよい | してはならない |
| --- | --- |
| `domain`、`di`、Android SDK、RxAndroid、DataBinding | `data` |

`presentation` が `data` のクラス名を書いたら設計の誤りである。
実体の組み立ては `di/ServiceLocator` に閉じる。

## クラス

| クラス | 役割 |
| --- | --- |
| `list/MainActivity` | View。一覧を `RecyclerView` へ渡す |
| `list/SampleListViewModel` | `GetSampleListUseCase` の結果を保持する |
| `list/SampleListAdapter` | `item_sample.xml` に `Sample` とクリックリスナを渡す |
| `run/SampleActivity` | View。`SampleId` を受け取り ViewModel をレイアウトへ束ねる |
| `run/SampleRunViewModel` | 購読の保持、ログの組み立て、実行状態の公開 |

## View の責務

View がしてよいのは 3 つだけ。

- レイアウトを `DataBindingUtil` で束ね、変数を渡す
- `RecyclerView` の `LayoutManager` と `Adapter` を設定する
- 画面遷移の `Intent` を組む

条件分岐・整形・購読は書かない。
`findViewById` と `setOnClickListener` は使わない。
ボタン押下はレイアウト側で ViewModel のメソッドへ直接束ねる。

```xml
<Button
    android:enabled="@{viewModel.running}"
    android:onClick="@{() -> viewModel.stop()}" />
```

## ViewModel の責務

ViewModel は Android の View を知らない。
`View`・`Context`・`Activity` を import した時点で誤りである。

状態は `ObservableField` / `ObservableBoolean` の public final フィールドで公開し、
DataBinding の式から直接読ませる。`LiveData` は使わない。
ライフサイクル連動の恩恵はあるが、実行中に画面を離れる想定がなく、
購読時にメインスレッドへ移送済みのため二重の仕組みになる。

UseCase は `ServiceLocator` から取る。コンストラクタ引数を持たせないことで
`ViewModelProvider.Factory` の実装が不要になる。

### 命名の注意

フィールド名とメソッド名を衝突させない。
`ObservableField<String> log` と `void log(String)` が同居すると
`@{viewModel.log}` の解決先が曖昧になるため、フィールド名は `logText` とする。

## 購読とスレッド

`SampleRunViewModel` が購読を 1 つだけ保持する。

| きっかけ | 挙動 |
| --- | --- |
| 実行ボタン | 既存の購読を破棄し、ログを空にしてから再購読する |
| 停止ボタン | 購読を破棄する |
| ストリームの終了・エラー | 実行状態を落とす |
| `onCleared()` | 購読を破棄する |

`RunSampleUseCase` が返すのは素の cold ストリームなので、
表示のためのスレッド移送はここで付ける。

```java
runSample.invoke(sample.getId())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::appendLine, this::appendError, this::markFinished);
```

ログ行の `[スレッド名] メッセージ` という整形もここで行う。
`SampleLogLine` は値を持つだけで、表示形式を知らない。

画面回転で ViewModel は生存するため、購読とログはそのまま引き継がれる。
無限ストリームを回したまま回転させても購読は二重にならない。

## DataBinding

| レイアウト | 受け取る変数 |
| --- | --- |
| `activity_main.xml` | なし（`RecyclerView` の参照取得のみ） |
| `item_sample.xml` | `sample`, `clickListener` |
| `activity_sample.xml` | `sample`, `viewModel` |

`SampleListAdapter` は binding を使い回すため、
値を差し替えたあとに `executePendingBindings()` を呼ぶ。

## 変更するとき

画面を増やすときは `presentation` の下にパッケージを 1 つ足し、
Activity と ViewModel を対で置く。
`domain` と `data` に手を入れる必要が出たら、その画面の要求が
表示の都合ではなくアプリの語彙の変化であることを先に確認する。
