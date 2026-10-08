package app.intervaltimer.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.intervaltimer.R
import app.intervaltimer.timer.DurationFormat
import app.intervaltimer.timer.TimerMachine
import app.intervaltimer.timer.TimerState
import app.intervaltimer.ui.components.LabeledIconButton
import app.intervaltimer.ui.components.ProgressRing
import app.intervaltimer.ui.components.rememberElapsedClock
import app.intervaltimer.ui.theme.Sizes
import app.intervaltimer.ui.theme.Spacing
import app.intervaltimer.ui.theme.TimerDigits

private val ControlsHeight = 130.dp
private val RoundBadgeHeight = 40.dp

/** Countdown in progress (running or paused): ring, digits and controls. */
@Composable
fun ActivePane(timer: TimerState, onEvent: (TimerEvent) -> Unit, modifier: Modifier = Modifier) {
    val running = timer is TimerState.Running
    val round = when (timer) {
        is TimerState.Running -> timer.round
        is TimerState.Paused -> timer.round
        else -> 1
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            val ringSize = minOf(maxHeight - Spacing.xxl, maxWidth * 0.5f, Sizes.ringMax)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.xl),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountdownRing(timer, ringSize)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                ) {
                    RoundBadge(round)
                    Controls(running, onEvent)
                }
            }
        } else {
            val reserved = RoundBadgeHeight + ControlsHeight + Spacing.xxxl * 2
            val ringSize = minOf(maxWidth - Spacing.xxxl, maxHeight - reserved, Sizes.ringMax).coerceAtLeast(160.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                RoundBadge(round)
                CountdownRing(timer, ringSize)
                Controls(running, onEvent)
            }
        }
    }
}

@Composable
private fun CountdownRing(timer: TimerState, size: Dp) {
    val running = timer is TimerState.Running
    val total = when (timer) {
        is TimerState.Running -> timer.totalMs
        is TimerState.Paused -> timer.totalMs
        else -> 1L
    }.coerceAtLeast(1L)
    val clock = rememberElapsedClock(ticking = running)
    // Recomposes once per second; the ring itself redraws every frame without recomposing.
    val secondsLeft by remember(timer) {
        derivedStateOf { DurationFormat.ceilSeconds(TimerMachine.remainingMs(timer, clock.longValue)) }
    }
    val res = LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    val clockText = DurationFormat.clock(secondsLeft * 1000)
    val remainingDescription = stringResource(R.string.remaining_description, DurationFormat.label(res, secondsLeft * 1000))

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        ProgressRing(
            progress = { TimerMachine.remainingMs(timer, clock.longValue).toFloat() / total },
            color = if (running) colors.primary else colors.onSurfaceVariant,
            trackColor = colors.outlineVariant,
            modifier = Modifier.fillMaxSize(),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val digitSize = with(LocalDensity.current) { (size * if (clockText.length > 5) 0.17f else 0.23f).toSp() }
            Text(
                text = clockText,
                style = TimerDigits.copy(fontSize = digitSize),
                color = colors.onBackground,
                maxLines = 1,
                modifier = Modifier.semantics { contentDescription = remainingDescription },
            )
            Text(
                text = if (running) {
                    stringResource(R.string.total_label, DurationFormat.label(res, total))
                } else {
                    stringResource(R.string.paused)
                },
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RoundBadge(round: Int) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(
            text = stringResource(R.string.round_label, round),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }
}

@Composable
private fun Controls(running: Boolean, onEvent: (TimerEvent) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
        verticalAlignment = Alignment.Top,
    ) {
        LabeledIconButton(
            icon = R.drawable.ic_replay,
            label = stringResource(R.string.action_restart_round),
            onClick = { onEvent(TimerEvent.Restart) },
            modifier = Modifier.padding(top = Spacing.md),
        )
        LabeledIconButton(
            icon = if (running) R.drawable.ic_pause else R.drawable.ic_play,
            label = stringResource(if (running) R.string.action_pause else R.string.action_resume),
            onClick = { onEvent(if (running) TimerEvent.Pause else TimerEvent.Resume) },
            emphasized = true,
        )
        LabeledIconButton(
            icon = R.drawable.ic_stop,
            label = stringResource(R.string.action_stop),
            onClick = { onEvent(TimerEvent.Stop) },
            modifier = Modifier.padding(top = Spacing.md),
        )
    }
}
