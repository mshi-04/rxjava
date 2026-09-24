# ユニットテスト

JVM 上で動く `app/src/test/` のテストの正本。
何をテストするか、どう書くかをここで決める。

画面の振る舞いを見る instrumented test は [ui-test.md](ui-test.md) を参照。

## 構成

| 項目 | 値 |
| --- | --- |
| フレームワーク | JUnit Jupiter 6.1.3（JUnit Platform） |
| 実行 | `./gradlew testDebugUnitTest` |
| モックライブラリ | MockWebServer 4.12.0 は data 層のローカル HTTP 結合テストだけで使う。その他の fake は手書きする |

Gradle は `tasks.withType<Test>().configureEach { useJUnitPlatform() }` で
Platform を起動する。`org.junit.platform:junit-platform-launcher` が
`testRuntimeOnly` に要る。

`testOptions.unitTests.isReturnDefaultValues = true` にしている。
JVM のテストが載せる `android.jar` は中身のないスタブで、既定では呼ぶと例外を投げる。
`ForecastViewModel` は失敗時に `Log.w` を呼ぶため、これがないと失敗経路のテストが
ログ出力で落ちる。Android の API の戻り値に依存する検証はここでは書かない。

JUnit 4 は使わない。`org.junit.Test` と `org.junit.Assert` は残っていない。

## JVM で検証できる範囲

| 対象 | 判断 |
| --- | --- |
| `domain` の値オブジェクトと `DailyForecast` | できる |
| `data` の `ForecastMapper` | できる |
| `data` の `RemoteForecastRepository`（ローカル HTTP サーバーを伴う） | できる。MockWebServer と実際の Retrofit・Moshi・RxJava アダプターを組み合わせる |
| `presentation` の `ForecastUiState` | できる |
| `presentation` の `ForecastViewModel` の状態遷移と購読の破棄 | できる。同期的な遷移には trampoline、スレッド移送と保留中の通知には独立した `TestScheduler` の組を渡す |
| `ForecastActivity.render` | できない。UI テストで見る |
| `UiSchedulers.android()`（`AndroidSchedulers.mainThread()`） | できない。UI テストで見る |
| Activity、DataBinding の式、Adapter | できない。UI テストで見る |

対象外のものは [ui-test.md](ui-test.md) へ回す。
どちらでも検証できないものは [development.md](development.md) の「未確認のまま残っていること」へ書き足す。

## AAA で書く

テストは Arrange / Act / Assert の 3 段に分け、**それぞれにコメントを置く**。

```java
@Test
void ofCelsius_aboveUpperBound_throwsIllegalArgument() {
    // Arrange
    int tooHot = 101;

    // Act
    Executable creation = () -> Temperature.ofCelsius(tooHot);

    // Assert
    assertThrows(IllegalArgumentException.class, creation);
}
```

コメントの見出しは `Arrange` / `Act` / `Assert` と英語で書く。
本文は日本語だが、この 3 語は型の名前と同じ扱いで、
どの段かを一目で判別できることを優先する。

段が省ける場合でもコメントは省かない。
Arrange が不要なテストは `// Arrange` の行ごと落とし、
残る 2 つにはコメントを置く。

テストコードは、字面から読み取れる説明であってもコメントを書いてよい唯一の場所である。
本番コードは字面から読み取れる説明を書かない。
ただし警告抑制の理由のように、ツールが要求するものは本番コードにも書く。
判断の正本は `~/.claude/rules/comments.md`。

## 命名

テストクラスとテストメソッドは package-private にする。`public` は付けない。
クラス名は `対象クラス名 + Test`。

メソッド名は `対象メソッド_条件_期待結果` の 3 部構成とし、`_` で区切る。

```java
void ofCelsius_aboveUpperBound_throwsIllegalArgument()
void toDailyForecasts_unknownWeatherCode_fallsBackToUnknown()
void toDailyForecasts_missingDataseries_throwsIllegalState()
```

区切りが要るのは、3 要素の境目が目で追えることが読みやすさの本体だからである。
区切りなく連結した `ofCelsiusRejectsATemperatureAboveTheUpperBound` のような名前は、
同じ情報量でもどこが条件でどこが期待値か掴めない。
この形は使わない。

第 1 部を対象メソッド名に固定すると、一覧が対象ごとに揃って並ぶ。
`TOKYO` のような定数を対象にするときは定数名を第 1 部に置く。
条件が自明なテストは第 2 部を省いて 2 部構成にしてよい。

流儀は他に `should_期待_when_条件` と `given_前提_when_操作_then_期待` があり、
どれを選ぶかより一貫して使うことのほうが重要とされる。
このリポジトリは対象メソッドが先頭に来る形を採る。

`@DisplayName` は使わない。JUnit 5 の読みやすさ向上策として広く勧められているが、
名前が 2 か所に分かれ、片方だけ直す事故が起きる。
名前が長くなりすぎたら `@Nested` で条件を外へ括り出す。

## assertAll

同じ対象に対する複数の検証は `assertAll` でまとめる。
最初の失敗で打ち切られず、何がいくつ壊れているかが 1 回の実行で分かる。

要素ごとに同じ検証を繰り返すときは `assertAll(stream.map(...))` を使い、
for ループの中で assert しない。

独立した別々の事実を検証したくなったら、それはテストを分ける合図である。

## ストリームの検証

`.test()`（`TestObserver`）を使う。
終了を待つときは `awaitDone(5, TimeUnit.SECONDS)` のように必ず上限を付ける。

cold であることを確かめるテストは、購読前後でカウンタが増えないこと・
増えることを見る。

`RemoteForecastRepository` の結合テストは毎回 MockWebServer を起動・終了し、
localhost だけに接続する。Retrofit は本番と同じ `ForecastApi`、Moshi converter、
`RxJava3CallAdapterFactory.create()` で組み立てる。通信には OkHttp の時間制限を設定し、
リクエストの取得と `TestObserver` の終了待ちにも上限を付ける。
DTO を直接作る Mapper テストだけでは、JSON の注釈や Retrofit の URL・Query の配線を
検証できないため、HTTP の入口から Repository の戻り値までを通す。
URL・固定パラメータ・東京の緯度経度、JSON から domain への写像、HTTP と JSON の失敗、
欠落・不正・空の `dataseries`、購読前と再購読時の通信回数を確認する。

## ViewModel の検証

`ForecastViewModel` は本番と同じコンストラクタで組み立てる。
`GetForecastUseCase` には手書きの fake の `ForecastRepository` を、
`UiSchedulers` には `Schedulers.trampoline()` の組を渡す。

コンストラクタが取得を始めるため、fake の応答は ViewModel を作る前に仕込む。

応答の完了を操作したいときは fake が `SingleSubject` を返す。
`onSuccess` / `onError` を呼ぶまで `Loading` に留まり、`hasObservers()` で購読の破棄を確かめられる。

見るのは `state` の型と、`Success` が持つ一覧である。
`onCleared()` は protected だが、テストは同じ package に置くため呼べる。

スケジューラの境界は background と main に別々の `TestScheduler` を渡して確かめる。
`triggerActions()` で購読と通知を個別に進め、通知が main に待機している間の
再読み込み・破棄でも古い結果が状態を上書きしないことを見る。

## 書かないもの

- 時間に依存するテスト。`Thread.sleep` で待つテストは環境で落ちる
- 外部 API を叩くテスト。ローカル HTTP サーバーへの通信は許可する
- テストのために本番コードの可視性を広げること。
  テストしたいロジックは、Android やシングルトンから切り離した位置へ寄せる

## 依存を足したくなったら

追加せずに、理由とともに報告する。
モックライブラリを入れる前に、手書きの fake で足りないかを確認する。
