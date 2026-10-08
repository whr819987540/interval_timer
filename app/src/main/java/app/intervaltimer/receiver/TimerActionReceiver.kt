package app.intervaltimer.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.intervaltimer.appContainer

/**
 * Handles notification buttons. Broadcast actions run without unlocking the device, which is what
 * makes resetting from the lock screen possible.
 */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val controller = context.appContainer.controller
        when (intent.action) {
            ACTION_PAUSE -> controller.pause()
            ACTION_RESUME -> controller.resume()
            ACTION_RESTART -> controller.restart()
            ACTION_SILENCE -> controller.silence()
            ACTION_STOP -> controller.stop()
        }
    }

    companion object {
        const val ACTION_PAUSE = "app.intervaltimer.action.PAUSE"
        const val ACTION_RESUME = "app.intervaltimer.action.RESUME"
        const val ACTION_RESTART = "app.intervaltimer.action.RESTART"
        const val ACTION_SILENCE = "app.intervaltimer.action.SILENCE"
        const val ACTION_STOP = "app.intervaltimer.action.STOP"

        private val requestCodes = mapOf(
            ACTION_PAUSE to 300,
            ACTION_RESUME to 301,
            ACTION_RESTART to 302,
            ACTION_SILENCE to 303,
            ACTION_STOP to 304,
        )

        fun pendingIntent(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodes.getValue(action),
            Intent(context, TimerActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
