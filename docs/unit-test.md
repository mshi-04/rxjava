# ユニットテスト

JVM 上で動く `app/src/test/` のテストの正本。
何をテストするか、どう書くかをここで決める。

画面の振る舞いを見る instrumented test は [ui-test.md](ui-test.md) を参照。

## 構成

| 項目 | 値 |
| --- | --- |
| フレームワーク | JUnit Jupiter 6.1.3（JUnit Platform） |
| 実行 | `./gradlew testDebugUnitTest` |
| モックライブラリ | なし。fake は手書きする |

Gradle は `tasks.withType<Test>().configureEach { useJUnitPlatform() }` で
Platform を起動する。`org.junit.platform:junit-platform-launcher` が
`testRuntimeOnly` に要る。

JUnit 4 は使わない。`org.junit.Test` と `org.junit.Assert` は残っていない。

## JVM で検証できる範囲

| 対象 | 判断 |
| --- | --- |
| `domain` の値オブジェクトと `DailyForecast` | できる |
| `data` の `ForecastMapper` | できる |
| `data` の `RemoteForecastRepository`（実通信を伴う） | しない。エミュレータで確認する |
| `AndroidSchedulers.mainThread()` を通る経路 | できない。UI テストで見る |
| Activity、DataBinding、`ObservableField` | できない。UI テストで見る |

対象外のものは [ui-test.md](ui-test.md) へ回すか、
確認手段がないものは [status.md](status.md) の未検証に残す。

## AAA で書く

テストは Arrange / Act / Assert の 3 段に分け、**それぞれにコメントを置く**。

```java
@Test
void ofCelsiusRejectsATemperatureAboveTheUpperBound() {
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

このリポジトリで**コメントを書いてよいのはテストコードだけ**である。
本番コードは字面から読み取れる説明をコメントに書かない。

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

## 書かないもの

- 時間に依存するテスト。`Thread.sleep` で待つテストは環境で落ちる
- 実ネットワークを叩くテスト
- テストのために本番コードの可視性を広げること。
  テストしたいロジックは、Android やシングルトンから切り離した位置へ寄せる

## 依存を足したくなったら

追加せずに、理由とともに報告する。
モックライブラリを入れる前に、手書きの fake で足りないかを確認する。
