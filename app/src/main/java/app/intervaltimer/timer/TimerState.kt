package app.intervaltimer.timer

/**
 * One countdown session. A session starts at round 1; each reset after time is up starts the
 * next round with the same duration. All timestamps are `SystemClock.elapsedRealtime()` values.
 */
sealed interface TimerState {
    data object Idle : TimerState

    data class Running(val totalMs: Long, val endAtElapsed: Long, val round: Int) : TimerState

    data class Paused(val totalMs: Long, val remainingMs: Long, val round: Int) : TimerState

    /** Time is up. [alerting] is true while vibration/sound is active. */
    data class Finished(val totalMs: Long, val round: Int, val alerting: Boolean) : TimerState
}

/**
 * Pure state transitions. Each function returns the state unchanged when the transition does not
 * apply, so callers can fire events without checking the current state first.
 */
object TimerMachine {
    /** An alarm that fires this close to the deadline counts as on time. */
    const val TIME_UP_TOLERANCE_MS = 500L

    fun start(state: TimerState, totalMs: Long, now: Long): TimerState =
        if (totalMs <= 0) state else TimerState.Running(totalMs, now + totalMs, round = 1)

    fun pause(state: TimerState, now: Long): TimerState =
        if (state is TimerState.Running) {
            TimerState.Paused(state.totalMs, remainingMs(state, now), state.round)
        } else {
            state
        }

    fun resume(state: TimerState, now: Long): TimerState =
        if (state is TimerState.Paused) {
            TimerState.Running(state.totalMs, now + state.remainingMs, state.round)
        } else {
            state
        }

    /** Restarts the countdown at full length; after time is up this begins the next round. */
    fun restart(state: TimerState, now: Long): TimerState = when (state) {
        is TimerState.Running -> TimerState.Running(state.totalMs, now + state.totalMs, state.round)
        is TimerState.Paused -> TimerState.Running(state.totalMs, now + state.totalMs, state.round)
        is TimerState.Finished -> TimerState.Running(state.totalMs, now + state.totalMs, state.round + 1)
        TimerState.Idle -> state
    }

    fun timeUp(state: TimerState, now: Long): TimerState =
        if (state is TimerState.Running && isDue(state, now)) {
            TimerState.Finished(state.totalMs, state.round, alerting = true)
        } else {
            state
        }

    fun silence(state: TimerState): TimerState =
        if (state is TimerState.Finished && state.alerting) state.copy(alerting = false) else state

    @Suppress("UNUSED_PARAMETER")
    fun stop(state: TimerState): TimerState = TimerState.Idle

    fun isDue(state: TimerState.Running, now: Long): Boolean =
        now >= state.endAtElapsed - TIME_UP_TOLERANCE_MS

    fun remainingMs(state: TimerState, now: Long): Long = when (state) {
        is TimerState.Running -> (state.endAtElapsed - now).coerceIn(0, state.totalMs)
        is TimerState.Paused -> state.remainingMs
        is TimerState.Finished, TimerState.Idle -> 0
    }
}
