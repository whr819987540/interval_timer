package app.intervaltimer.ui.alert

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.intervaltimer.R
import app.intervaltimer.timer.DurationFormat
import app.intervaltimer.timer.TimerState
import app.intervaltimer.ui.theme.Motion
import app.intervaltimer.ui.theme.Sizes
import app.intervaltimer.ui.theme.Spacing
import app.intervaltimer.ui.theme.rememberReducedMotion

/** "Time is up" content, shared by the main screen and the lock-screen [AlarmActivity][app.intervaltimer.ui.AlarmActivity]. */
@Composable
fun FinishedPanel(
    state: TimerState.Finished,
    continuousAlert: Boolean,
    onReset: () -> Unit,
    onSilence: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalContext.current.resources
    val pulseEnabled = state.alerting && !rememberReducedMotion()
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(Motion.PULSE, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.heroBadge)
                .graphicsLayer {
                    val scale = if (pulseEnabled) pulseScale else 1f
                    scaleX = scale
                    scaleY = scale
                }
                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_alarm),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(Sizes.iconHero),
            )
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(R.string.time_up),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.round_done, state.round, DurationFormat.label(res, state.totalMs)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (state.alerting) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(if (continuousAlert) R.string.alerting_continuous else R.string.alerting_count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
        Button(
            onClick = onReset,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.heroAction),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(painterResource(R.drawable.ic_replay), contentDescription = null, modifier = Modifier.size(Sizes.iconLg))
            Spacer(Modifier.width(Spacing.md))
            Text(stringResource(R.string.action_reset), style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.next_round_hint, state.round + 1),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.alerting) {
                OutlinedButton(onClick = onSilence, modifier = Modifier.heightIn(min = Sizes.secondaryAction)) {
                    Icon(painterResource(R.drawable.ic_notifications_off), contentDescription = null, modifier = Modifier.size(Sizes.iconSm))
                    Spacer(Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.action_silence))
                }
            }
            TextButton(onClick = onStop, modifier = Modifier.heightIn(min = Sizes.secondaryAction)) {
                Text(stringResource(R.string.action_stop_timer))
            }
        }
    }
}
