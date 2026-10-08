package app.intervaltimer.ui.timer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.intervaltimer.R
import app.intervaltimer.timer.DurationDigits
import app.intervaltimer.timer.DurationFormat
import app.intervaltimer.ui.theme.Sizes
import app.intervaltimer.ui.theme.Spacing
import app.intervaltimer.ui.theme.TimerDigits

private val Presets = listOf(30_000L, 60_000L, 3 * 60_000L, 5 * 60_000L, 10 * 60_000L, 25 * 60_000L)
private val KeypadMaxWidth = 420.dp
private val KeypadMinHeight = 48.dp * 4 + Spacing.md * 3

/** Display, presets, start button and gaps in portrait; the keypad gets the rest of the height. */
private val PortraitReservedHeight = 300.dp

/** Duration entry: display, quick presets, keypad and the start button. */
@Composable
fun SetupPane(entry: DurationDigits, onEvent: (TimerEvent) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EntryDisplay(entry)
                    PresetGrid(entry, onEvent)
                    StartButton(enabled = entry.totalMs > 0, onClick = { onEvent(TimerEvent.Start) })
                }
                Keypad(
                    onEvent = onEvent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        } else {
            // Keypad and start button sit at the bottom, within thumb reach. The keypad takes what
            // is left after the other rows (capped), and the display is centred in the rest.
            val keypadHeight = (maxHeight - PortraitReservedHeight).coerceIn(KeypadMinHeight, Sizes.keypadMaxHeight)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EntryDisplay(entry, Modifier.padding(horizontal = Spacing.xl))
                    Spacer(Modifier.height(Spacing.lg))
                    PresetRow(entry, onEvent)
                }
                Keypad(
                    onEvent = onEvent,
                    modifier = Modifier
                        .padding(horizontal = Spacing.xl)
                        .height(keypadHeight)
                        .widthIn(max = KeypadMaxWidth)
                        .fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.xl))
                StartButton(
                    enabled = entry.totalMs > 0,
                    onClick = { onEvent(TimerEvent.Start) },
                    modifier = Modifier.padding(horizontal = Spacing.xl),
                )
            }
        }
    }
}

/** `00时05分00秒`; digits the user has typed are emphasised, placeholder zeros are muted. */
@Composable
private fun EntryDisplay(entry: DurationDigits, modifier: Modifier = Modifier) {
    val res = LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    val units = listOf(R.string.unit_hours, R.string.unit_minutes, R.string.unit_seconds).map { stringResource(it) }
    val firstTyped = DurationDigits.MAX_DIGITS - entry.digits.length
    val description = stringResource(R.string.entry_description, DurationFormat.label(res, entry.totalMs))
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Shrinks with the available width so large font scales still fit on one line.
        val digitSize = with(LocalDensity.current) { minOf(52.sp.toPx(), maxWidth.toPx() / 7f).toSp() }
        val text = buildAnnotatedString {
            entry.padded.forEachIndexed { index, digit ->
                withStyle(SpanStyle(color = if (index >= firstTyped) colors.onSurface else colors.onSurfaceVariant)) {
                    append(digit)
                }
                if (index % 2 == 1) {
                    withStyle(SpanStyle(fontSize = digitSize * 0.38f, color = colors.onSurfaceVariant)) {
                        append(units[index / 2])
                        if (index < DurationDigits.MAX_DIGITS - 1) append(' ')
                    }
                }
            }
        }
        Text(
            text = text,
            style = TimerDigits.copy(fontSize = digitSize),
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        )
    }
}

/** Single scrolling line that runs to the screen edges, so a cut-off chip reads as "scroll for more". */
@Composable
private fun PresetRow(entry: DurationDigits, onEvent: (TimerEvent) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Presets.forEach { ms -> PresetChip(ms, selected = entry.totalMs == ms, onEvent) }
    }
}

/** Wrapping grid for landscape, where the left column is too narrow for a scrolling line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetGrid(entry: DurationDigits, onEvent: (TimerEvent) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterHorizontally),
        maxItemsInEachRow = 3,
    ) {
        Presets.forEach { ms -> PresetChip(ms, selected = entry.totalMs == ms, onEvent) }
    }
}

@Composable
private fun PresetChip(ms: Long, selected: Boolean, onEvent: (TimerEvent) -> Unit) {
    val res = LocalContext.current.resources
    FilterChip(
        selected = selected,
        onClick = { onEvent(TimerEvent.Preset(ms)) },
        label = { Text(DurationFormat.label(res, ms)) },
    )
}

@Composable
private fun Keypad(onEvent: (TimerEvent) -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("00", "0", null))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                row.forEach { key ->
                    val keyModifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    if (key != null) {
                        KeypadKey(onClick = { onEvent(TimerEvent.Key(key)) }, modifier = keyModifier) {
                            Text(key, style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"))
                        }
                    } else {
                        KeypadKey(
                            onClick = { onEvent(TimerEvent.Backspace) },
                            onLongClick = { onEvent(TimerEvent.Clear) },
                            onLongClickLabel = stringResource(R.string.clear_all),
                            modifier = keyModifier,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_backspace),
                                contentDescription = stringResource(R.string.delete),
                                modifier = Modifier.size(Sizes.iconLg),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeypadKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(
                role = Role.Button,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun StartButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .widthIn(max = KeypadMaxWidth)
            .fillMaxWidth()
            .heightIn(min = Sizes.primaryAction),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(Sizes.iconMd))
        Spacer(Modifier.width(Spacing.sm))
        Text(stringResource(R.string.action_start), style = MaterialTheme.typography.titleLarge)
    }
}
