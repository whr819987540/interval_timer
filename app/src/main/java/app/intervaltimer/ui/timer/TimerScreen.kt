package app.intervaltimer.ui.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.intervaltimer.R
import app.intervaltimer.settings.VibrationMode
import app.intervaltimer.timer.TimerState
import app.intervaltimer.ui.alert.FinishedPanel
import app.intervaltimer.ui.components.CenteredScrollBox
import app.intervaltimer.ui.components.rememberNotificationAccess
import app.intervaltimer.ui.theme.Motion
import app.intervaltimer.ui.theme.Sizes
import app.intervaltimer.ui.theme.Spacing
import app.intervaltimer.ui.theme.rememberReducedMotion

@Composable
fun TimerRoute(onOpenSettings: () -> Unit, viewModel: TimerViewModel = viewModel(factory = TimerViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationAccess = rememberNotificationAccess(viewModel::consumeNotificationPrompt, viewModel::onNotificationAccessResult)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshSystemStatus() }

    TimerScreen(
        state = state,
        onEvent = { event ->
            // Ask for notification access the first time a countdown starts: the lock-screen
            // controls live in the notification.
            if (event == TimerEvent.Start && !state.notificationsEnabled) notificationAccess.promptOnce()
            viewModel.onEvent(event)
        },
        onEnableNotifications = notificationAccess::request,
        onOpenSettings = onOpenSettings,
    )
}

private enum class Phase { Setup, Active, Done }

private fun TimerState.phase(): Phase = when (this) {
    TimerState.Idle -> Phase.Setup
    is TimerState.Running, is TimerState.Paused -> Phase.Active
    is TimerState.Finished -> Phase.Done
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    state: TimerUiState,
    onEvent: (TimerEvent) -> Unit,
    onEnableNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!state.notificationsEnabled) {
                NotificationBanner(
                    onEnable = onEnableNotifications,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
            AnimatedContent(
                targetState = state.timer,
                contentKey = { it.phase() },
                transitionSpec = {
                    if (reducedMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(tween(Motion.MEDIUM)) togetherWith fadeOut(tween(Motion.FAST))
                    }
                },
                modifier = Modifier.weight(1f),
                label = "phase",
            ) { timer ->
                when (timer) {
                    TimerState.Idle -> SetupPane(state.entry, onEvent)
                    is TimerState.Running, is TimerState.Paused -> ActivePane(timer, onEvent)
                    is TimerState.Finished -> CenteredScrollBox(Modifier.fillMaxSize()) {
                        FinishedPanel(
                            state = timer,
                            continuousAlert = state.alert.mode == VibrationMode.CONTINUOUS,
                            onReset = { onEvent(TimerEvent.Restart) },
                            onSilence = { onEvent(TimerEvent.Silence) },
                            onStop = { onEvent(TimerEvent.Stop) },
                            modifier = Modifier
                                .padding(Spacing.xl)
                                .widthIn(max = Sizes.contentMaxWidth)
                                .fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationBanner(onEnable: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(painterResource(R.drawable.ic_error), contentDescription = null, modifier = Modifier.size(Sizes.iconMd))
            Text(
                text = stringResource(R.string.notifications_off_banner),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onEnable,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
            ) {
                Text(stringResource(R.string.enable))
            }
        }
    }
}
