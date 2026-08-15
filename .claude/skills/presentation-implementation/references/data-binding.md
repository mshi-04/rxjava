# DataBinding

レイアウト側で表示と操作を完結させ、Activity にコードを書かせない。

## 確認する対象

- `app/src/main/res/layout/` の 3 枚と、それぞれの `<data>` ブロック
- 対応する binding クラスを使っている Activity / Adapter

## レイアウトと変数

| レイアウト | 受け取る変数 |
| --- | --- |
| `activity_main.xml` | なし（`RecyclerView` の参照取得のみ） |
| `item_sample.xml` | `sample`, `clickListener` |
| `activity_sample.xml` | `sample`, `viewModel` |

## 実装時の注意

- ルート要素は `<layout>` にする。変数を使わない画面も例外にしない。
  binding クラスの生成が揃わないと、参照側でだけ規約が変わる。
- 押下はレイアウト側で ViewModel のメソッドへ束ねる。

```xml
<Button
    android:enabled="@{viewModel.running}"
    android:onClick="@{() -> viewModel.stop()}" />
```

- `ObservableBoolean` / `ObservableField` は `@{viewModel.running}` の形で読む。`.get()` は書かない。
- 文字サイズや色を直接書かず、Material 3 の theme 属性（`?attr/textAppearanceTitleMedium` など）を使う。
- `paddingVertical` / `paddingHorizontal` は API 26 以降のため使わない。個別に書く。
- Adapter で binding を使い回すときは、値を差し替えたあとに `executePendingBindings()` を呼ぶ。
  忘れると再利用された行に前の値が残る。
- 変数の型は完全修飾名で書く。domain の model を渡し、data の型は渡さない。

## ビルドが通らないとき

DataBinding の失敗は Java 側のエラーだけを見ても分からないことが多い。
原因のレイアウトは `app/build/reports/data_binding/` 以下のログに出る。

## テスト判断

- binding 式は JVM Unit Test で検証できない。エミュレータでの確認に回す。

## 検証観点

- レイアウトが参照している名前と、ViewModel が公開しているフィールド名が一致しているか。
- `<layout>` で包まれていないレイアウトが混ざっていないか。
