package app.intervaltimer

import android.app.Application
import android.content.Context
import app.intervaltimer.service.Alerter
import app.intervaltimer.service.TimerNotifications
import app.intervaltimer.settings.SettingsRepository
import app.intervaltimer.timer.TimerAlarmScheduler
import app.intervaltimer.timer.TimerController
import app.intervaltimer.timer.TimerStore

/** Process-wide singletons, wired by hand. */
class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
    val alerter = Alerter(context)
    val notifications = TimerNotifications(context)
    val controller = TimerController(context, TimerStore(context), TimerAlarmScheduler(context), settings)
}

class IntervalTimerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.createChannels()
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as IntervalTimerApp).container
