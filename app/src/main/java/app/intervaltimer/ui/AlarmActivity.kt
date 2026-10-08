package app.intervaltimer.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.intervaltimer.appContainer
import app.intervaltimer.settings.VibrationMode
import app.intervaltimer.timer.TimerState
import app.intervaltimer.ui.alert.AlertScreen
import app.intervaltimer.ui.theme.IntervalTimerTheme

/**
 * Full-screen "time is up" screen launched by the alert notification. It shows over the keyguard,
 * so the user can reset without unlocking. It closes itself once the timer leaves Finished.
 */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        val controller = appContainer.controller
        val settings = appContainer.settings
        setContent {
            IntervalTimerTheme {
                val state by controller.state.collectAsStateWithLifecycle()
                val alert by settings.alert.collectAsStateWithLifecycle()
                val finished = state as? TimerState.Finished
                LaunchedEffect(finished == null) {
                    if (finished == null) finish()
                }
                if (finished != null) {
                    AlertScreen(
                        state = finished,
                        continuousAlert = alert.mode == VibrationMode.CONTINUOUS,
                        onReset = controller::restart,
                        onSilence = controller::silence,
                        onStop = controller::stop,
                    )
                }
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
