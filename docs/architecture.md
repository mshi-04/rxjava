# アーキテクチャ

Clean Architecture の 3 層。`presentation` の内部は MVVM。
各層の詳細は層ごとの文書にある。

| 層 | 文書 |
| --- | --- |
| `domain` | [domain.md](domain.md) |
| `data` | [data.md](data.md) |
| `presentation` | [presentation.md](presentation.md) |

## このアプリが解きたいこと

RxJava のオペレータや Scheduler の挙動は、戻り値を見るだけでは分からない。
「どのスレッドで」「どの順に」「いつ終わるか」が本体だからである。
そこでこのアプリは、サンプルの実行過程をスレッド名つきの時系列ログとして
画面に流すことだけに専念する。

この目的から、設計の軸は 2 つになる。

- サンプルを増やすコストを、関数 1 つと一覧への 1 行に抑える
- サンプル本体を Android の都合から切り離し、RxJava の記述だけが残るようにする

## 層と依存の向き

```
        presentation                          data
   (Activity / ViewModel / Adapter)   (Repository 実装 / サンプル定義)
                  │                             │
                  └──────────┐     ┌────────────┘
                             ▼     ▼
                           domain
              (Sample / SampleLogLine / UseCase
               / SampleRepository インタフェース)
```

依存は常に外側から内側へ向かう。`domain` は `presentation` も `data` も知らない。
`data` が `domain` の `SampleRepository` を実装することで、依存が内向きに反転する。

| 層 | import してよい | してはならない |
| --- | --- | --- |
| `domain` | `io.reactivex.rxjava3.core`、標準 Java | Android SDK、RxAndroid、`data`、`presentation` |
| `data` | `domain`、RxJava 全般 | Android SDK、RxAndroid、`presentation` |
| `presentation` | `domain`、`di`、Android SDK、RxAndroid、DataBinding | `data` |

```
com.example.rxjava/
  domain/       model/ repository/ usecase/
  data/         catalog/ repository/
  presentation/ list/ run/
  di/           ServiceLocator
```

## 中心にある設計

サンプルの実行結果を `Observable<SampleLogLine>` として表現する。
これが 3 層すべてを貫く唯一の契約で、他の設計判断はここから導かれる。

- **cold である。** `SampleRepository.run()` も UseCase も、購読するまで何も起こさない。
  Scheduler の指定もしない
- **スレッド名は放出時に確定する。** `SampleLogLine` が生成時のスレッド名を自分で持つ。
  観察対象がスレッド名にしか現れないため、ここが崩れると目的そのものが失われる
- **表示形式を知らない。** `[スレッド名] メッセージ` の組み立ては `presentation` の仕事

`data` は 2 系統の変換だけを提供する。
オペレータの結果を見せる `SampleLogs.materialize` と、
手続き的に書く `SampleLogs.create` である。
`Subject` を立てて `mergeWith` で合流させる書き方は、取りこぼしが出るため使わない。

## 依存の解決

`di/ServiceLocator` が Repository と UseCase を組み立てる遅延シングルトン。
ViewModel はここから UseCase を取る。

依存グラフは Repository 1 つと UseCase 3 つしかなく、
学習用アプリでは結線が目で追えることに価値があるため、DI ライブラリは使わない。
サンプルの出所を差し替えるときは、ここの 1 行を変える。

---

この文書は完成形の設計を書いている。
実装がどこまで追いついているかは [status.md](status.md) を参照。
