# 東京の天気

7Timer! の API から東京の 7 日予報を取得して表示する Android アプリ。

画面は 1 枚。起動すると通信し、読み込み中・成功・失敗をそれぞれ別の表示で出す。
失敗したときは再試行ボタンから取り直せる。

小さなアプリだが、非同期処理が持つ問題は一通り出る。
待っている状態、失敗した状態、再試行、画面回転をまたぐ購読の扱いである。
これを RxJava と Clean Architecture でどう配置するかがこのリポジトリの主題になる。

## 動かす

```bash
./gradlew assembleDebug
```

```bash
android emulator start Pixel_10
```

```bash
android run --apks app/build/outputs/apk/debug/app-debug.apk
```

## 構成

Java 17 + View (XML) + ViewDataBinding。Clean Architecture の 3 層で、
`presentation` の内部は MVVM。

| 層 | 役割 |
| --- | --- |
| `domain` | モデル、値オブジェクト、UseCase、Repository インタフェース。Android 非依存 |
| `data` | 7Timer! との通信と Repository 実装。Retrofit と Moshi はここに閉じる |
| `presentation` | Activity、ViewModel、Adapter |
| `di` | Hilt の Module。依存関係の組み立てとインタフェース・実装の対応付け |

## 使っている API

```
https://www.7timer.info/bin/api.pl?product=civillight&output=json&lat=35.690&lon=139.692
```

認証もキーもない。現行の公式 Wiki では、無料・有料のソフトウェアで利用でき、
利用許可の取得は不要と案内されている。利用時は最新の記載を確認する。
仕様と利用条件は https://github.com/Yeqzids/7timer-issues/wiki/Wiki にある。

設計と開発手順は [docs/](docs/) にある。
