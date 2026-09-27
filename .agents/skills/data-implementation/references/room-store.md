# Room store

`ForecastStore` の Room 実装。`data/local/` に閉じる。

## 確認する対象

- `data/local/entity/` の Entity、`data/local/dao/ForecastDao`、`ForecastDatabase`、`ForecastEntityMapper`、`ForecastStoreImpl`
- `data/repository/ForecastStore` の契約
- `di/DatabaseModule` と `di/RepositoryModule`
- `app/schemas/` のスキーマ JSON
- [`docs/data.md`](../../../../docs/data.md) の「ForecastStore と Room」

## 実装時の注意

- Room・AndroidX SQLite・AndroidX annotation を使ってよいのは `data/local/` だけ。
  Android SDK と `Context` は持ち込まない。DB の生成は `di/DatabaseModule` が行う。
- Entity を `local/` の外へ出さない。`ForecastStore` が返すのは `domain` のモデルだけにする。
- Entity と `domain` の写像は `ForecastEntityMapper` だけが行う。
  天気区分は対応表で保存文字列と結び、`name()` / `valueOf()` に頼らない。現在の保存値を変えない。
- 保存値が不正なら例外にする。`UNKNOWN` や既定値に落とさない。
- `read` は cold な `Maybe<Snapshot>`、`save` は cold な `Completable` を返す。
- `save` は渡された一覧を呼び出し時点でコピーし、DB 操作は購読まで始めない。
- 予報の置換と日時の更新は DAO の `@Transaction replace` で 1 操作にまとめる。
- 保存は Room の transaction executor 上で実行する。この `subscribeOn` だけは `local` に許される。
  blocking 呼び出しは使わない。
- migration は用意しない。スキーマを変えるときは [`docs/development.md`](../../../../docs/development.md) の手順に従う。
  Entity の改名だけならスキーマ JSON は変わらないことを `git diff app/schemas` で確かめる。

## テスト判断

- `ForecastEntityMapper` と `ForecastStore.Snapshot` は JVM Unit Test で検証する。
- `ForecastStoreImpl` は実際の DB を要するため instrumented test で検証する。
  観点の一覧は [`docs/ui-test.md`](../../../../docs/ui-test.md) の「ForecastStoreImpl を実際の DB で検証する」を正本とする。

## 検証観点

- Entity や DAO が `local/` の外から参照されていないか。
- 天気区分の保存に `name()` / `valueOf()` が戻っていないか。
- 置換と日時更新が別トランザクションに分かれていないか。
- 購読前に DB を開いていないか。
