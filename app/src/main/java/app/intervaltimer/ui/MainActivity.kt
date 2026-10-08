package app.intervaltimer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.intervaltimer.appContainer
import app.intervaltimer.ui.settings.SettingsRoute
import app.intervaltimer.ui.theme.IntervalTimerTheme
import app.intervaltimer.ui.theme.Motion
import app.intervaltimer.ui.theme.rememberReducedMotion
import app.intervaltimer.ui.timer.TimerRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntervalTimerTheme {
                AppNavigation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Brings the service back if the system killed it while a session was active.
        appContainer.controller.ensureService()
    }
}

private enum class Screen { Timer, Settings }

@Composable
private fun AppNavigation() {
    var screen by rememberSaveable { mutableStateOf(Screen.Timer) }
    val reducedMotion = rememberReducedMotion()
    BackHandler(enabled = screen == Screen.Settings) { screen = Screen.Timer }
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            if (reducedMotion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                fadeIn(tween(Motion.MEDIUM)) togetherWith fadeOut(tween(Motion.FAST))
            }
        },
        label = "screen",
    ) { target ->
        when (target) {
            Screen.Timer -> TimerRoute(onOpenSettings = { screen = Screen.Settings })
            Screen.Settings -> SettingsRoute(onBack = { screen = Screen.Timer })
        }
    }
}
