package motakashi.androidapp.pomodorotimer

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * ポモドーロタイマーの設定画面とタイマー画面を管理するアクティビティです。
 */
class MainActivity : Activity() {
    private val stateReceiver = object : BroadcastReceiver() {
        /**
         * タイマーサービスから通知された状態を画面へ反映します。
         */
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != TimerService.ACTION_STATE) {
                return
            }
            if (!intent.getBooleanExtra(TimerService.EXTRA_HAS_SESSION, false)) {
                showSettings()
                return
            }
            showTimer()
            renderState(intent)
        }
    }

    private var selectedSets = DEFAULT_SETS
    private var selectedLongBreakMinutes = DEFAULT_LONG_BREAK_MINUTES
    private var receiverRegistered = false

    private lateinit var settingsPanel: View
    private lateinit var timerPanel: View
    private lateinit var setCount: TextView
    private lateinit var breakMinutes: TextView
    private lateinit var setProgress: TextView
    private lateinit var phaseLabel: TextView
    private lateinit var timeRemaining: TextView
    private lateinit var timeProgress: MinuteProgressView
    private lateinit var pauseButton: Button
    private lateinit var resumeButton: Button

    /**
     * アクティビティを初期化し、画面部品と初期設定を準備します。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        renderSettings()
        showSettings()
        requestNotificationPermission()
    }

    /**
     * タイマーサービスからの状態通知を購読し、現在の状態を要求します。
     */
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(TimerService.ACTION_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(stateReceiver, filter)
        }
        receiverRegistered = true
        sendServiceCommand(TimerService.ACTION_QUERY)
    }

    /**
     * タイマーサービスからの状態通知を購読解除します。
     */
    override fun onStop() {
        if (receiverRegistered) {
            unregisterReceiver(stateReceiver)
            receiverRegistered = false
        }
        super.onStop()
    }

    /**
     * レイアウト内のビューを取得し、各種クリック処理を設定します。
     */
    private fun bindViews() {
        settingsPanel = findViewById(R.id.settings_panel)
        timerPanel = findViewById(R.id.timer_panel)
        timerPanel.background = TimerGridDrawable(
            20 * resources.displayMetrics.density,
            getColor(R.color.screen_background)
        )
        setCount = findViewById(R.id.set_count)
        breakMinutes = findViewById(R.id.break_minutes)
        setProgress = findViewById(R.id.set_progress)
        phaseLabel = findViewById(R.id.phase_label)
        timeRemaining = findViewById(R.id.time_remaining)
        timeProgress = findViewById(R.id.time_progress)
        pauseButton = findViewById(R.id.pause_button)
        resumeButton = findViewById(R.id.resume_button)

        findViewById<View>(R.id.set_decrease).setOnClickListener {
            selectedSets = (selectedSets - 1).coerceAtLeast(MIN_SETS)
            renderSettings()
        }
        findViewById<View>(R.id.set_increase).setOnClickListener {
            selectedSets = (selectedSets + 1).coerceAtMost(MAX_SETS)
            renderSettings()
        }
        findViewById<View>(R.id.break_decrease).setOnClickListener {
            selectedLongBreakMinutes =
                (selectedLongBreakMinutes - 1).coerceAtLeast(MIN_LONG_BREAK_MINUTES)
            renderSettings()
        }
        findViewById<View>(R.id.break_increase).setOnClickListener {
            selectedLongBreakMinutes =
                (selectedLongBreakMinutes + 1).coerceAtMost(MAX_LONG_BREAK_MINUTES)
            renderSettings()
        }
        findViewById<View>(R.id.reset_button).setOnClickListener {
            selectedSets = DEFAULT_SETS
            selectedLongBreakMinutes = DEFAULT_LONG_BREAK_MINUTES
            renderSettings()
        }
        findViewById<View>(R.id.start_button).setOnClickListener { startTimer() }
        pauseButton.setOnClickListener { sendServiceCommand(TimerService.ACTION_PAUSE) }
        resumeButton.setOnClickListener { sendServiceCommand(TimerService.ACTION_RESUME) }
        findViewById<View>(R.id.end_button).setOnClickListener {
            sendServiceCommand(TimerService.ACTION_STOP)
            showSettings()
        }
    }

    /**
     * 選択中のセット数と長い休憩時間を設定画面へ表示します。
     */
    private fun renderSettings() {
        setCount.text = selectedSets.toString()
        breakMinutes.text = selectedLongBreakMinutes.toString()
    }

    /**
     * 現在の設定でタイマーサービスを開始します。
     */
    private fun startTimer() {
        val intent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_TOTAL_SETS, selectedSets)
            putExtra(TimerService.EXTRA_LONG_BREAK_MINUTES, selectedLongBreakMinutes)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        showTimer()
    }

    /**
     * 指定したアクションをタイマーサービスへ送信します。
     *
     * @param action タイマーサービスが処理するアクション
     */
    private fun sendServiceCommand(action: String) {
        val intent = Intent(this, TimerService::class.java).apply {
            this.action = action
        }
        startService(intent)
    }

    /**
     * 設定パネルを表示し、タイマーパネルを非表示にします。
     */
    private fun showSettings() {
        settingsPanel.visibility = View.VISIBLE
        timerPanel.visibility = View.GONE
    }

    /**
     * タイマーパネルを表示し、設定パネルを非表示にします。
     */
    private fun showTimer() {
        settingsPanel.visibility = View.GONE
        timerPanel.visibility = View.VISIBLE
    }

    /**
     * サービスから受け取ったタイマー状態を画面へ描画します。
     *
     * @param state タイマー状態を格納したインテント
     */
    private fun renderState(state: Intent) {
        val currentSet = state.getIntExtra(TimerService.EXTRA_CURRENT_SET, 1)
        val totalSets = state.getIntExtra(TimerService.EXTRA_TOTAL_SETS, DEFAULT_SETS)
        val phase = state.getStringExtra(TimerService.EXTRA_PHASE)
        val remainingMillis = state.getLongExtra(TimerService.EXTRA_REMAINING_MILLIS, 0L)
        val phaseDurationMillis =
            state.getLongExtra(TimerService.EXTRA_PHASE_DURATION_MILLIS, 0L)
        val paused = state.getBooleanExtra(TimerService.EXTRA_PAUSED, false)
        val finished = state.getBooleanExtra(TimerService.EXTRA_FINISHED, false)

        setProgress.text = getString(R.string.set_progress_format, currentSet, totalSets)
        phaseLabel.text = labelForPhase(phase)

        val seconds = (remainingMillis + 999L) / 1000L
        timeRemaining.text = getString(R.string.time_remaining_format, seconds / 60L, seconds % 60L)
        val percent = if (phaseDurationMillis > 0L) {
            (100.0 * remainingMillis / phaseDurationMillis).roundToInt()
        } else {
            0
        }
        timeRemaining.setTextColor(when {
            percent >= 67 -> 0xFF207249.toInt()
            percent >= 34 -> 0xFF856100.toInt()
            else -> 0xFFB52D40.toInt()
        })
        timeProgress.render(
            remainingMillis,
            phaseDurationMillis,
            state.getLongExtra(TimerService.EXTRA_SAMPLED_AT_MILLIS, SystemClock.elapsedRealtime()),
            running = !paused && !finished
        )

        pauseButton.visibility = if (paused || finished) View.GONE else View.VISIBLE
        resumeButton.visibility = if (paused && !finished) View.VISIBLE else View.GONE
    }

    /**
     * フェーズ名に対応する表示用ラベルを取得します。
     *
     * @param phase タイマーサービスから受け取ったフェーズ名
     * @return 画面に表示するフェーズラベル
     */
    private fun labelForPhase(phase: String?): String = when (phase) {
        TimerEngine.Phase.WORK.name -> getString(R.string.phase_work)
        TimerEngine.Phase.SHORT_BREAK.name -> getString(R.string.phase_short_break)
        TimerEngine.Phase.LONG_BREAK.name -> getString(R.string.phase_long_break)
        else -> getString(R.string.phase_finished)
    }

    /**
     * Android 13 以降で通知権限を必要に応じて要求します。
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    companion object {
        private const val DEFAULT_SETS = 4
        private const val DEFAULT_LONG_BREAK_MINUTES = 20
        private const val MIN_SETS = 1
        private const val MAX_SETS = 99
        private const val MIN_LONG_BREAK_MINUTES = 15
        private const val MAX_LONG_BREAK_MINUTES = 30
    }
}
