package app.intervaltimer.timer

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.edit

/**
 * Persists the session so it survives process death. Elapsed-realtime timestamps are only
 * meaningful within one boot, so a state saved before a reboot is discarded.
 */
class TimerStore(context: Context) {
    private val prefs = context.getSharedPreferences("timer_state", Context.MODE_PRIVATE)
    private val resolver = context.contentResolver

    fun load(): TimerState {
        if (prefs.getInt(KEY_BOOT_COUNT, UNKNOWN_BOOT) != bootCount()) return TimerState.Idle
        val total = prefs.getLong(KEY_TOTAL, 0)
        val round = prefs.getInt(KEY_ROUND, 1)
        if (total <= 0) return TimerState.Idle
        return when (prefs.getString(KEY_KIND, null)) {
            KIND_RUNNING -> {
                val end = prefs.getLong(KEY_END, 0)
                // Guards against devices without BOOT_COUNT: an end time further away than the
                // full duration can only come from a previous boot.
                if (end - SystemClock.elapsedRealtime() > total) {
                    TimerState.Idle
                } else {
                    TimerState.Running(total, end, round)
                }
            }
            KIND_PAUSED -> TimerState.Paused(total, prefs.getLong(KEY_REMAINING, total), round)
            KIND_FINISHED -> TimerState.Finished(total, round, prefs.getBoolean(KEY_ALERTING, false))
            else -> TimerState.Idle
        }
    }

    fun save(state: TimerState) {
        // commit() rather than apply(): the process may be killed right after a transition.
        prefs.edit(commit = true) {
            clear()
            putInt(KEY_BOOT_COUNT, bootCount())
            when (state) {
                TimerState.Idle -> Unit
                is TimerState.Running -> {
                    putString(KEY_KIND, KIND_RUNNING)
                    putLong(KEY_TOTAL, state.totalMs)
                    putLong(KEY_END, state.endAtElapsed)
                    putInt(KEY_ROUND, state.round)
                }
                is TimerState.Paused -> {
                    putString(KEY_KIND, KIND_PAUSED)
                    putLong(KEY_TOTAL, state.totalMs)
                    putLong(KEY_REMAINING, state.remainingMs)
                    putInt(KEY_ROUND, state.round)
                }
                is TimerState.Finished -> {
                    putString(KEY_KIND, KIND_FINISHED)
                    putLong(KEY_TOTAL, state.totalMs)
                    putInt(KEY_ROUND, state.round)
                    putBoolean(KEY_ALERTING, state.alerting)
                }
            }
        }
    }

    private fun bootCount(): Int =
        Settings.Global.getInt(resolver, Settings.Global.BOOT_COUNT, UNKNOWN_BOOT)

    private companion object {
        const val UNKNOWN_BOOT = -1
        const val KEY_BOOT_COUNT = "boot_count"
        const val KEY_KIND = "kind"
        const val KEY_TOTAL = "total_ms"
        const val KEY_END = "end_at_elapsed"
        const val KEY_REMAINING = "remaining_ms"
        const val KEY_ROUND = "round"
        const val KEY_ALERTING = "alerting"
        const val KIND_RUNNING = "running"
        const val KIND_PAUSED = "paused"
        const val KIND_FINISHED = "finished"
    }
}
