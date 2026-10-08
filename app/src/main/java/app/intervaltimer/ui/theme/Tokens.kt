package app.intervaltimer.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 4/8 dp spacing rhythm. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

object Sizes {
    val iconSm = 20.dp
    val iconMd = 24.dp
    val iconLg = 32.dp
    val iconHero = 56.dp
    val secondaryAction = 56.dp
    val primaryAction = 64.dp
    val heroAction = 80.dp
    val heroBadge = 112.dp
    val ringStroke = 12.dp
    val ringMax = 340.dp
    val contentMaxWidth = 480.dp
    val keypadMaxHeight = 380.dp
}

object Motion {
    const val FAST = 150
    const val MEDIUM = 250
    const val PULSE = 700
}

/** Tabular figures keep countdown digits from shifting as they change. */
val TimerDigits = TextStyle(
    fontWeight = FontWeight.Light,
    fontFeatureSettings = "tnum",
    letterSpacing = 0.sp,
)

/** True when the user turned animations off in developer or accessibility settings. */
@Composable
fun rememberReducedMotion(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
