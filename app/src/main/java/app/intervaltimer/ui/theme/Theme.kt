package app.intervaltimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette: ui-ux-pro-max "Timer & Pomodoro" (focus red + break green on slate). Contrast ratios
// are listed in docs/DESIGN.md; re-check them there when changing a value.
private object Palette {
    val Slate50 = Color(0xFFF8FAFC)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate150 = Color(0xFFEEF2F7)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)
    val Slate600 = Color(0xFF475569)
    val Slate700 = Color(0xFF334155)
    val Slate900 = Color(0xFF0F172A)
    val Navy950 = Color(0xFF0B1222)
    val Navy900 = Color(0xFF131B2E)
    val Navy800 = Color(0xFF192134)
    val Navy700 = Color(0xFF222C42)
    val Navy600 = Color(0xFF2A3550)
    val Red100 = Color(0xFFFEE2E2)
    val Red200 = Color(0xFFFECACA)
    val Red400 = Color(0xFFF87171)
    val Red600 = Color(0xFFDC2626)
    val Red800 = Color(0xFF991B1B)
    val Red950 = Color(0xFF4A1414)
    val Emerald100 = Color(0xFFD1FAE5)
    val Emerald200 = Color(0xFFA7F3D0)
    val Emerald400 = Color(0xFF34D399)
    val Emerald700 = Color(0xFF047857)
    val Emerald800 = Color(0xFF065F46)
    val Emerald900 = Color(0xFF064E3B)
    val White = Color(0xFFFFFFFF)
}

private val LightColors = lightColorScheme(
    primary = Palette.Red600,
    onPrimary = Palette.White,
    primaryContainer = Palette.Red600,
    onPrimaryContainer = Palette.White,
    secondary = Palette.Slate600,
    onSecondary = Palette.White,
    secondaryContainer = Palette.Red100,
    onSecondaryContainer = Palette.Red800,
    tertiary = Palette.Emerald700,
    onTertiary = Palette.White,
    tertiaryContainer = Palette.Emerald100,
    onTertiaryContainer = Palette.Emerald800,
    background = Palette.Slate50,
    onBackground = Palette.Slate900,
    surface = Palette.Slate50,
    onSurface = Palette.Slate900,
    surfaceVariant = Palette.Slate100,
    onSurfaceVariant = Palette.Slate600,
    surfaceContainerLowest = Palette.White,
    surfaceContainerLow = Palette.White,
    surfaceContainer = Palette.White,
    surfaceContainerHigh = Palette.Slate150,
    surfaceContainerHighest = Palette.Slate200,
    outline = Palette.Slate400,
    outlineVariant = Palette.Slate200,
    error = Palette.Red600,
    onError = Palette.White,
)

private val DarkColors = darkColorScheme(
    primary = Palette.Red400,
    onPrimary = Palette.Slate900,
    primaryContainer = Palette.Red600,
    onPrimaryContainer = Palette.White,
    secondary = Palette.Slate400,
    onSecondary = Palette.Slate900,
    secondaryContainer = Palette.Red950,
    onSecondaryContainer = Palette.Red200,
    tertiary = Palette.Emerald400,
    onTertiary = Palette.Slate900,
    tertiaryContainer = Palette.Emerald900,
    onTertiaryContainer = Palette.Emerald200,
    background = Palette.Slate900,
    onBackground = Palette.Slate50,
    surface = Palette.Slate900,
    onSurface = Palette.Slate50,
    surfaceVariant = Palette.Navy800,
    onSurfaceVariant = Palette.Slate400,
    surfaceContainerLowest = Palette.Navy950,
    surfaceContainerLow = Palette.Navy900,
    surfaceContainer = Palette.Navy800,
    surfaceContainerHigh = Palette.Navy700,
    surfaceContainerHighest = Palette.Navy600,
    outline = Palette.Slate500,
    outlineVariant = Palette.Slate700,
    error = Palette.Red400,
    onError = Palette.Slate900,
)

@Composable
fun IntervalTimerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
