package motakashi.androidapp.pomodorotimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class TimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var engine: TimerEngine? = null

    private val tick = object : Runnable {
        override fun run() {
            val timer = engine ?: return
            val changed = advanceWithVibration(timer, SystemClock.elapsedRealtime())
            broadcastState()
            if (changed || timer.isFinished) {
                updateNotification()
            }
            if (!timer.isFinished) {
                handler.postDelayed(this, TICK_INTERVAL_MILLIS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                val timer = TimerEngine()
                timer.start(
                    intent.getIntExtra(EXTRA_TOTAL_SETS, 4),
                    intent.getIntExtra(EXTRA_LONG_BREAK_MINUTES, 20),
                    SystemClock.elapsedRealtime()
                )
                engine = timer
                handler.removeCallbacks(tick)
                updateNotification()
                broadcastState()
                handler.postDelayed(tick, TICK_INTERVAL_MILLIS)
            }
            ACTION_PAUSE -> engine?.let { timer ->
                val now = SystemClock.elapsedRealtime()
                advanceWithVibration(timer, now)
                timer.pause(now)
                handler.removeCallbacks(tick)
                broadcastState()
                updateNotification()
            }
            ACTION_RESUME -> engine?.let { timer ->
                timer.resume(SystemClock.elapsedRealtime())
                broadcastState()
                handler.removeCallbacks(tick)
                if (!timer.isFinished) {
                    handler.postDelayed(tick, TICK_INTERVAL_MILLIS)
                }
                updateNotification()
            }
            ACTION_STOP -> {
                handler.removeCallbacks(tick)
                engine = null
                broadcastNoSession()
                stopForeground(true)
                stopSelf(startId)
            }
            ACTION_QUERY -> {
                val timer = engine
                if (timer == null) {
                    broadcastNoSession()
                    stopSelf(startId)
                } else {
                    if (advanceWithVibration(timer, SystemClock.elapsedRealtime())) {
                        updateNotification()
                    }
                    broadcastState()
                    if (timer.isFinished) {
                        handler.removeCallbacks(tick)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun advanceWithVibration(timer: TimerEngine, now: Long): Boolean {
        val changed = timer.advance(now)
        if (changed) vibrateForPhaseChange()
        return changed
    }

    @Suppress("DEPRECATION")
    private fun vibrateForPhaseChange() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            getSystemService(Vibrator::class.java)
        } ?: return
        if (!vibrator.hasVibrator()) return

        val durationMillis = 2_000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val effect = VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect, attributes)
            } else {
                vibrator.vibrate(durationMillis, attributes)
            }
        }
    }

    private fun broadcastState() {
        val timer = engine ?: return broadcastNoSession()
        val sampledAtMillis = SystemClock.elapsedRealtime()
        val state = Intent(ACTION_STATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_HAS_SESSION, true)
            putExtra(EXTRA_TOTAL_SETS, timer.totalSets)
            putExtra(EXTRA_CURRENT_SET, timer.currentSet)
            putExtra(EXTRA_LONG_BREAK_MINUTES, timer.longBreakMinutes)
            putExtra(EXTRA_PHASE, timer.phase.name)
            putExtra(EXTRA_REMAINING_MILLIS, timer.getRemainingMillis(sampledAtMillis))
            putExtra(EXTRA_SAMPLED_AT_MILLIS, sampledAtMillis)
            putExtra(EXTRA_PHASE_DURATION_MILLIS, timer.phaseDurationMillis)
            putExtra(EXTRA_PAUSED, timer.isPaused)
            putExtra(EXTRA_FINISHED, timer.isFinished)
        }
        sendBroadcast(state)
    }

    private fun broadcastNoSession() {
        val state = Intent(ACTION_STATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_HAS_SESSION, false)
        }
        sendBroadcast(state)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timer = engine
        val content = when {
            timer == null -> getString(R.string.notification_running)
            timer.isFinished -> getString(R.string.phase_finished)
            else -> "${phaseLabel(timer.phase)} · ${
                getString(
                    if (timer.isPaused) R.string.notification_paused
                    else R.string.notification_running
                )
            }"
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun phaseLabel(phase: TimerEngine.Phase): String = when (phase) {
        TimerEngine.Phase.WORK -> getString(R.string.phase_work)
        TimerEngine.Phase.SHORT_BREAK -> getString(R.string.phase_short_break)
        TimerEngine.Phase.LONG_BREAK -> getString(R.string.phase_long_break)
        TimerEngine.Phase.FINISHED -> getString(R.string.phase_finished)
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    companion object {
        const val ACTION_START = "motakashi.androidapp.pomodorotimer.START"
        const val ACTION_PAUSE = "motakashi.androidapp.pomodorotimer.PAUSE"
        const val ACTION_RESUME = "motakashi.androidapp.pomodorotimer.RESUME"
        const val ACTION_STOP = "motakashi.androidapp.pomodorotimer.STOP"
        const val ACTION_QUERY = "motakashi.androidapp.pomodorotimer.QUERY"
        const val ACTION_STATE = "motakashi.androidapp.pomodorotimer.STATE"
        const val EXTRA_TOTAL_SETS = "total_sets"
        const val EXTRA_LONG_BREAK_MINUTES = "long_break_minutes"
        const val EXTRA_HAS_SESSION = "has_session"
        const val EXTRA_CURRENT_SET = "current_set"
        const val EXTRA_PHASE = "phase"
        const val EXTRA_REMAINING_MILLIS = "remaining_millis"
        const val EXTRA_SAMPLED_AT_MILLIS = "sampled_at_millis"
        const val EXTRA_PHASE_DURATION_MILLIS = "phase_duration_millis"
        const val EXTRA_PAUSED = "paused"
        const val EXTRA_FINISHED = "finished"

        private const val CHANNEL_ID = "pomodoro_timer"
        private const val NOTIFICATION_ID = 1
        private const val TICK_INTERVAL_MILLIS = 1000L
    }
}
