package motakashi.androidapp.pomodorotimer.e2e;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AndroidDriverFactoryTest {
    @Test
    void APKパスがない場合は接続前にエラーになる() {
        // APK パスを一時的に外し、接続前にエラーとなることを確認する。
        String configuredApp = System.getProperty("appium.app");
        try {
            System.clearProperty("appium.app");
            assertThrows(IllegalStateException.class, AndroidDriverFactory::create);
        } finally {
            // 他のテストに影響しないよう、元の設定に戻す。
            if (configuredApp == null) {
                System.clearProperty("appium.app");
            } else {
                System.setProperty("appium.app", configuredApp);
            }
        }
    }
}
