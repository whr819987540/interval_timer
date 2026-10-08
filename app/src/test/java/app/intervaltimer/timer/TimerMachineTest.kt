package app.intervaltimer.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class TimerMachineTest {
    private val total = 60_000L

    @Test
    fun `start begins round one`() {
        val state = TimerMachine.start(TimerState.Idle, total, now = 1_000)
        assertEquals(TimerState.Running(total, endAtElapsed = 61_000, round = 1), state)
    }

    @Test
    fun `start ignores zero duration`() {
        assertSame(TimerState.Idle, TimerMachine.start(TimerState.Idle, 0, now = 1_000))
    }

    @Test
    fun `pause then resume keeps remaining time`() {
        val running = TimerState.Running(total, endAtElapsed = 61_000, round = 2)
        val paused = TimerMachine.pause(running, now = 21_000)
        assertEquals(TimerState.Paused(total, remainingMs = 40_000, round = 2), paused)

        val resumed = TimerMachine.resume(paused, now = 100_000)
        assertEquals(TimerState.Running(total, endAtElapsed = 140_000, round = 2), resumed)
    }

    @Test
    fun `time up before deadline is ignored`() {
        val running = TimerState.Running(total, endAtElapsed = 61_000, round = 1)
        assertSame(running, TimerMachine.timeUp(running, now = 59_000))
    }

    @Test
    fun `time up within tolerance finishes and alerts`() {
        val running = TimerState.Running(total, endAtElapsed = 61_000, round = 3)
        val finished = TimerMachine.timeUp(running, now = 61_000 - TimerMachine.TIME_UP_TOLERANCE_MS)
        assertEquals(TimerState.Finished(total, round = 3, alerting = true), finished)
    }

    @Test
    fun `reset after time up starts next round with same duration`() {
        val finished = TimerState.Finished(total, round = 3, alerting = true)
        val next = TimerMachine.restart(finished, now = 5_000)
        assertEquals(TimerState.Running(total, endAtElapsed = 65_000, round = 4), next)
    }

    @Test
    fun `restart while running keeps the round`() {
        val running = TimerState.Running(total, endAtElapsed = 61_000, round = 2)
        assertEquals(TimerState.Running(total, endAtElapsed = 90_000, round = 2), TimerMachine.restart(running, now = 30_000))
        val paused = TimerState.Paused(total, remainingMs = 10_000, round = 2)
        assertEquals(TimerState.Running(total, endAtElapsed = 90_000, round = 2), TimerMachine.restart(paused, now = 30_000))
    }

    @Test
    fun `silence stops alerting but stays finished`() {
        val finished = TimerState.Finished(total, round = 1, alerting = true)
        assertEquals(finished.copy(alerting = false), TimerMachine.silence(finished))
        val running = TimerState.Running(total, 61_000, 1)
        assertSame(running, TimerMachine.silence(running))
    }

    @Test
    fun `stop always returns to idle`() {
        assertSame(TimerState.Idle, TimerMachine.stop(TimerState.Finished(total, 5, alerting = true)))
        assertSame(TimerState.Idle, TimerMachine.stop(TimerState.Running(total, 61_000, 1)))
    }

    @Test
    fun `remaining time is clamped`() {
        val running = TimerState.Running(total, endAtElapsed = 61_000, round = 1)
        assertEquals(0L, TimerMachine.remainingMs(running, now = 70_000))
        assertEquals(total, TimerMachine.remainingMs(running, now = 0))
        assertEquals(31_000L, TimerMachine.remainingMs(running, now = 30_000))
    }
}
