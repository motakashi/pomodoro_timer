# ポモドーロタイマー

横画面で使う、作業・休憩サイクル用のAndroidアプリです。

## 機能

- セット数を1〜99、長い休憩を15〜30分に設定できます（初期値は4セット、20分）。
- 1セットは作業25分と短い休憩5分です。
- 4セットごとに長い休憩を入れます。長い休憩の後に次のセットがある場合に限り、4セット目の短い休憩を長い休憩に置き換えます。最終セットの後は5分休憩して終了します。
- バックグラウンドでも動作するフォアグラウンドサービスを使います。アプリが強制終了された後の状態復元には対応しません。

## ビルド

JDK 17 と Android SDK 35 を用意し、プロジェクトのルートディレクトリで実行してください。Gradle Wrapper が設定済みの Gradle 9.6.0 を初回実行時にダウンロードします。

| 操作 | Windows（PowerShell） | Linux |
| --- | --- | --- |
| ユニットテスト実行（デバッグ） | `.\gradlew.bat :app:testDebugUnitTest` | `./gradlew :app:testDebugUnitTest` |
| ビルド（テスト・検証を含む） | `.\gradlew.bat :app:build` | `./gradlew :app:build` |
| デバッグ APK 生成 | `.\gradlew.bat :app:assembleDebug` | `./gradlew :app:assembleDebug` |

Linux で `gradlew` に実行権限がない場合は、先に `chmod +x gradlew` を実行してください。

デバッグ APK の出力先は `app/build/outputs/apk/debug/` です。

Android Studio からビルドする場合も、JDK 17 と Android SDK 35 を使用してください。

## Android E2E テスト

Appium で Android エミュレータ上の設定値の増減・リセットと、タイマーの開始・一時停止・再開・終了を検証します。テストコードは `e2e/` にあります。iOS の実行には対応していません。

**注意:** テストは対象アプリのデータをリセットし、通知などの実行時権限を自動的に許可します。大切なデータのある端末ではなく、テスト専用のエミュレータを使用してください。複数端末が接続されている場合は、対象を誤らないようエミュレータ1台だけを接続してください。

### 準備

1. JDK 17、Android SDK 35、Node.js と npm を用意します。Android Studio の SDK Manager で Android SDK Platform-Tools、Android Emulator、Android SDK Command-line Tools (latest) をインストールし、テスト用 AVD を作成してください。
2. Appium を起動するターミナルで、`JAVA_HOME` を JDK、`ANDROID_HOME` を Android SDK のディレクトリに設定します。Android Studio の SDK Manager で SDK の場所を確認できます。
3. Appium 2 と、その互換バージョンの UiAutomator2 driver をインストールし、設定を確認します。

```powershell
npm install --global appium@2
appium driver install uiautomator2@4.2.9
appium driver list --installed
appium driver doctor uiautomator2
```

`doctor` の必須項目を解消してください。`bundletool`、`ffmpeg`、GStreamer はこのテストには不要なオプションです。Windows で `appium` が認識されない場合は `npm config get prefix` で表示されるディレクトリを PATH に追加し、ターミナルを開き直してください。

### 実行

1. Android Studio の Device Manager からテスト用エミュレータを起動し、完全に起動するまで待ちます。`adb devices` で対象が `device` として1台だけ表示されることを確認します。Windows で `adb` が PATH にない場合は `& "$env:ANDROID_HOME\platform-tools\adb.exe" devices` で確認できます。
2. Appium を起動し、そのターミナルを開いたままにします。

```powershell
appium --address 127.0.0.1 --port 4723
```

3. 別のターミナルでプロジェクトのルートから実行します。デバッグ APK はテスト前に自動でビルドされます。

| Windows（PowerShell） | Linux |
| --- | --- |
| `.\gradlew.bat :e2e:test --rerun-tasks` | `./gradlew :e2e:test --rerun-tasks` |

テスト結果は `e2e/build/reports/tests/test/index.html` で確認できます。`--rerun-tasks` を付けることで、前回の結果を再利用せずに実行できます。Appium を使い終えたら、起動したターミナルで `Ctrl+C` を押して停止してください。

ポート 4723 が使用中の場合は、例えば `appium --address 127.0.0.1 --port 4724` で起動し、Gradle コマンドに `-PappiumServerUrl=http://127.0.0.1:4724` を追加してください。
