# Activity（View）

レイアウトを束ね、ViewModel の状態を RecyclerView へ橋渡しするだけの薄い層に保つ。

## 確認する対象

- `presentation/activity/ForecastActivity`
- `activity_forecast.xml` の `<data>` ブロック
- `AndroidManifest.xml` の Activity 宣言

## 実装時の注意

View がしてよいのは次だけ。

- レイアウトを `DataBindingUtil.setContentView` で束ね、`viewModel` 変数を渡す
- `RecyclerView` の `LayoutManager` と `Adapter` を設定する
- ViewModel が公開する `ObservableField<List<DailyForecast>>` の変更を `Adapter.submit(...)` へ橋渡しする
- 画面の初回起動を検知して `viewModel.load()` を呼ぶ

守ること。

- 条件分岐・整形・購読を書かない。それらは ViewModel の仕事。
- `findViewById` と `setOnClickListener` を使わない。ボタン押下はレイアウト側で ViewModel のメソッドへ直接束ねる。
- `ObservableField<List<DailyForecast>>` は DataBinding が `RecyclerView` の内容へ直接反映できないため、
  `Observable.OnPropertyChangedCallback` で変更を受け取り、取れた値を `Adapter.submit(...)` へ渡す。
  この橋渡しは値の受け渡しであり、表示の条件分岐ではない。
- **登録直後に現在値を自分で流す。** コールバックは値が変わったときにしか発火しない。
  回転で Activity だけが作り直されたとき、ViewModel は値を保持済みで変更が起きないため、
  流し込みを省くと一覧が空のままになる。
- コールバックは `onDestroy()` で解除する。ViewModel のほうが長生きするので、
  解除しないと古い Adapter への参照が回転のたびに積み上がる。

```java
viewModel.forecasts.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
    @Override
    public void onPropertyChanged(Observable sender, int propertyId) {
        List<DailyForecast> result = viewModel.forecasts.get();
        if (result != null) {
            adapter.submit(result);
        }
    }
});
```

- 通信の起動は `viewModel.isUntouched()` が `true` のときだけ `viewModel.load()` を呼ぶ形で防ぐ。
  画面回転では既存の ViewModel が状態を保持するため再取得せず、プロセス death からの復帰では
  新しい ViewModel が未着手なので取得する。`savedInstanceState == null` で判定してはいけない。
  プロセス復帰では `Bundle` が非 `null` でも ViewModel は新品になり、取得しないと空画面で固定される。

## テスト判断

- Activity は JVM Unit Test で検証しない。エミュレータでの確認に回す。

## 検証観点

- `data` 層のクラスを import していないか。
- ロジックが Activity へ染み出していないか（橋渡しと `viewModel.isUntouched()` による
  初回起動判定以外の `if` が出てきたら疑う）。
- 画面回転のたびに `viewModel.load()` が呼ばれ、通信が重複していないか。
- プロセス death からの復帰で新しい ViewModel が取得を開始するか。
- 回転後に一覧が空にならないか。登録直後の流し込みとコールバック解除が揃っているか。
