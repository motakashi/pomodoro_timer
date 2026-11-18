package motakashi.androidapp.pomodorotimer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class TimerServiceTest {

    private lateinit var controller: ServiceController<TimerService>
    private lateinit var service: TimerService
    private lateinit var receivedIntents: MutableList<Intent>
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { receivedIntents.add(it) }
        }
    }

    @Before
    fun setUp() {
        receivedIntents = mutableListOf()
        controller = Robolectric.buildService(TimerService::class.java)
        service = controller.create().get()

        val filter = IntentFilter(TimerService.ACTION_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            service.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION", "UnspecifiedRegisterReceiverFlag")
            service.registerReceiver(receiver, filter)
        }
    }

    @Test
    fun `セッションなしの状態でACTION_QUERYを送信するとHAS_SESSIONがfalseで返る`() {
        val queryIntent = Intent(TimerService.ACTION_QUERY)
        service.onStartCommand(queryIntent, 0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, receivedIntents.size)
        val state = receivedIntents.last()
        assertEquals(TimerService.ACTION_STATE, state.action)
        assertFalse(state.getBooleanExtra(TimerService.EXTRA_HAS_SESSION, true))
    }

    @Test
    fun `ACTION_STARTを送信するとセッションが開始され状態がブロードキャストされる`() {
        val startIntent = Intent(TimerService.ACTION_START).apply {
            putExtra(TimerService.EXTRA_TOTAL_SETS, 4)
            putExtra(TimerService.EXTRA_LONG_BREAK_MINUTES, 20)
        }
        service.onStartCommand(startIntent, 0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, receivedIntents.size)
        val state = receivedIntents.last()
        assertEquals(TimerService.ACTION_STATE, state.action)
        assertTrue(state.getBooleanExtra(TimerService.EXTRA_HAS_SESSION, false))
        assertEquals(4, state.getIntExtra(TimerService.EXTRA_TOTAL_SETS, 0))
        assertEquals(1, state.getIntExtra(TimerService.EXTRA_CURRENT_SET, 0))
        assertEquals(TimerEngine.Phase.WORK.name, state.getStringExtra(TimerService.EXTRA_PHASE))
        assertFalse(state.getBooleanExtra(TimerService.EXTRA_PAUSED, true))

        val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = shadowOf(notificationManager)
        assertEquals(1, shadowNm.allNotifications.size)
    }

    @Test
    fun `タイマーの開始から一時停止と再開および終了のライフサイクルが正しく機能する`() {
        // スタート
        val startIntent = Intent(TimerService.ACTION_START).apply {
            putExtra(TimerService.EXTRA_TOTAL_SETS, 4)
            putExtra(TimerService.EXTRA_LONG_BREAK_MINUTES, 20)
        }
        service.onStartCommand(startIntent, 0, 1)
        shadowOf(Looper.getMainLooper()).idle()

        // 一時停止
        val pauseIntent = Intent(TimerService.ACTION_PAUSE)
        service.onStartCommand(pauseIntent, 0, 2)
        shadowOf(Looper.getMainLooper()).idle()

        var state = receivedIntents.last()
        assertTrue(state.getBooleanExtra(TimerService.EXTRA_PAUSED, false))

        // 再開
        val resumeIntent = Intent(TimerService.ACTION_RESUME)
        service.onStartCommand(resumeIntent, 0, 3)
        shadowOf(Looper.getMainLooper()).idle()

        state = receivedIntents.last()
        assertFalse(state.getBooleanExtra(TimerService.EXTRA_PAUSED, true))

        // ストップ
        val stopIntent = Intent(TimerService.ACTION_STOP)
        service.onStartCommand(stopIntent, 0, 4)
        shadowOf(Looper.getMainLooper()).idle()

        state = receivedIntents.last()
        assertFalse(state.getBooleanExtra(TimerService.EXTRA_HAS_SESSION, true))
    }

    @Test
    fun `onBindはnullを返す`() {
        assertNull(service.onBind(null))
    }
}
