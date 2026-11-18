# E2E構築メモ

## 目的

PomodoroTimer の Android アプリを Appium で実操作する E2E テストを追加する。既存の Robolectric による JVM テストは維持する。

## 現状の確認

- リポジトリには Android アプリのみがあり、iOS アプリや Xcode プロジェクトはない。
- UI は Android Views。既存の画面操作には resource ID が付与されている。
- 既存テストは `app/src/test` の Robolectric/JUnit テスト。
- タイマーは Foreground Service で動作する。強制終了後の状態復元は未対応。

## 採用方針

- Appium 2 + UiAutomator2 + Appium Java Client + JUnit 5。
- テストは独立した `e2e/` プロジェクトに分離し、Android エミュレータで実行する。
- 共通のユーザーフローを画面オブジェクトにまとめ、driver/capabilities と OS 固有 locator を分離する。iOS アプリが用意された場合にシナリオを再利用しやすくする。
- 通知権限を含む OS UI の扱いを整理する。時間経過を長時間待つテストや、強制終了後の復元は初期スコープに含めない。

## 作業順序と状態

作業は一項目ずつ順に行い、完了のたびにこのメモを更新する。

| 順番 | 作業 | 状態 |
| --- | --- | --- |
| 1 | Appium Java Client / JUnit 5 の E2E プロジェクトと実行設定 | 完了 |
| 2 | Android の設定・開始・一時停止・再開・終了シナリオ | 完了 |
| 3 | README に環境準備と実行手順を記載 | 完了 |
| 4 | APK ビルドとエミュレータ上の E2E 実行で検証 | 完了 |

## 実施記録

- `:e2e` を独立 Gradle サブプロジェクトとして追加し、Java 17、Appium Java Client 10.1.1、JUnit 5 を設定した。
- Android driver factory は UiAutomator2、アプリ ID / Activity、APK パス、Appium server URL、端末名を設定する。
- `:e2e:test` が APK をビルドし、基盤テスト 1 件が成功した。
- 既存 `:app:testDebugUnitTest` は 24 件成功した。
- ユーザー環境では UiAutomator2 doctor の必須項目がすべて解消済み。IDE シェルでは Appium CLI と Java が PATH に見つからず、接続中の Android 端末はなかった。
- Android の設定値の増減・リセット、および開始・一時停止・再開・終了の2シナリオを追加した。通知権限はドライバーで自動許可する。
- テストコードのコンパイルと既存のドライバー基盤テストは成功。専用 Android API 37 エミュレータ、Appium 2.19.0、UiAutomator2 4.2.9 で APK ビルドと2つの E2E シナリオが成功した。
- E2E は Appium の通常のリセット動作を使用するため、対象端末のアプリデータが消去される。検証には専用エミュレータを使用する。
- README.md の既存内容を保持して Android E2E の準備、エミュレータと Appium の起動、テスト実行およびデータ消去の注意を追記した。既定ポートが使用中だったため、別ポートの指定方法も追記した。
- 検証ではポート 4724 の Appium サーバーを一時的に起動し、`:e2e:test --rerun-tasks -PappiumServerUrl=http://127.0.0.1:4724` を実行した。専用サーバーとエミュレータは実行後に停止した。

## 保存場所

このメモはリポジトリの `e2e/` にある作業記録である。アプリ側の実装は未変更で、リポジトリ内では Gradle 設定、`e2e/`、README を変更している。
