package app.intervaltimer.timer

import android.content.Context
import android.os.SystemClock
import app.intervaltimer.service.TimerService
import app.intervaltimer.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single entry point for changing the timer. UI, notification actions, the alarm receiver and the
 * service all call into this class; [TimerService] observes [state] and renders it.
 */
class TimerController(
    private val context: Context,
    private val store: TimerStore,
    private val scheduler: TimerAlarmScheduler,
    private val settings: SettingsRepository,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    fun start(totalMs: Long) {
        if (totalMs <= 0) return
        settings.lastDurationMs = totalMs
        transition { TimerMachine.start(it, totalMs, clock()) }
    }

    fun pause() = transition { TimerMachine.pause(it, clock()) }

    fun resume() = transition { TimerMachine.resume(it, clock()) }

    fun restart() = transition { TimerMachine.restart(it, clock()) }

    fun stop() = transition { TimerMachine.stop(it) }

    fun silence() = transition { TimerMachine.silence(it) }

    fun onTimeUp() = transition { TimerMachine.timeUp(it, clock()) }

    /** AlarmManager callback. Re-arms when it fires early (RTC vs. elapsed-time skew). */
    fun onAlarmFired() {
        val current = _state.value
        if (current is TimerState.Running && !TimerMachine.isDue(current, clock())) {
            scheduler.schedule(current)
        } else {
            onTimeUp()
        }
    }

    /**
     * For an active session, starts the foreground service (e.g. after the process was killed) or
     * makes it re-post its notification (e.g. after notification access was granted). Call only
     * while an activity is visible.
     */
    fun ensureService() {
        if (_state.value !is TimerState.Idle) TimerService.start(context, force = true)
    }

    @Synchronized
    private fun transition(reduce: (TimerState) -> TimerState) {
        val old = _state.value
        val new = reduce(old)
        if (new == old) return
        _state.value = new
        store.save(new)
        if (new is TimerState.Running) scheduler.schedule(new) else scheduler.cancel()
        if (new !is TimerState.Idle) TimerService.start(context)
    }
}
