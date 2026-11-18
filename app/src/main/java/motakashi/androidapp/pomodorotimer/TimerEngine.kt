package motakashi.androidapp.pomodorotimer

/**
 * ポモドーロタイマーのフェーズ遷移と時間計算を管理します。
 */
class TimerEngine {
    /**
     * タイマーが取り得るフェーズを表します。
     */
    enum class Phase {
        WORK,
        SHORT_BREAK,
        LONG_BREAK,
        FINISHED
    }

    var totalSets: Int = 0
        private set
    var longBreakMinutes: Int = 0
        private set
    var currentSet: Int = 0
        private set
    var phase: Phase = Phase.WORK
        private set
    var phaseDurationMillis: Long = 0L
        private set
    var isPaused: Boolean = false
        private set
    var isFinished: Boolean = false
        private set

    private var endAtMillis: Long = 0L
    private var pausedRemainingMillis: Long = 0L

    /**
     * 指定された設定でタイマーを開始します。
     *
     * @param totalSets 実施するセット数
     * @param longBreakMinutes 長い休憩の時間（分）
     * @param nowMillis 開始時点の経過時間（ミリ秒）
     */
    fun start(totalSets: Int, longBreakMinutes: Int, nowMillis: Long) {
        require(totalSets in 1..99) { "totalSets must be between 1 and 99" }
        require(longBreakMinutes in 15..30) {
            "longBreakMinutes must be between 15 and 30"
        }

        this.totalSets = totalSets
        this.longBreakMinutes = longBreakMinutes
        currentSet = 1
        phase = Phase.WORK
        phaseDurationMillis = WORK_MILLIS
        endAtMillis = nowMillis + WORK_MILLIS
        pausedRemainingMillis = 0L
        isPaused = false
        isFinished = false
    }

    /**
     * 現在時刻に基づいて、必要なフェーズ遷移を進めます。
     *
     * @param nowMillis 現在の経過時間（ミリ秒）
     * @return 1つ以上のフェーズが遷移した場合は true
     */
    fun advance(nowMillis: Long): Boolean {
        if (isPaused || isFinished) {
            return false
        }

        var changed = false
        while (!isFinished && nowMillis >= endAtMillis) {
            val nextPhaseStart = endAtMillis
            if (phase == Phase.WORK) {
                if (currentSet % 4 == 0 && currentSet < totalSets) {
                    phase = Phase.LONG_BREAK
                    phaseDurationMillis = longBreakMinutes * 60L * 1000L
                } else {
                    phase = Phase.SHORT_BREAK
                    phaseDurationMillis = SHORT_BREAK_MILLIS
                }
                endAtMillis = nextPhaseStart + phaseDurationMillis
            } else if (currentSet == totalSets) {
                phase = Phase.FINISHED
                // Keep the final phase duration so its empty minute blocks can still be shown.
                pausedRemainingMillis = 0L
                isFinished = true
            } else {
                currentSet++
                phase = Phase.WORK
                phaseDurationMillis = WORK_MILLIS
                endAtMillis = nextPhaseStart + phaseDurationMillis
            }
            changed = true
        }
        return changed
    }

    /**
     * タイマーを一時停止し、その時点の残り時間を保持します。
     *
     * @param nowMillis 一時停止時点の経過時間（ミリ秒）
     */
    fun pause(nowMillis: Long) {
        advance(nowMillis)
        if (!isFinished && !isPaused) {
            pausedRemainingMillis = (endAtMillis - nowMillis).coerceAtLeast(0L)
            isPaused = true
        }
    }

    /**
     * 一時停止時に保持した残り時間からタイマーを再開します。
     *
     * @param nowMillis 再開時点の経過時間（ミリ秒）
     */
    fun resume(nowMillis: Long) {
        if (isPaused && !isFinished) {
            endAtMillis = nowMillis + pausedRemainingMillis
            isPaused = false
        }
    }

    /**
     * 指定時点における現在フェーズの残り時間を取得します。
     *
     * @param nowMillis 残り時間を計算する時点の経過時間（ミリ秒）
     * @return 現在フェーズの残り時間（ミリ秒）
     */
    fun getRemainingMillis(nowMillis: Long): Long {
        if (isFinished) {
            return 0L
        }
        if (isPaused) {
            return pausedRemainingMillis
        }
        return (endAtMillis - nowMillis).coerceAtLeast(0L)
    }

    private companion object {
        const val WORK_MILLIS = 25L * 60L * 1000L
        const val SHORT_BREAK_MILLIS = 5L * 60L * 1000L
    }
}
