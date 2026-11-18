package motakashi.androidapp.pomodorotimer.e2e;

import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static motakashi.androidapp.pomodorotimer.e2e.TimerElement.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimerE2ETest {
    private AndroidDriver driver;
    private TimerScreen screen;

    @BeforeEach
    void setUp() throws Exception {
        // アプリを起動し、設定画面が表示されるまで待つ。
        driver = AndroidDriverFactory.create();
        screen = new TimerScreen(driver, AndroidTimerLocators::locator);
        screen.waitUntilVisible(START);
    }

    @AfterEach
    void tearDown() {
        // テスト後に Appium セッションを終了する。
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void 設定値を変更してリセットできる() {
        // 設定の初期値を確認する。
        assertEquals("4", screen.text(SET_COUNT));
        assertEquals("20", screen.text(BREAK_MINUTES));

        // セット数を増減し、表示が更新されることを確認する。
        screen.click(SET_INCREASE);
        screen.waitForText(SET_COUNT, "5");
        screen.click(SET_DECREASE);
        screen.click(SET_DECREASE);
        screen.waitForText(SET_COUNT, "3");

        // 長い休憩時間を増減し、表示が更新されることを確認する。
        screen.click(BREAK_INCREASE);
        screen.waitForText(BREAK_MINUTES, "21");
        screen.click(BREAK_DECREASE);
        screen.click(BREAK_DECREASE);
        screen.waitForText(BREAK_MINUTES, "19");

        // 両方の設定が初期値に戻ることを確認する。
        screen.click(RESET);
        screen.waitForText(SET_COUNT, "4");
        screen.waitForText(BREAK_MINUTES, "20");
    }

    @Test
    void タイマーを開始一時停止再開終了できる() throws InterruptedException {
        // セット数を変更して開始し、集中フェーズの表示を確認する。
        screen.click(SET_INCREASE);
        screen.waitForText(SET_COUNT, "5");
        screen.click(START);
        screen.waitForText(SET_PROGRESS, "セット 1 / 5");
        screen.waitForText(PHASE, "集中");
        assertTrue(screen.text(TIME_REMAINING).matches("残り (?:24:[0-5][0-9]|25:00)"));

        // 一時停止中は時間が進まないことを確認する。
        screen.click(PAUSE);
        screen.waitUntilVisible(RESUME);
        String pausedTime = screen.text(TIME_REMAINING);
        Thread.sleep(2000);
        assertEquals(pausedTime, screen.text(TIME_REMAINING));

        // 再開後に残り時間が変化することを確認する。
        screen.click(RESUME);
        screen.waitUntilVisible(PAUSE);
        screen.waitForDifferentText(TIME_REMAINING, pausedTime);

        // 終了後に設定画面へ戻り、設定値が保持されることを確認する。
        screen.click(END);
        screen.waitUntilVisible(START);
        assertEquals("5", screen.text(SET_COUNT));
    }
}
