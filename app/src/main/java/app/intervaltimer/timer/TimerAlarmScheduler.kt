package app.intervaltimer.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import app.intervaltimer.receiver.TimerAlarmReceiver
import app.intervaltimer.ui.MainActivity

/**
 * Wakes the device when time is up, even in Doze and even if the process has been killed.
 * `setAlarmClock` is used because it is exempt from Doze throttling, which matters for short
 * intervals that repeat many times.
 */
class TimerAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(state: TimerState.Running) {
        val operation = alarmIntent()
        if (canScheduleExact()) {
            val triggerAtWall = System.currentTimeMillis() + (state.endAtElapsed - SystemClock.elapsedRealtime())
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtWall, showIntent()), operation)
        } else {
            // Exact alarms revoked by the user (Android 12+): fall back to an inexact alarm. The
            // foreground service also keeps an in-process timer while the CPU is awake.
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, state.endAtElapsed, operation)
        }
    }

    fun cancel() {
        alarmManager.cancel(alarmIntent())
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_ALARM,
        Intent(context, TimerAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_SHOW,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_ALARM = 100
        const val REQUEST_SHOW = 101
    }
}
