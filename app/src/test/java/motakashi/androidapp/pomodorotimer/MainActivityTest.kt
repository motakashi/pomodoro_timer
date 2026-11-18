package motakashi.androidapp.pomodorotimer

import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    private lateinit var controller: ActivityController<MainActivity>
    private lateinit var activity: MainActivity

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(MainActivity::class.java)
        activity = controller.create().start().resume().get()
    }

    @Test
    fun `初期状態で設定パネルが表示されデフォルト値がセットされている`() {
        val settingsPanel = activity.findViewById<View>(R.id.settings_panel)
        val timerPanel = activity.findViewById<View>(R.id.timer_panel)
        val setCount = activity.findViewById<TextView>(R.id.set_count)
        val breakMinutes = activity.findViewById<TextView>(R.id.break_minutes)

        assertEquals(View.VISIBLE, settingsPanel.visibility)
        assertEquals(View.GONE, timerPanel.visibility)
        assertEquals("4", setCount.text.toString())
        assertEquals("20", breakMinutes.text.toString())
    }

    @Test
    fun `onStartでACTION_QUERYサービスコマンドが送信される`() {
        val shadowApp = shadowOf(activity.application)
        val startedIntent = shadowApp.nextStartedService
        assertNotNull(startedIntent)
        assertEquals(TimerService.ACTION_QUERY, startedIntent?.action)
    }

    @Test
    fun `セット数の増減ボタンで値が更新される`() {
        val setCount = activity.findViewById<TextView>(R.id.set_count)
        val increaseBtn = activity.findViewById<View>(R.id.set_increase)
        val decreaseBtn = activity.findViewById<View>(R.id.set_decrease)

        increaseBtn.performClick()
        assertEquals("5", setCount.text.toString())

        decreaseBtn.performClick()
        decreaseBtn.performClick()
        assertEquals("3", setCount.text.toString())
    }

    @Test
    fun `長い休憩時間の増減ボタンで値が更新される`() {
        val breakMinutes = activity.findViewById<TextView>(R.id.break_minutes)
        val increaseBtn = activity.findViewById<View>(R.id.break_increase)
        val decreaseBtn = activity.findViewById<View>(R.id.break_decrease)

        increaseBtn.performClick()
        assertEquals("21", breakMinutes.text.toString())

        decreaseBtn.performClick()
        decreaseBtn.performClick()
        assertEquals("19", breakMinutes.text.toString())
    }

    @Test
    fun `リセットボタンで初期値に戻る`() {
        val setCount = activity.findViewById<TextView>(R.id.set_count)
        val breakMinutes = activity.findViewById<TextView>(R.id.break_minutes)
        val setIncreaseBtn = activity.findViewById<View>(R.id.set_increase)
        val breakIncreaseBtn = activity.findViewById<View>(R.id.break_increase)
        val resetBtn = activity.findViewById<View>(R.id.reset_button)

        setIncreaseBtn.performClick()
        breakIncreaseBtn.performClick()

        assertEquals("5", setCount.text.toString())
        assertEquals("21", breakMinutes.text.toString())

        resetBtn.performClick()

        assertEquals("4", setCount.text.toString())
        assertEquals("20", breakMinutes.text.toString())
    }

    @Test
    fun `スタートボタンを押すとTimerServiceが開始されタイマーパネルが表示される`() {
        val shadowApp = shadowOf(activity.application)
        // Consume initial ACTION_QUERY intent sent during onStart()
        shadowApp.nextStartedService

        val startBtn = activity.findViewById<View>(R.id.start_button)
        startBtn.performClick()

        val settingsPanel = activity.findViewById<View>(R.id.settings_panel)
        val timerPanel = activity.findViewById<View>(R.id.timer_panel)

        assertEquals(View.GONE, settingsPanel.visibility)
        assertEquals(View.VISIBLE, timerPanel.visibility)

        val startedServiceIntent = shadowApp.nextStartedService
        assertNotNull(startedServiceIntent)
        assertEquals(TimerService.ACTION_START, startedServiceIntent?.action)
        assertEquals(4, startedServiceIntent?.getIntExtra(TimerService.EXTRA_TOTAL_SETS, 0))
        assertEquals(20, startedServiceIntent?.getIntExtra(TimerService.EXTRA_LONG_BREAK_MINUTES, 0))
    }

    @Test
    fun `タイマー状態のブロードキャスト受信時に画面情報が正しく更新される`() {
        val intent = Intent(TimerService.ACTION_STATE).apply {
            putExtra(TimerService.EXTRA_HAS_SESSION, true)
            putExtra(TimerService.EXTRA_CURRENT_SET, 2)
            putExtra(TimerService.EXTRA_TOTAL_SETS, 4)
            putExtra(TimerService.EXTRA_PHASE, TimerEngine.Phase.WORK.name)
            putExtra(TimerService.EXTRA_REMAINING_MILLIS, 25L * 60L * 1000L)
            putExtra(TimerService.EXTRA_PHASE_DURATION_MILLIS, 25L * 60L * 1000L)
            putExtra(TimerService.EXTRA_PAUSED, false)
            putExtra(TimerService.EXTRA_FINISHED, false)
        }

        activity.sendBroadcast(intent)
        shadowOf(Looper.getMainLooper()).idle()

        val setProgress = activity.findViewById<TextView>(R.id.set_progress)
        val phaseLabel = activity.findViewById<TextView>(R.id.phase_label)
        val timeRemaining = activity.findViewById<TextView>(R.id.time_remaining)
        val pauseButton = activity.findViewById<Button>(R.id.pause_button)
        val resumeButton = activity.findViewById<Button>(R.id.resume_button)

        assertEquals("セット 2 / 4", setProgress.text.toString())
        assertEquals("集中", phaseLabel.text.toString())
        assertEquals("残り 25:00", timeRemaining.text.toString())
        assertEquals(View.VISIBLE, pauseButton.visibility)
        assertEquals(View.GONE, resumeButton.visibility)
    }

    @Test
    fun `一時停止ボタンと再開ボタンを押した際に適切なサービスコマンドが送信される`() {
        val shadowApp = shadowOf(activity.application)
        // Consume initial ACTION_QUERY intent sent during onStart()
        shadowApp.nextStartedService

        val pauseButton = activity.findViewById<Button>(R.id.pause_button)
        val resumeButton = activity.findViewById<Button>(R.id.resume_button)

        pauseButton.performClick()
        val pauseIntent = shadowApp.nextStartedService
        assertNotNull(pauseIntent)
        assertEquals(TimerService.ACTION_PAUSE, pauseIntent?.action)

        resumeButton.performClick()
        val resumeIntent = shadowApp.nextStartedService
        assertNotNull(resumeIntent)
        assertEquals(TimerService.ACTION_RESUME, resumeIntent?.action)
    }

    @Test
    fun `終了ボタンを押すとSTOPコマンドが送信され設定画面に戻る`() {
        val shadowApp = shadowOf(activity.application)
        // Consume initial ACTION_QUERY intent sent during onStart()
        shadowApp.nextStartedService

        val endButton = activity.findViewById<View>(R.id.end_button)
        endButton.performClick()

        val stopIntent = shadowApp.nextStartedService
        assertNotNull(stopIntent)
        assertEquals(TimerService.ACTION_STOP, stopIntent?.action)

        val settingsPanel = activity.findViewById<View>(R.id.settings_panel)
        val timerPanel = activity.findViewById<View>(R.id.timer_panel)
        assertEquals(View.VISIBLE, settingsPanel.visibility)
        assertEquals(View.GONE, timerPanel.visibility)
    }
}
