# Activity（View）

レイアウトを束ねるだけの薄い層に保つ。

## 確認する対象

- `presentation/list/MainActivity` と `presentation/run/SampleActivity`
- 対応するレイアウトの `<data>` ブロック
- `AndroidManifest.xml` の Activity 宣言

## 実装時の注意

View がしてよいのは 3 つだけ。

- レイアウトを `DataBindingUtil.setContentView` で束ね、変数を渡す
- `RecyclerView` の `LayoutManager` と `Adapter` を設定する
- 画面遷移の `Intent` を組む

守ること。

- 条件分岐・整形・購読を書かない。それらは ViewModel の仕事。
- `findViewById` と `setOnClickListener` を使わない。
- 画面遷移の `Intent` はその Activity 自身の `static createIntent(...)` で組む。
  extra のキーを呼び出し側に知らせない。
- extra には値オブジェクトの生の値（`SampleId.value()`）を載せ、受け側で型に戻す。
- Activity を増やすときは `presentation` の下にパッケージを 1 つ足し、
  Activity と ViewModel を対で置く。マニフェストの `parentActivityName` も設定する。

## テスト判断

- Activity は JVM Unit Test で検証しない。エミュレータでの確認に回す。

## 検証観点

- `data` 層のクラスを import していないか。
- ロジックが Activity へ染み出していないか（`if` が出てきたら疑う）。
