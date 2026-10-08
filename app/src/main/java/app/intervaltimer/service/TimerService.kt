package app.intervaltimer.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.intervaltimer.appContainer
import app.intervaltimer.settings.VibrationMode
import app.intervaltimer.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps the process alive while a session is active and renders [TimerState] as a notification
 * and as the time-up alert. It never changes the state on its own except through the controller.
 */
class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container by lazy { appContainer }
    private var foregroundId = 0
    private var lastStartId = 0
    private var countdownJob: Job? = null
    private var autoSilenceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        scope.launch { container.controller.state.collect(::render) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        lastStartId = startId
        // Every startForegroundService() call must be answered with startForeground().
        val state = container.controller.state.value
        promote(state)
        if (state is TimerState.Idle) shutDown()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        stopAlert()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun render(state: TimerState) {
        countdownJob?.cancel()
        if (state is TimerState.Idle) {
            stopAlert()
            shutDown()
            return
        }
        promote(state)
        if (state is TimerState.Running) {
            // Backup for the AlarmManager alarm while the CPU is awake (e.g. exact alarms revoked).
            countdownJob = scope.launch {
                delay((state.endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0))
                container.controller.onTimeUp()
            }
        }
        if (state is TimerState.Finished && state.alerting) startAlert() else stopAlert()
    }

    private fun promote(state: TimerState) {
        val notifications = container.notifications
        val id = notifications.idFor(state)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(this, id, notifications.build(state), type)
        } catch (e: IllegalStateException) {
            // Android 12+ may refuse when the system restarts this sticky service in the
            // background. The exact alarm will start the service again when time is up.
            Log.w(TAG, "startForeground not allowed", e)
            shutDown()
            return
        }
        if (foregroundId != 0 && foregroundId != id) NotificationManagerCompat.from(this).cancel(foregroundId)
        foregroundId = id
    }

    private fun startAlert() {
        val alerter = container.alerter
        if (alerter.isActive) return
        val settings = container.settings.alert.value
        alerter.start(settings)
        if (settings.mode == VibrationMode.COUNT) {
            autoSilenceJob = scope.launch {
                delay(Alerter.durationMs(settings.vibrationCount))
                container.controller.silence()
            }
        }
    }

    private fun stopAlert() {
        autoSilenceJob?.cancel()
        autoSilenceJob = null
        container.alerter.stop()
    }

    private fun shutDown() {
        isRunning = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foregroundId = 0
        // Using the start id keeps the service alive if a newer start request is already queued.
        stopSelf(lastStartId)
    }

    companion object {
        private const val TAG = "TimerService"

        @Volatile
        var isRunning = false
            private set

        /**
         * Starts the service. With [force], a running service also re-posts its notification;
         * only use that from the foreground, where starting a foreground service is always allowed.
         */
        fun start(context: Context, force: Boolean = false) {
            if (isRunning && !force) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException: started from a context without a
                // background-start exemption. The alarm and notification still drive the session.
                Log.w(TAG, "Could not start foreground service", e)
            }
        }
    }
}
