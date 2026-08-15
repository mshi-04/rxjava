# domain 層

アプリの語彙を定義する最内層。何が「サンプル」で、何が「ログ 1 行」かをここで決める。

## 依存

| import してよい | してはならない |
| --- | --- |
| `io.reactivex.rxjava3.core`、標準 Java | Android SDK、RxAndroid、`data`、`presentation` |

RxJava core だけは例外的に許す。
このアプリの題材が RxJava そのものであり、
「ログの列が時間をかけて流れてくる」という概念が語彙の一部だからである。
ここを自前の抽象で包むと、RxJava を包み直すインタフェースが増え、観察対象が層の下に埋まる。

RxAndroid は入れない。Scheduler の指定は domain の関心事ではない。

## クラス

| クラス | 役割 |
| --- | --- |
| `model/SampleId` | サンプルの識別子。値オブジェクト |
| `model/Sample` | タイトルと説明を持つエンティティ |
| `model/SampleLogLine` | スレッド名とメッセージを持つログ 1 行 |
| `repository/SampleRepository` | サンプルの取得と実行の契約 |
| `usecase/GetSampleListUseCase` | 一覧を返す |
| `usecase/GetSampleUseCase` | 1 件を返す |
| `usecase/RunSampleUseCase` | 実行のログストリームを返す |

## SampleId

`String` を包んだ値オブジェクト。`equals` と `hashCode` を持ち、`Map` のキーになる。

画面遷移で `Intent` に載る識別子でもあるため、一度公開した値は変えない。
一覧の並び順や添字に依存しない文字列を使う。

## Sample

タイトルと説明だけを持ち、**実行手段を持たない**。

サンプルの中身は `data` 層の関心事であり、
`presentation` が一覧を描くのに必要なのは表示用の文字列と識別子だけである。
`Sample` に実行関数を持たせると、一覧を組み立てるだけで
全サンプルの実装が `presentation` から見えてしまう。

## SampleLogLine

生成時のスレッド名を自分で確定させる。
これが放出と同じスレッドで起きるため、スレッド名は自然に正しくなる。

コールバックで受けてから別スレッドへ渡す設計だと、
どこで名前を確定させるかを毎回意識する必要があり、間違えると全行が `main` になる。
このアプリでは `subscribeOn` / `observeOn` の効果がスレッド名にしか現れないので、
そこが崩れると目的そのものが失われる。

表示用の整形はしない。`[スレッド名] メッセージ` の組み立ては `presentation` の仕事。

## SampleRepository

インタフェースを **domain が所有** し、`data` が実装する。
これで依存が内向きに反転し、`domain` はサンプルの出所を知らずに済む。

`run(SampleId)` が返すのは cold な `Observable<SampleLogLine>` で、
購読するまで何も起きない。Scheduler の指定もしない。

## UseCase

1 つの操作につき 1 クラス、公開メソッドは `invoke` のみ。

現状は 3 つとも Repository への委譲 1 行しか持たない。
それでも置くのは、`presentation` が Repository のインタフェースへ直接依存すると、
サンプルの絞り込みや並べ替えといった規則の置き場が `presentation` へ流れるためである。

## 変更するとき

エンティティを増やす前に、それが本当にアプリの語彙かを確認する。
表示のための値（整形済み文字列、色、アイコン）は `presentation` に置く。
サンプルの実装都合の値は `data` に置く。
