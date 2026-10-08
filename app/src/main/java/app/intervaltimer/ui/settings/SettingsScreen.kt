package app.intervaltimer.ui.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.intervaltimer.BuildConfig
import app.intervaltimer.R
import app.intervaltimer.settings.AlertSettings
import app.intervaltimer.settings.VibrationMode
import app.intervaltimer.system.SystemChecks
import app.intervaltimer.system.SystemStatus
import app.intervaltimer.system.openSystemSettings
import app.intervaltimer.ui.components.rememberNotificationAccess
import app.intervaltimer.ui.theme.Sizes
import app.intervaltimer.ui.theme.Spacing

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationAccess = rememberNotificationAccess(viewModel::consumeNotificationPrompt, viewModel::onNotificationAccessResult)
    // Statuses change in system settings; re-read them whenever the user comes back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshStatus() }

    SettingsScreen(
        state = state,
        onBack = onBack,
        onModeChange = viewModel::setMode,
        onCountChange = viewModel::changeCount,
        onSoundChange = viewModel::setSound,
        onPreview = viewModel::preview,
        onFixNotifications = notificationAccess::request,
        onFixLockScreen = { context.openSystemSettings(SystemChecks.notificationSettings(context)) },
        onFixExactAlarms = { context.openSystemSettings(SystemChecks.exactAlarmSettings(context)) },
        onFixFullScreen = { context.openSystemSettings(SystemChecks.fullScreenSettings(context)) },
        onFixBattery = { context.openSystemSettings(SystemChecks.batteryOptimizationRequest(context)) },
        onOpenAppDetails = { context.openSystemSettings(SystemChecks.appDetails(context)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onModeChange: (VibrationMode) -> Unit,
    onCountChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onFixNotifications: () -> Unit,
    onFixLockScreen: () -> Unit,
    onFixExactAlarms: () -> Unit,
    onFixFullScreen: () -> Unit,
    onFixBattery: () -> Unit,
    onOpenAppDetails: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.lg,
                end = Spacing.lg,
                top = padding.calculateTopPadding() + Spacing.sm,
                bottom = padding.calculateBottomPadding() + Spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "alert_header") { SectionHeader(R.string.section_alert) }
            item(key = "alert_card") {
                AlertCard(state.alert, onModeChange, onCountChange, onSoundChange, onPreview)
            }
            item(key = "background_header") { SectionHeader(R.string.section_background) }
            item(key = "background_intro") {
                Text(
                    text = stringResource(R.string.background_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.contentWidth(),
                )
            }
            item(key = "background_card") {
                BackgroundCard(state.status, onFixNotifications, onFixLockScreen, onFixExactAlarms, onFixFullScreen, onFixBattery)
            }
            item(key = "vendor_card") { VendorCard(onOpenAppDetails) }
            item(key = "version") {
                Text(
                    text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .contentWidth()
                        .padding(top = Spacing.lg),
                )
            }
        }
    }
}

private fun Modifier.contentWidth(): Modifier = widthIn(max = Sizes.contentMaxWidth).fillMaxWidth()

@Composable
private fun SectionHeader(title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .contentWidth()
            .padding(top = Spacing.lg, start = Spacing.xs)
            .semantics { heading() },
    )
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.contentWidth(),
    ) {
        Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertCard(
    alert: AlertSettings,
    onModeChange: (VibrationMode) -> Unit,
    onCountChange: (Int) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onPreview: () -> Unit,
) {
    SettingsCard {
        Text(
            text = stringResource(R.string.alert_mode),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.md),
        )
        val modes = listOf(VibrationMode.COUNT to R.string.mode_count, VibrationMode.CONTINUOUS to R.string.mode_continuous)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = alert.mode == mode,
                    onClick = { onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                ) {
                    Text(stringResource(label))
                }
            }
        }
        Text(
            text = stringResource(if (alert.mode == VibrationMode.COUNT) R.string.mode_count_desc else R.string.mode_continuous_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.md),
        )
        if (alert.mode == VibrationMode.COUNT) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CountStepper(alert.vibrationCount, onCountChange)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .toggleable(value = alert.sound, role = Role.Switch, onValueChange = onSoundChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.sound), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.sound_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.lg))
            // The whole row is the toggle; the switch only mirrors its state.
            Switch(checked = alert.sound, onCheckedChange = null)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.preview_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.lg))
            OutlinedButton(onClick = onPreview) { Text(stringResource(R.string.preview)) }
        }
    }
}

@Composable
private fun CountStepper(count: Int, onCountChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.vibration_count), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onCountChange(-1) }, enabled = count > AlertSettings.MIN_COUNT) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.decrease))
        }
        Text(
            text = stringResource(R.string.count_value, count),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 64.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        FilledTonalIconButton(onClick = { onCountChange(1) }, enabled = count < AlertSettings.MAX_COUNT) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.increase))
        }
    }
}

@Composable
private fun BackgroundCard(
    status: SystemStatus,
    onFixNotifications: () -> Unit,
    onFixLockScreen: () -> Unit,
    onFixExactAlarms: () -> Unit,
    onFixFullScreen: () -> Unit,
    onFixBattery: () -> Unit,
) {
    SettingsCard {
        StatusRow(R.string.status_notifications, R.string.status_notifications_desc, status.notifications, onFixNotifications)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StatusRow(R.string.status_lock_screen, R.string.status_lock_screen_desc, status.lockScreenNotifications, onFixLockScreen)
        if (SystemChecks.hasFullScreenSetting) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatusRow(R.string.status_full_screen, R.string.status_full_screen_desc, status.fullScreenAlerts, onFixFullScreen)
        }
        if (SystemChecks.hasExactAlarmSetting) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatusRow(R.string.status_exact_alarm, R.string.status_exact_alarm_desc, status.exactAlarms, onFixExactAlarms)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StatusRow(R.string.status_battery, R.string.status_battery_desc, status.batteryUnrestricted, onFixBattery)
    }
}

/** Icon, wording and colour all change with [ok], so state is never conveyed by colour alone. */
@Composable
private fun StatusRow(title: Int, description: Int, ok: Boolean, onFix: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusIcon(if (ok) R.drawable.ic_check_circle else R.drawable.ic_error, ok)
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(if (ok) R.string.status_on else R.string.status_off),
                style = MaterialTheme.typography.labelMedium,
                color = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
        }
        if (!ok) {
            Spacer(Modifier.width(Spacing.md))
            FilledTonalButton(onClick = onFix) { Text(stringResource(R.string.fix)) }
        }
    }
}

@Composable
private fun StatusIcon(@DrawableRes icon: Int, ok: Boolean) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = if (ok) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(Sizes.iconMd),
    )
}

@Composable
private fun VendorCard(onOpenAppDetails: () -> Unit) {
    SettingsCard {
        Row(
            modifier = Modifier.padding(top = Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.iconMd),
            )
            Spacer(Modifier.width(Spacing.lg))
            Text(
                text = stringResource(R.string.vendor_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onOpenAppDetails, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.open_app_details))
        }
    }
}
