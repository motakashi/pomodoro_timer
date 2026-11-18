package com.example.pomodorotimer

class TimerEngine {
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
                phaseDurationMillis = 0L
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

    fun pause(nowMillis: Long) {
        advance(nowMillis)
        if (!isFinished && !isPaused) {
            pausedRemainingMillis = (endAtMillis - nowMillis).coerceAtLeast(0L)
            isPaused = true
        }
    }

    fun resume(nowMillis: Long) {
        if (isPaused && !isFinished) {
            endAtMillis = nowMillis + pausedRemainingMillis
            isPaused = false
        }
    }

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
