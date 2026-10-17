# 東京の天気 — RxJava Android サンプル

日本語 | [English](README.en.md)

7Timer! の公開 API から東京の 7 日予報を取得して表示する、学習・参照用の Android アプリです。
RxJava 3 による非同期処理を Clean Architecture と MVVM にどう配置するかを、
実装と設計文書、コミット履歴を通して残すことを目的としています。
アプリの配布やサービスとしての運用は対象にしていません。

## できること

- 東京の予報を 1 画面の一覧で表示します。各行は日付、天気、最低・最高気温、風速を持ちます。
- 読み込み中・成功・失敗を別の画面状態として扱い、失敗時には再試行できます。
- 取得結果を Room に保存し、取得から 24 時間未満なら保存済みの予報を使います。
  初回表示でも、キャッシュが有効なら通信は行いません。
- ViewModel で購読を管理し、画面回転や Fragment の View 再生成に対応します。

## 構成

Java 17、View（XML）と ViewDataBinding を使う単一の `app` モジュールです。
非同期処理は RxJava 3 / RxAndroid、通信は Retrofit / Moshi、永続化は Room、DI は Hilt を使います。

依存方向は `presentation -> domain <- data` とし、`presentation` の内部は MVVM です。

| 配置 | 役割 |
| --- | --- |
| `domain/` | Android に依存しないモデル、値オブジェクト、UseCase、Repository インタフェース |
| `data/` | API 通信、DTO の変換、Room による保存、24 時間キャッシュの判定 |
| `presentation/` | Activity、Fragment、ViewModel、Adapter と DataBinding による表示 |
| `di/` | Hilt による依存の組み立てと実装の選択 |

コードは `app/src/main/java/com/example/rxjava/` にあります。
依存のバージョンは [バージョンカタログ](gradle/libs.versions.toml) に集約しています。

## 必要な環境

| 項目 | 設定 |
| --- | --- |
| Android Studio | このプロジェクトの Android Gradle Plugin 9.0.1 に対応するもの |
| Java | ソース・ターゲットは Java 17。Gradle JDK には Android Studio 同梱の対応する JBR などを使用 |
| Android SDK | SDK Platform 36（`compileSdk` / `targetSdk` は 36） |
| 実行端末 | Android 7.0（API 24）以降のエミュレータまたは実機 |
| Gradle | 同梱の Wrapper を使用（9.1.0）。別途インストールは不要 |

依存の初回取得と、キャッシュがない・期限切れのときの天気取得にはインターネット接続が必要です。
API キーやアカウントの設定はありません。

## 動かす

1. このリポジトリのルートを Android Studio で開きます。
2. SDK Platform 36 と Gradle JDK を設定し、Gradle Sync を行います。
   SDK の場所は Android Studio またはローカルの `local.properties` の `sdk.dir` で設定します。
   `local.properties` は Git 管理しません。
3. エミュレータを起動するか、デバッグを有効にした実機を接続します。
4. `app` の実行構成を選び、Android Studio の Run から起動します。

コマンドラインで debug APK を作る場合は、リポジトリのルートで Wrapper を使います。

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

macOS / Linux:

```bash
./gradlew assembleDebug
```

出力先は `app/build/outputs/apk/debug/app-debug.apk` です。
この README の整備時には掲載コマンドを実行せず、コードと既存文書との照合だけを行っています。

## 検証用コマンド

以下は利用者が実行できるチェックです。Windows PowerShell では `./gradlew` を `.\gradlew.bat` に読み替えてください。

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
```

JVM テストには ArchUnit による依存方向の検査を含みます。
端末テストにはエミュレータまたは実機が必要で、画面の状態と実際の Room DB を検証します。
テスト方針と既存の検証記録は、下記の詳細文書を参照してください。

## API と既知の制約

東京の固定座標（緯度 35.690、経度 139.692）で 7Timer! の `civillight` を呼び出します。
認証情報は送信しません。仕様・利用条件は [7Timer! 公式 Wiki](https://github.com/Yeqzids/7timer-issues/wiki/Wiki) を確認してください。

- 地点選択はなく、画面の表示言語は日本語です。
- 取得判定は初回表示と失敗後の再試行時だけです。成功後の強制更新、画面復帰時やバックグラウンドでの自動更新はありません。
- Room のキャッシュはプロセス終了後も残るため、アプリを強制停止して開き直しても、取得から 24 時間未満なら同じ予報を表示します。
  期限切れ後の通信失敗時に、古い予報へフォールバックする実装はありません。
- 外部 API の可用性に依存します。
- release 配布と Room の migration は対象外です。開発中にスキーマを変更した場合は、アプリのデータを消してから起動する運用です。

## 詳細文書

詳細文書は日本語です。

| 文書 | 内容 |
| --- | --- |
| [アーキテクチャ](docs/architecture.md) | 層、依存方向、非同期処理とキャッシュの設計 |
| [Domain](docs/domain.md) | モデル、値オブジェクト、UseCase、Repository 契約 |
| [Data](docs/data.md) | 通信、DTO、DataSource、Room、Repository 実装 |
| [Presentation](docs/presentation.md) | ViewModel、画面状態、DataBinding、購読管理 |
| [開発手順](docs/development.md) | 環境、ビルド、外部 API、既存の検証記録、制約 |
| [JVM テスト](docs/unit-test.md) | 単体テストと依存方向の検査 |
| [UI・DB テスト](docs/ui-test.md) | 端末上の検証方針と手順 |
