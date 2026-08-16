# 購読と Scheduler

UseCase が返す cold な `Single` を購読し、破棄まで面倒を見る。
ViewModel 一般の規則（状態の公開、命名、Hilt による注入）は `viewmodel.md` を見る。
ここは購読の扱いだけを扱う。

## 確認する対象

- `presentation/viewmodel/ForecastViewModel` の `load` / `disposeCurrent` / `onCleared`
- `GetForecastUseCase` が返すストリーム（cold、Scheduler 未指定）

## 実装時の注意

- 購読は ViewModel が 1 つだけ保持する。`Disposable` をフィールドに持ち、破棄したら `null` に戻す。
- 表示のためのスレッド移送は購読の直前に付ける。取得は `Schedulers.io()`、結果の反映は
  `AndroidSchedulers.mainThread()`。

```java
disposable = getForecast.invoke(Location.TOKYO)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::handleSuccess, this::handleError);
```

- `AndroidSchedulers.mainThread()` を書いてよいのはここだけ。domain と data には書かない。
- 再読み込みは「既存を破棄 → エラー状態を戻す → 読み込み中にする → 購読」の順で行う。
  順序を変えると、前回の失敗表示が一瞬残ったり、読み込み中の表示が抜けたりする。
- 成功時は `loading` を戻してから `forecasts` を設定する。失敗時は `loading` を戻してから
  `hasError` を立てる。読み込み中のフラグを消し忘れると、成功・失敗どちらでも
  進捗表示が残ったままになる。
- `onCleared()` で必ず破棄する。通信中に画面を離れると購読が残る。
- 画面回転で ViewModel は生存するため、購読と状態はそのまま引き継がれる。
  Activity 側で購読し直さない。二重購読になる。

## テスト判断

- `AndroidSchedulers.mainThread()` を含む経路は JVM Unit Test では動かない。エミュレータでの確認に回す。
- 通信が失敗したときに `loading` が戻り `hasError` が立つことは実機で確認する。

## 検証観点

- 成功・失敗のどちらの経路でも `loading` が戻っているか。
- `Disposable` が破棄後に `null` へ戻っているか。
- `observeOn(AndroidSchedulers.mainThread())` が presentation 以外に現れていないか。
