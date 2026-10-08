package app.intervaltimer.ui.alert

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.intervaltimer.timer.TimerState
import app.intervaltimer.ui.components.CenteredScrollBox
import app.intervaltimer.ui.theme.Sizes
import app.intervaltimer.ui.theme.Spacing

/** Full-screen content of the lock-screen alert. */
@Composable
fun AlertScreen(
    state: TimerState.Finished,
    continuousAlert: Boolean,
    onReset: () -> Unit,
    onSilence: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        CenteredScrollBox(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            FinishedPanel(
                state = state,
                continuousAlert = continuousAlert,
                onReset = onReset,
                onSilence = onSilence,
                onStop = onStop,
                modifier = Modifier
                    .padding(Spacing.xl)
                    .widthIn(max = Sizes.contentMaxWidth)
                    .fillMaxWidth(),
            )
        }
    }
}
