# 開発手順

## 技術構成

| 項目 | 値 |
| --- | --- |
| 言語 | Java 17 |
| UI | View (XML) + ViewDataBinding |
| 画面ホスト | AndroidX Fragment 1.9.1 |
| applicationId / namespace | `com.example.rxjava` |
| AGP / Gradle | 9.0.1 / 9.1.0 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 24 |
| Rx | RxJava 3.1.12, RxAndroid 3.0.2 |
| 通信 | Retrofit 3.0.0, Moshi 1.15.2, OkHttp（Retrofit 経由） |
| 永続化 | Room 2.8.5（annotationProcessor）、room-rxjava3 2.8.5。スキーマは Room Gradle プラグインで `app/schemas/` に出力し git 管理する |
| DI | Hilt 2.60.1（Java なので annotationProcessor。KSP は使わない） |
| JVM Unit Test | JUnit Jupiter 6.1.3（JUnit Platform）、ArchUnit 1.5.0（依存方向の検査） |
| UI テスト | Espresso 3.7.0 + Hilt testing。方針は [ui-test.md](ui-test.md) |
| テーマ | Material 3 (`Theme.Material3.DayNight.NoActionBar`) |

採用しないもの: Kotlin、Jetpack Compose、ViewBinding。
バインディングは DataBinding に一本化する。

## 前提環境

| 項目 | 値 |
| --- | --- |
| JDK | Android Studio 同梱の JBR (`JAVA_HOME` に設定済み) |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk` |
| 必要な SDK Platform | android-36 |
| 補助ツール | Android CLI (`android`) |

`local.properties` は SDK の位置を書くだけのファイルで、環境ごとに異なるため
git 管理しない。手元にない場合は次で作る。

```bash
android create empty-activity --name=Probe --output=./probe-tmp
```

生成された `probe-tmp/local.properties` を複製し、`probe-tmp` は捨ててよい。

## ビルドとテスト

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew lintDebug
```

```bash
./gradlew connectedDebugAndroidTest
```

instrumented test はエミュレータか実機の接続が要る。

DataBinding の生成コードが絡む失敗は、原因のレイアウトが
`app/build/reports/data_binding/` 以下のログに出る。
Java 側のエラーだけを見ても分からないことが多い。

## 表示の検証記録（2026-09-29）

`Pixel_10` エミュレータ、Android 17（API 37、AVD の system image は android-37.1）、
縦画面 1080×2424・密度 420 dpi で、既存の fake Repository を使って確認した。

| テーマ | 文字サイズ | `connectedDebugAndroidTest` | 画像での確認 |
| --- | --- | --- | --- |
| ライト | 100％ | 21 件成功、失敗・スキップ 0 件 | 成功・読み込み・失敗表示に欠けや重なりなし |
| ライト | 200％ | 21 件成功、失敗・スキップ 0 件 | 拡大した予報・エラー文言・再試行ボタンも表示される |
| ダーク | 100％ | 21 件成功、失敗・スキップ 0 件 | 文字・進捗表示・再試行ボタンを視認できる |
| ダーク | 200％ | 21 件成功、失敗・スキップ 0 件 | 拡大した予報・エラー文言・再試行ボタンも表示される |

各条件で成功（先頭・最終行・先頭へ戻った後）、読み込み中、失敗の 5 画像を確認した。
7 日分の全項目・入力順、スクロール後の再表示、未知の天気、空の成功結果、
再試行の操作は自動テストで確認した。ステータスバーはライトで暗いアイコン、
ダークで明るいアイコンになり、どちらも視認できた。
文字サイズ 200％で一覧の端に部分表示される行は、スクロールすると全項目を確認できる。

画像と各実行のテスト結果は `app/build/reports/ui-validation/<条件>/` に保存する。
再実行の設定・画像取得方法は [ui-test.md](ui-test.md) の「テーマと文字サイズの検証」にある。
検証後は、実行前の夜間モード `no` と文字サイズ `1.0` に戻した。

併せて `assembleDebug`、`testDebugUnitTest`（121 件、うち ArchUnit 16 件）、
`lintDebug`、`assembleDebugAndroidTest` が成功した。
Lint はエラー 0 件、既存の targetSdk・Gradle・AGP の更新案内の警告 3 件。

## 未確認のまま残っていること

検証できていない前提はここに集める。潰したら消す。

- 別 OS・実機上でのライト／ダークテーマと拡大文字の表示。
  上の検証記録は `Pixel_10` エミュレータ上の Android 17 に限る
- 成功後に予報を取り直す手段が無い。再試行ボタンは失敗時にしか出ず、
  取得の判定は初回表示と再試行のときだけ行う。画面復帰やバックグラウンドでは取り直さない。
  取得から 24 時間は保存済みの予報を返すため、force-stop しても更新されない。
  既知の制約であって不具合ではない

## release ビルドは対象外

このリポジトリは配布しない。`assembleRelease` は通していないし、通す予定もない。

`isMinifyEnabled = false` のままで、`proguard-rules.pro` は空である。
minify を有効にすると Moshi が `ForecastResponse` のフィールドを
リフレクションで読めなくなるため、`-keep` 規則が要る。
配布する必要が出たときに初めて書く。

同じ理由で、Room の migration は用意しない。
スキーマを変えるときは DB バージョン 1 のまま `app/schemas/` の JSON を上書きし、
開発端末ではアプリのデータを消してから起動する。

## ビルドの設定

`gradle.properties` で次を有効にしている。

| 設定 | 効果 |
| --- | --- |
| `org.gradle.caching` | ビルドキャッシュ |
| `org.gradle.configuration-cache` | 設定フェーズの結果を再利用する |

`org.gradle.parallel` は入れない。
これは独立した project のタスクを並行実行する設定で、
`:app` の単一モジュール構成では効果がない。
モジュールを分割するときに合わせて入れる。

ビルドの再現性を疑うときは、この 2 つを外して切り分ける。

```bash
./gradlew assembleDebug --no-build-cache --no-configuration-cache
```

## 外部 API

天気予報は 7Timer! の公開 API を使う。認証もキーもない。

```
https://www.7timer.info/bin/api.pl?product=civillight&output=json&lat=35.690&lon=139.692
```

現行の公式 Wiki では、無料・有料のソフトウェアで利用でき、利用許可の取得は不要と
案内されている。利用時は最新の記載を確認する。仕様と利用条件は
https://github.com/Yeqzids/7timer-issues/wiki/Wiki にある。

`INTERNET` 権限が要る。HTTPS のみを使い、cleartext は許可しない。
`res/xml/network_security_config.xml` の `base-config` で平文通信を拒否し、
Manifest の `android:networkSecurityConfig` から参照する。

## エミュレータでの実行

```bash
android emulator list
```

```bash
android emulator start <名前>
```

```bash
android run --apks app/build/outputs/apk/debug/app-debug.apk
```

`android run` は APK の指定が要る。引数なしでは `No apks specified` で失敗する。

```bash
android screen capture -o shot.png
```

```bash
android layout -p
```

画面の確認は `android screen capture`、UI 階層の確認は `android layout -p` を使う。
表示位置や重なりを調べるなら `android layout` のほうが速く、座標も取れる。

## 依存の更新

バージョンは `gradle/libs.versions.toml` だけを書き換える。
最新版の確認には Android CLI を使う。

```bash
android studio version-lookup agp gradle io.reactivex.rxjava3:rxjava io.reactivex.rxjava3:rxandroid
```

AGP と Gradle は組で上げる。

## Android の情報を調べるとき

推測で API を使わず、Android CLI の docs を引く。

```bash
android docs search "キーワード"
```

```bash
android docs fetch "kb://..."
```

## git 運用

- ブランチは `main`
- コミットメッセージは日本語、件名は「〜する」の形
- 改行コードは `.gitattributes` で LF に統一。`*.bat` のみ CRLF
- ビルド成果物・`local.properties`・署名鍵はコミットしない
