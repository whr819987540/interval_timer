package app.intervaltimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.intervaltimer.R
import app.intervaltimer.receiver.TimerActionReceiver
import app.intervaltimer.timer.DurationFormat
import app.intervaltimer.timer.TimerState
import app.intervaltimer.ui.AlarmActivity
import app.intervaltimer.ui.MainActivity

/**
 * Builds the foreground-service notification. Every action is a broadcast, so the buttons work
 * from the lock screen without unlocking the phone.
 */
class TimerNotifications(private val context: Context) {

    fun createChannels() {
        val running = NotificationChannel(
            CHANNEL_RUNNING,
            context.getString(R.string.channel_running),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_running_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        // Vibration and sound are played by Alerter so they follow the in-app settings.
        val alert = NotificationChannel(
            CHANNEL_ALERT,
            context.getString(R.string.channel_alert),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_alert_desc)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(running, alert))
    }

    /** Running/paused and finished use separate IDs so a new round re-alerts as a fresh post. */
    fun idFor(state: TimerState): Int = if (state is TimerState.Finished) ID_ALERT else ID_RUNNING

    fun build(state: TimerState): Notification =
        if (state is TimerState.Finished) finished(state) else countdown(state)

    private fun countdown(state: TimerState): Notification {
        val res = context.resources
        val builder = baseBuilder(CHANNEL_RUNNING)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setSilent(true)
            .setContentIntent(openApp())
        when (state) {
            is TimerState.Running -> builder
                .setContentTitle(res.getString(R.string.notif_running_title, state.round))
                .setContentText(res.getString(R.string.notif_total, DurationFormat.label(res, state.totalMs)))
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + (state.endAtElapsed - SystemClock.elapsedRealtime()))
                .addAction(R.drawable.ic_pause, res.getString(R.string.action_pause), action(TimerActionReceiver.ACTION_PAUSE))
            is TimerState.Paused -> builder
                .setContentTitle(res.getString(R.string.notif_paused_title, state.round))
                .setContentText(res.getString(R.string.notif_remaining, DurationFormat.clock(state.remainingMs)))
                .setShowWhen(false)
                .addAction(R.drawable.ic_play, res.getString(R.string.action_resume), action(TimerActionReceiver.ACTION_RESUME))
            else -> builder
                .setContentTitle(res.getString(R.string.app_name))
                .setShowWhen(false)
        }
        if (state !is TimerState.Idle) {
            builder
                .addAction(R.drawable.ic_replay, res.getString(R.string.action_restart_round), action(TimerActionReceiver.ACTION_RESTART))
                .addAction(R.drawable.ic_stop, res.getString(R.string.action_stop), action(TimerActionReceiver.ACTION_STOP))
        }
        return builder.build()
    }

    private fun finished(state: TimerState.Finished): Notification {
        val res = context.resources
        val alertScreen = PendingIntent.getActivity(
            context,
            REQUEST_ALERT_SCREEN,
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = baseBuilder(CHANNEL_ALERT)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentTitle(res.getString(R.string.notif_done_title, state.round))
            .setContentText(res.getString(R.string.notif_done_text, state.round + 1))
            .setShowWhen(false)
            .setContentIntent(alertScreen)
            .addAction(R.drawable.ic_replay, res.getString(R.string.action_reset), action(TimerActionReceiver.ACTION_RESTART))
        if (state.alerting) {
            // Lights up the screen with AlarmActivity when locked; a heads-up otherwise.
            builder
                .setFullScreenIntent(alertScreen, true)
                .addAction(R.drawable.ic_notifications_off, res.getString(R.string.action_silence), action(TimerActionReceiver.ACTION_SILENCE))
        }
        builder.addAction(R.drawable.ic_stop, res.getString(R.string.action_stop), action(TimerActionReceiver.ACTION_STOP))
        return builder.build()
    }

    private fun baseBuilder(channel: String) = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_stat_timer)
        .setColor(ContextCompat.getColor(context, R.color.brand_red))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN_APP,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(action: String): PendingIntent = TimerActionReceiver.pendingIntent(context, action)

    companion object {
        const val CHANNEL_RUNNING = "timer_running"
        const val CHANNEL_ALERT = "timer_alert"
        const val ID_RUNNING = 1
        const val ID_ALERT = 2
        private const val REQUEST_OPEN_APP = 200
        private const val REQUEST_ALERT_SCREEN = 201
    }
}
