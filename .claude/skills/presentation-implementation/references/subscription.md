# 購読と Scheduler

UseCase が返す cold な `Single` を購読し、破棄まで面倒を見る。
ViewModel 一般の規則（状態の公開、命名、Hilt による注入）は `viewmodel.md` を見る。
ここは購読の扱いだけを扱う。

## 確認する対象

- `presentation/viewmodel/ForecastViewModel` の `load` / `disposeCurrent` / `onCleared`
- `GetForecastUseCase` が返すストリーム（cold、Scheduler 未指定）

## 実装時の注意

- 購読は ViewModel が 1 つだけ保持する。`Disposable` をフィールドに持ち、破棄したら `null` に戻す。
- 表示のためのスレッド移送は購読の直前に付ける。Scheduler は直接書かず、
  注入された `UiSchedulers` から取る。

```java
disposable = getForecastUseCase.invoke(Location.TOKYO)
        .subscribeOn(schedulers.background())
        .observeOn(schedulers.main())
        .subscribe(this::publishSuccess, this::publishError);
```

- `AndroidSchedulers.mainThread()` を書いてよいのは `UiSchedulers.android()` だけ。
  domain、data、di には書かない。
- 取得はコンストラクタと再試行の `load()` で始める。Activity からは始めない。
- 再読み込みは「既存を破棄 → `Loading` にする → 購読」の順で行う。
  破棄より先に `Loading` にすると、古い購読の結果が新しい `Loading` を上書きしうる。
- 成功・失敗はそれぞれ `Success` / `Failure` へ 1 回の `state.set` で移す。
  相が 1 つの値なので、読み込み中の表示を消し忘れる経路はない。
- `onCleared()` で必ず破棄する。通信中に画面を離れると購読が残る。
- 画面回転で ViewModel は生存するため、購読と状態はそのまま引き継がれる。
  Activity 側で購読し直さない。二重購読になる。

## テスト判断

- 状態遷移と購読の破棄は `ForecastViewModelTest` で押さえる。trampoline の組を渡す。
- `UiSchedulers.android()` を通る経路はエミュレータでの確認に回す。

## 検証観点

- 成功・失敗のどちらの経路でも `Loading` から抜けているか。
- `Disposable` が破棄後に `null` へ戻っているか。
- `Schedulers.io()` / `AndroidSchedulers.mainThread()` が `UiSchedulers.android()` 以外に現れていないか。
