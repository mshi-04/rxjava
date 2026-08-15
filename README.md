# RxJava Samples

RxJava 3 の挙動を画面上のログで観察する学習用 Android アプリ。

サンプルを一覧から選んで実行すると、各イベントがスレッド名つきの時系列ログとして流れる。
オペレータの結果だけでなく、「どのスレッドで」「どの順に」「いつ終わるか」を見ることが目的。
無限に流れるサンプルは停止ボタンで購読を破棄できる。

## 収録しているサンプル

| タイトル | 観察できること |
| --- | --- |
| Observable の基本 | just / filter / map をつないだときの onNext・onComplete |
| Scheduler の切り替え | subscribeOn だけの場合と observeOn を足した場合で、値が流れるスレッドがどう変わるか |
| interval と Disposable | 無限に流れ続けるストリームを dispose で止める |
| Flowable と backpressure | 生産が消費より速いときに onBackpressureDrop が値を捨てる様子 |
| BehaviorSubject の購読タイミング | 購読開始が遅れた側に直前の値が届くこと |
| エラーハンドリングと retry | 2 回失敗してから成功する処理を retry で拾い、onErrorReturnItem で保険をかける |
| zip による合成 | 有限のリストと interval を zip して、遅い側のペースで揃えて流す |

## 動かす

```bash
./gradlew assembleDebug
```

```bash
android emulator start Pixel_10
```

```bash
android run
```

## 技術構成

Java 17 + View (XML) + ViewDataBinding。RxJava 3.1.12 / RxAndroid 3.0.2。
minSdk 24、compileSdk / targetSdk 36。

Clean Architecture の 3 層で、`presentation` の内部は MVVM。
設計と開発手順は [docs/](docs/) にある。
