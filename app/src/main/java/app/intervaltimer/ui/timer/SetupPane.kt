package app.intervaltimer.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.intervaltimer.R
import app.intervaltimer.timer.DurationEntry
import app.intervaltimer.timer.DurationFormat
import app.intervaltimer.timer.TimeField
import app.intervaltimer.ui.theme.Sizes
import app.intervaltimer.ui.theme.Spacing
import app.intervaltimer.ui.theme.TimerDigits

private val Presets = listOf(30_000L, 60_000L, 3 * 60_000L, 5 * 60_000L, 10 * 60_000L, 25 * 60_000L)
private val KeypadMaxWidth = 420.dp
private val KeypadMinHeight = 48.dp * 4 + Spacing.md * 3
private val FieldMaxWidth = 120.dp

/** Fields, hint, presets, start button and gaps in portrait; the keypad gets the rest of the height. */
private val PortraitReservedHeight = 330.dp

/** Duration entry: tap a field (时/分/秒), type its value on the keypad, then start. */
@Composable
fun SetupPane(entry: DurationEntry, onEvent: (TimerEvent) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val start = { onEvent(TimerEvent.Start) }
        val portraitKeypadHeight = (maxHeight - PortraitReservedHeight).coerceIn(KeypadMinHeight, Sizes.keypadMaxHeight)
        when {
            maxWidth > maxHeight -> Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DurationFields(entry, onEvent)
                    PresetGrid(entry, onEvent)
                    StartButton(enabled = entry.totalMs > 0, onClick = start)
                }
                Keypad(
                    onEvent = onEvent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }

            // Too short for the fixed layout (small phone with the notification banner, large
            // fonts): fields and keypad scroll at minimum size; the start button stays visible.
            maxHeight < PortraitReservedHeight + KeypadMinHeight -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PortraitHeader(entry, onEvent)
                    Spacer(Modifier.height(Spacing.lg))
                    PortraitKeypad(onEvent, KeypadMinHeight)
                }
                Spacer(Modifier.height(Spacing.sm))
                StartButton(entry.totalMs > 0, start, Modifier.padding(horizontal = Spacing.xl))
            }

            // Keypad and start button sit at the bottom, within thumb reach. The keypad takes what
            // is left after the other rows (capped), and the fields are centred in the rest.
            else -> Column(
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
                ) {
                    PortraitHeader(entry, onEvent)
                }
                Spacer(Modifier.height(Spacing.lg))
                PortraitKeypad(onEvent, portraitKeypadHeight)
                Spacer(Modifier.height(Spacing.xl))
                StartButton(entry.totalMs > 0, start, Modifier.padding(horizontal = Spacing.xl))
            }
        }
    }
}

@Composable
private fun PortraitHeader(entry: DurationEntry, onEvent: (TimerEvent) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DurationFields(entry, onEvent, Modifier.padding(horizontal = Spacing.xl))
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.entry_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.xl),
        )
        Spacer(Modifier.height(Spacing.lg))
        PresetRow(entry, onEvent)
    }
}

@Composable
private fun PortraitKeypad(onEvent: (TimerEvent) -> Unit, height: Dp) {
    Keypad(
        onEvent = onEvent,
        modifier = Modifier
            .padding(horizontal = Spacing.xl)
            .height(height)
            .widthIn(max = KeypadMaxWidth)
            .fillMaxWidth(),
    )
}

private val TimeField.unitRes: Int
    get() = when (this) {
        TimeField.HOURS -> R.string.unit_hours
        TimeField.MINUTES -> R.string.unit_minutes
        TimeField.SECONDS -> R.string.unit_seconds
    }

private val TimeField.descriptionRes: Int
    get() = when (this) {
        TimeField.HOURS -> R.string.duration_hours
        TimeField.MINUTES -> R.string.duration_minutes
        TimeField.SECONDS -> R.string.duration_seconds
    }

/** Three tappable fields; the selected one receives keypad input. */
@Composable
private fun DurationFields(entry: DurationEntry, onEvent: (TimerEvent) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fieldWidth = ((maxWidth - Spacing.sm * 2) / 3).coerceAtMost(FieldMaxWidth)
        // Sized from the field width so large font scales still fit two digits.
        val digitSize = with(LocalDensity.current) { minOf(48.sp.toPx(), (fieldWidth * 0.44f).toPx()).toSp() }
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TimeField.entries.forEach { field ->
                DurationField(
                    field = field,
                    entry = entry,
                    digitSize = digitSize,
                    onSelect = { onEvent(TimerEvent.SelectField(field)) },
                    modifier = Modifier.width(fieldWidth),
                )
            }
        }
    }
}

/**
 * Selected: tinted fill and a thick primary outline (not colour alone). While the first of two
 * digits is being typed, the leading zero is muted to show a second digit is expected.
 */
@Composable
private fun DurationField(
    field: TimeField,
    entry: DurationEntry,
    digitSize: TextUnit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val selected = entry.selected == field
    val value = entry.valueOf(field)
    val partial = selected && entry.typedDigits == 1
    val description = stringResource(field.descriptionRes, value)
    val digits = buildAnnotatedString {
        val text = value.toString().padStart(2, '0')
        withStyle(SpanStyle(color = if (partial) colors.onSurfaceVariant else colors.onSurface)) { append(text[0]) }
        withStyle(SpanStyle(color = colors.onSurface)) { append(text[1]) }
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.secondaryContainer else Color.Transparent)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = shape,
            )
            .selectable(selected = selected, role = Role.Tab, onClick = onSelect)
            .semantics { contentDescription = description }
            .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = digits,
            style = TimerDigits.copy(fontSize = digitSize),
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(
            text = stringResource(field.unitRes),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** Single scrolling line that runs to the screen edges, so a cut-off chip reads as "scroll for more". */
@Composable
private fun PresetRow(entry: DurationEntry, onEvent: (TimerEvent) -> Unit) {
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
private fun PresetGrid(entry: DurationEntry, onEvent: (TimerEvent) -> Unit) {
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

/** Digits 1–9 and 0, plus "clear field" and "delete digit"; all act on the selected field. */
@Composable
private fun Keypad(onEvent: (TimerEvent) -> Unit, modifier: Modifier = Modifier) {
    val digitStyle = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            KeypadRow {
                row.forEach { digit ->
                    KeypadKey(onClick = { onEvent(TimerEvent.Digit(digit)) }) { Text(digit.toString(), style = digitStyle) }
                }
            }
        }
        KeypadRow {
            KeypadKey(
                onClick = { onEvent(TimerEvent.ClearField) },
                contentDescription = stringResource(R.string.clear_field_description),
            ) {
                Text(
                    text = stringResource(R.string.clear_field),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
            KeypadKey(onClick = { onEvent(TimerEvent.Digit(0)) }) { Text("0", style = digitStyle) }
            KeypadKey(onClick = { onEvent(TimerEvent.Backspace) }, contentDescription = stringResource(R.string.delete)) {
                Icon(painterResource(R.drawable.ic_backspace), contentDescription = null, modifier = Modifier.size(Sizes.iconLg))
            }
        }
    }
}

@Composable
private fun ColumnScope.KeypadRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        content = content,
    )
}

@Composable
private fun RowScope.KeypadKey(
    onClick: () -> Unit,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
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
