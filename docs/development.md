# 開発手順

## 技術構成

| 項目 | 値 |
| --- | --- |
| 言語 | Java 17 |
| UI | View (XML) + ViewDataBinding |
| applicationId / namespace | `com.example.rxjava` |
| AGP / Gradle | 9.0.1 / 9.1.0 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 24 |
| Rx | RxJava 3.1.12, RxAndroid 3.0.2 |
| テーマ | Material 3 (`Theme.Material3.DayNight`) |

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

DataBinding の生成コードが絡む失敗は、原因のレイアウトが
`app/build/reports/data_binding/` 以下のログに出る。
Java 側のエラーだけを見ても分からないことが多い。

## エミュレータでの実行

```bash
android emulator list
```

```bash
android emulator start <名前>
```

```bash
android run
```

画面の確認は `android screenshot`、UI 階層の確認は `android layout -p` を使う。
スクリーンショットを撮って目視するより、`android layout` のほうが速い。

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

未検証の前提が残っている。着手前に [status.md](status.md) を読む。
