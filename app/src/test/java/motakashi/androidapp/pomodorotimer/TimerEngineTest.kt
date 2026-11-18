package motakashi.androidapp.pomodorotimer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {
    @Test
    fun `4セット目終了後にまだセットが残っている場合は長い休憩に入る`() {
        val engine = TimerEngine()
        engine.start(5, 20, 0L)

        var now = 0L
        for (set in 1..3) {
            now += 25L * MINUTE
            engine.advance(now)
            assertEquals(TimerEngine.Phase.SHORT_BREAK, engine.phase)
            now += 5L * MINUTE
            engine.advance(now)
        }

        now += 25L * MINUTE
        engine.advance(now)
        assertEquals(TimerEngine.Phase.LONG_BREAK, engine.phase)
        assertEquals(20L * MINUTE, engine.getRemainingMillis(now))
    }

    @Test
    fun `合計4セットの場合は4セット目の短い休憩終了後に終了する`() {
        val engine = TimerEngine()
        engine.start(4, 20, 0L)

        val fourthSetEnd = 4L * 30L * MINUTE - 5L * MINUTE
        engine.advance(fourthSetEnd)
        assertEquals(4, engine.currentSet)
        assertEquals(TimerEngine.Phase.SHORT_BREAK, engine.phase)

        engine.advance(fourthSetEnd + 5L * MINUTE)
        assertEquals(TimerEngine.Phase.FINISHED, engine.phase)
        assertTrue(engine.isFinished)
    }

    @Test
    fun `一時停止と再開後も残り時間が保持される`() {
        val engine = TimerEngine()
        engine.start(2, 20, 0L)
        engine.pause(10L * MINUTE)

        assertTrue(engine.isPaused)
        assertEquals(15L * MINUTE, engine.getRemainingMillis(10L * MINUTE))

        engine.resume(60L * MINUTE)
        assertFalse(engine.isPaused)
        assertEquals(15L * MINUTE, engine.getRemainingMillis(60L * MINUTE))
        engine.advance(75L * MINUTE)
        assertEquals(TimerEngine.Phase.SHORT_BREAK, engine.phase)
    }

    @Test
    fun `大幅に遅延したTickでも複数のフェーズをまたいで追いつくことができる`() {
        val engine = TimerEngine()
        engine.start(5, 20, 0L)

        assertTrue(engine.advance(135L * MINUTE))
        assertEquals(5, engine.currentSet)
        assertEquals(TimerEngine.Phase.WORK, engine.phase)
        assertEquals(25L * MINUTE, engine.getRemainingMillis(135L * MINUTE))
    }

    @Test
    fun `一時停止中はadvanceを実行してもフェーズが遷移せずfalseを返す`() {
        val engine = TimerEngine()
        engine.start(1, 20, 0L)
        engine.pause(10L * MINUTE)

        assertTrue(engine.isPaused)
        val initialPhase = engine.phase
        val result = engine.advance(30L * MINUTE)

        assertFalse(result)
        assertEquals(initialPhase, engine.phase)
    }

    @Test
    fun `終了後はadvanceを実行してもフェーズが遷移せずfalseを返す`() {
        val engine = TimerEngine()
        engine.start(1, 20, 0L)
        engine.advance(30L * MINUTE)

        assertTrue(engine.isFinished)
        val result = engine.advance(60L * MINUTE)

        assertFalse(result)
        assertEquals(TimerEngine.Phase.FINISHED, engine.phase)
    }

    @Test
    fun `終了後はgetRemainingMillisを実行すると0を返す`() {
        val engine = TimerEngine()
        engine.start(1, 20, 0L)
        engine.advance(30L * MINUTE)

        assertTrue(engine.isFinished)
        assertEquals(0L, engine.getRemainingMillis(30L * MINUTE))
        assertEquals(0L, engine.getRemainingMillis(60L * MINUTE))
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
