package app.intervaltimer.ui.components

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis

/** `elapsedRealtime()` refreshed every frame while [ticking]; frozen otherwise. */
@Composable
fun rememberElapsedClock(ticking: Boolean): LongState {
    val clock = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(ticking) {
        clock.longValue = SystemClock.elapsedRealtime()
        while (ticking) {
            withFrameMillis { clock.longValue = SystemClock.elapsedRealtime() }
        }
    }
    return clock
}
