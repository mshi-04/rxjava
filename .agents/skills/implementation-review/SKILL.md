---
name: implementation-review
description: 実装差分をレビューする依頼で使う。東京の天気の依存方向、層の責務、ストリームの cold 性、購読の破棄、テスト不足を確認して指摘する。
---

# 目的

東京の天気の差分が既存方針に沿っているかを確認し、
コミット前に直すべき問題を具体的に洗い出す。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/development.md`](../../../docs/development.md) の「未確認のまま残っていること」
- 差分が触れている層の docs（[`domain.md`](../../../docs/domain.md) / [`data.md`](../../../docs/data.md) / [`presentation.md`](../../../docs/presentation.md)）
- 差分と関連コード

# 手順

1. 比較基準を確定する（作業ブランチの差分か、直近コミットからの差分か）。
2. 差分の目的、変更範囲、既存実装との関係を確認する。
3. 下の観点を順に見る。
4. 指摘を分類し、根拠となるファイルと行を添えて報告する。
5. 問題がない場合も、残るリスクと未検証項目を明記する。

# 観点

## 依存方向

- `presentation -> domain <- data` が崩れていないか。
- `presentation` が `data` のクラスを参照していないか。
- `domain` に Android SDK、RxAndroid、DataBinding、Retrofit、Moshi が入っていないか。
- `data` に Android SDK、RxAndroid が入っていないか。
- Retrofit と Moshi の型（`ForecastResponse` など）が `data/remote/` の外へ漏れていないか。
- 実体の組み立てが `di/` の Module の外へ漏れていないか。
- 自作クラスがコンストラクタ注入になっているか。`@Provides` で `new` を書いていないか。
- インタフェースと実装の対応が `@Binds` で結ばれているか。
- `domain` と `data` に `@Module` や `@Provides` のような Hilt 固有の型が入っていないか。

## ストリームの契約

- `ForecastRepository` と `GetForecastUseCase` が返す `Single` が cold か。呼ばれただけで通信が動き出していないか。
- `subscribeOn` / `observeOn` が presentation（`ForecastViewModel`）以外に現れていないか。
- `AndroidSchedulers.mainThread()` が presentation の外に現れていないか。
- 通信の失敗が `onError` としてそのまま上がっているか。`onErrorReturn` や `try-catch` で
  空リストや `null`、既定値へ握り潰していないか。

## API の語彙とガード節

- 7Timer! の JSON の項目名や天気コードの文字列が `ForecastMapper` の外へ漏れていないか。
- 未知の天気コードが例外にならず `WeatherCondition.UNKNOWN` に落ちているか。
- `dataseries` のうち写せない 1 日分だけが除外され、利用できる日は返されるか。
- `dataseries` 自体の欠落、または要素があるのに 1 件も写せない状態が例外として上がるか。
  空リストや `null` に潰して形の異常を隠していないか。
- 全件を写せない例外の cause に最初の日別変換例外が残り、欠けた項目などを診断できるか。
- 気温・風速・日付・緯度経度が `Temperature` / `WindSpeed` / `ForecastDate` / `Location` の
  値オブジェクトを経由せず、生の `int` / `double` のまま層をまたいでいないか。

## 購読の寿命

- 購読が ViewModel の 1 つに集約されているか。
- `onCleared()` で破棄されているか。
- Activity 側で購読し直していないか（画面回転で二重になる）。
- 再読み込みの前に、既存の購読の破棄と状態のリセットが行われているか。

## 責務と状態

- Activity が UseCase を直接呼んでいないか。domain へのアクセスは ViewModel を通す。
- `@AndroidEntryPoint` と `@HiltViewModel` が対で付いているか。
- Activity の状態による分岐が `render` の 1 か所に集まっているか。整形と購読が染み出していないか。
- `ForecastUiState` の全部の相を `render` が扱っているか。Java 17 ではコンパイラが網羅を検査しない。
- 初回取得を Activity が判断していないか。取得は ViewModel のコンストラクタで始める。
- ViewModel に `View` / `Context` が入っていないか。エラー文言を `Context` なしで組み立てようとしていないか。
- 画面の状態が `ForecastUiState` の 1 つの値で公開され、読み込み中・成功・失敗が別々の record になっているか。
  `ForecastUiState` に相の判定メソッドが生えていないか。
  真偽値のフィールドを並べて矛盾した組み合わせを作れる形に戻っていないか。
- ViewModel が Scheduler を直接書かず、注入された `UiSchedulers` から取っているか。
- 表示用の整形（`ForecastFormatter` が担う文言選択、`strings.xml` の書式）が domain の model へ移っていないか。

## 過剰

- 委譲だけの UseCase を新たに増やしていないか。
- 既存 contract で足りるのに新しい公開 API を足していないか。
- 呼び出し側のない public メソッドや overload が残っていないか。

## テストと検証

- 変更した振る舞いのうち、JVM で検証できるもの（`domain` の value object、`ForecastMapper`、
  `ForecastUiState`、`ForecastViewModel` の状態遷移）にテストがあるか。
- 実ネットワーク呼び出しや `AndroidSchedulers.mainThread()` を伴う経路が、
  JVM Unit Test の対象として誤って扱われていないか。
- エミュレータでしか確認できない項目が `docs/development.md` の「未確認のまま残っていること」に
  書かれているか。差分がそこへ追記していなければ指摘する。
- 実行した task と結果が報告されているか。

# 指摘の形式

指摘 1 件につき、次を書く。

- 分類（`bug` / `依存方向` / `責務` / `過剰` / `テスト不足` / `文書`）
- 対象のファイルと行
- 何が問題か（現象、または崩れている前提）
- どう直すか

# 注意事項

- 好みではなく、バグ、依存方向、責務分離、保守性、テスト不足を優先する。
- テストコードだけの追加・修正は `test-implementation` を優先する。
- 設計そのものを変える必要があると判断したときは、コードではなく
  `docs/architecture.md` と該当層の docs を先に直すよう指摘する。設計が正で、コードがそれに従う。
