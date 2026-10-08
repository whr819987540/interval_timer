package app.intervaltimer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.intervaltimer.appContainer

/** Fired by AlarmManager when the countdown ends; may cold-start the process. */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.appContainer.controller.onAlarmFired()
    }
}
