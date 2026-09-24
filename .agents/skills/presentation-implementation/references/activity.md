# Activity ホストと Fragment の View

Activity はランチャー兼ホストに保つ。Fragment が画面の View を担う。

## 確認する対象

- `presentation/activity/ForecastActivity` と `presentation/fragment/ForecastFragment`
- `activity_forecast.xml` と `fragment_forecast.xml`
- `AndroidManifest.xml` の Activity 宣言

## 実装時の注意

View がしてよいのは次だけ。

- Activity は `DataBindingUtil.setContentView` で固定 ID の `FragmentContainerView` を持つホストを設定する。`android:name` で Fragment を指定し、手動の add、back stack、Navigation は使わない
- Fragment は `onCreate` で `new ViewModelProvider(this)` から ViewModel を取得し、`onCreateView` で画面の binding を作る
- Fragment は `onViewCreated` で binding に ViewModel を渡し、View ごとの Adapter を作る
- `RecyclerView` の `LayoutManager` と `Adapter` を設定する
- `state` の変更を `OnPropertyChangedCallback` で受け、`render(ForecastUiState)` へ渡す

守ること。

- 状態による分岐は `render` の 1 か所にだけ書く。相ごとの `visibility` と、
  `Success` の一覧を `Adapter.submit(...)` へ渡す処理がここに集まる。
- Java 17 の `switch` は sealed 型の網羅を検査できない。相を足したら `render` を必ず見直す。
- 整形と購読を書かない。それらは ViewModel と `ForecastFormatter` の仕事。
- `findViewById` と `setOnClickListener` を使わない。ビューは binding のフィールドで参照し、
  ボタン押下はレイアウト側で ViewModel のメソッドへ直接束ねる。
- **登録直後に現在の状態を自分で `render` する。** コールバックは値が変わったときにしか発火しない。
  回転や Fragment の View 再生成時、ViewModel は値を保持済みで変更が起きないため、
  省くと何も描画されない。
- コールバックは `onDestroyView()` で解除する。RecyclerView の Adapter を外し、
  binding を unbind して binding、Adapter、コールバックへの参照を消す。
  ViewModel と取得の購読はここで破棄しない。
- 取得を始めない。取得は ViewModel の生成時に始まる。`savedInstanceState` や
  ViewModel の状態を見て `load()` を呼ぶ分岐を書かない。

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

## テスト判断

- Activity と Fragment は JVM Unit Test で検証しない。ActivityScenario と Espresso で確認する。

## 検証観点

- `data` 層のクラスを import していないか。
- 状態の分岐が `render` の外へ散っていないか。
- `ForecastUiState` の全部の相が `render` で扱われているか。
- Activity と Fragment が `load()` を呼ぶ分岐を持っていないか。
- 回転後に何も描画されない経路がないか。登録直後の `render` とコールバック解除が揃っているか。
