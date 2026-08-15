# 購読と Scheduler

UseCase が返す cold ストリームを購読し、破棄まで面倒を見る。
ViewModel 一般の規則（状態の公開、命名、`ServiceLocator` の使い方）は `viewmodel.md` を見る。
ここは購読の扱いだけを扱う。

## 確認する対象

- `presentation/run/SampleRunViewModel` の `start` / `stop` / `onCleared`
- `RunSampleUseCase` が返すストリーム（cold、Scheduler 未指定）

## 実装時の注意

- 購読は ViewModel が 1 つだけ保持する。`Disposable` をフィールドに持ち、
  破棄したら `null` に戻す。
- 表示のためのスレッド移送は購読の直前に付ける。

```java
runSample.invoke(sample.getId())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::appendLine, this::appendError, this::markFinished);
```

- `AndroidSchedulers.mainThread()` を書いてよいのはここだけ。
  domain と data には書かない。
- 実行状態を落とす経路を 3 つとも塞ぐ。停止ボタン、正常終了、エラー終了。
  どれか 1 つ忘れると、終わったのに停止ボタンが有効なまま残る。
- 再実行は「既存を破棄 → ログを空にする → 購読」の順で行う。
  順序を変えると前回の残りが混ざる。
- `onCleared()` で必ず破棄する。無限ストリームを回したまま画面を離れると購読が残る。
- 画面回転で ViewModel は生存するため、購読とログはそのまま引き継がれる。
  Activity 側で購読し直さない。二重購読になる。

## テスト判断

- `AndroidSchedulers.mainThread()` を含む経路は JVM Unit Test では動かない。
  エミュレータでの確認に回す。
- 無限ストリームは停止ボタンで実際に止まること、画面を戻って購読が残らないことを実機で確認する。

## 検証観点

- 実行状態を落とす経路が 3 つとも塞がれているか。
- `Disposable` が破棄後に `null` へ戻っているか。
- `observeOn(AndroidSchedulers.mainThread())` が presentation 以外に現れていないか。
