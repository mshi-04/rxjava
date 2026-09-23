# UseCase

1 操作 1 UseCase で、取得の規則を presentation / data へ漏らさない。

## 確認する対象

- `domain/usecase/GetForecastUseCase` の現在の実装
- 依存する `ForecastRepository` のシグネチャ
- 呼び出している `presentation/viewmodel/ForecastViewModel`

## 実装時の注意

- 1 つの UseCase は 1 つの明確な操作を表す。
- 公開メソッドは `invoke` だけにする。名前で操作を表し、引数で対象を表す。
- コンストラクタで `ForecastRepository` を受け取り、`@Inject` を付ける。
  Hilt がコンストラクタ注入で組み立てるため、Module へ書き足す必要はない。
- 戻り値は `Single<List<DailyForecast>>` のまま、cold で返す。`subscribeOn` / `observeOn` を付けない。
  Scheduler の指定は presentation に限る。
- 失敗はそのまま `onError` として通す。空リストや `null` へ潰さない。
  潰すと、待っている状態と失敗した状態が呼び出し側で区別できなくなる。
- `GetForecastUseCase` は現状、委譲 1 行で構わない。それでも置くのは、
  `presentation` が `ForecastRepository` へ直接依存すると、地点の既定値や取得の再試行といった
  規則の置き場が `presentation` へ流れるためである。

## テスト判断

- 委譲だけの UseCase に単体テストは不要。`ForecastMapper` や Repository 実装側のテストで足りる。
- 分岐・順序・失敗経路を持たせたときに JVM Unit Test を足す。

## 検証観点

- 委譲だけの UseCase を新たに増やしていないか（結合を避ける必要があるときだけ挟む）。
- `presentation` が `ForecastRepository` を直接参照していないか。
