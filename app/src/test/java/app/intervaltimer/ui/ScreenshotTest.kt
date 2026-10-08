package app.intervaltimer.ui

import android.os.SystemClock
import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.intervaltimer.settings.AlertSettings
import app.intervaltimer.settings.VibrationMode
import app.intervaltimer.system.SystemStatus
import app.intervaltimer.timer.DurationDigits
import app.intervaltimer.timer.TimerState
import app.intervaltimer.ui.alert.AlertScreen
import app.intervaltimer.ui.settings.SettingsScreen
import app.intervaltimer.ui.settings.SettingsUiState
import app.intervaltimer.ui.theme.IntervalTimerTheme
import app.intervaltimer.ui.timer.TimerScreen
import app.intervaltimer.ui.timer.TimerUiState
import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import org.junit.Rule
import org.junit.Test

/**
 * Renders every screen on the JVM (no emulator needed). Record with
 * `./gradlew recordPaparazziDebug`, check with `./gradlew verifyPaparazziDebug`.
 */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = PHONE, theme = "android:Theme.Material.Light.NoActionBar")

    private val fiveMinutes = 5 * 60_000L

    @Test
    fun setup_light() = snap(PHONE) { Timer(TimerUiState(entry = DurationDigits("500"))) }

    @Test
    fun setup_dark() = snap(PHONE.copy(nightMode = NightMode.NIGHT)) { Timer(TimerUiState(entry = DurationDigits("2500"))) }

    @Test
    fun setup_smallPhone_notificationsOff() = snap(SMALL_PHONE) {
        Timer(TimerUiState(entry = DurationDigits("130"), notificationsEnabled = false))
    }

    @Test
    fun setup_landscape() = snap(PHONE.copy(orientation = ScreenOrientation.LANDSCAPE)) { Timer(TimerUiState(entry = DurationDigits("500"))) }

    @Test
    fun setup_largeFont() = snap(PHONE.copy(fontScale = 1.6f)) { Timer(TimerUiState(entry = DurationDigits("13000"))) }

    @Test
    fun running_light() = snap(PHONE) {
        Timer(TimerUiState(timer = TimerState.Running(fiveMinutes, SystemClock.elapsedRealtime() + 192_000, round = 3)))
    }

    @Test
    fun paused_dark() = snap(PHONE.copy(nightMode = NightMode.NIGHT)) {
        Timer(TimerUiState(timer = TimerState.Paused(fiveMinutes, remainingMs = 75_000, round = 2)))
    }

    @Test
    fun paused_landscape() = snap(PHONE.copy(orientation = ScreenOrientation.LANDSCAPE)) {
        Timer(TimerUiState(timer = TimerState.Paused(3_600_000L + fiveMinutes, remainingMs = 3_000_000, round = 1)))
    }

    @Test
    fun finished_alerting_light() = snap(PHONE) {
        Timer(TimerUiState(timer = TimerState.Finished(fiveMinutes, round = 4, alerting = true)))
    }

    @Test
    fun lockScreenAlert_dark() = snap(PHONE.copy(nightMode = NightMode.NIGHT)) {
        AlertScreen(TimerState.Finished(fiveMinutes, round = 4, alerting = true), continuousAlert = true, {}, {}, {})
    }

    @Test
    fun lockScreenAlert_landscape() = snap(PHONE.copy(orientation = ScreenOrientation.LANDSCAPE)) {
        AlertScreen(TimerState.Finished(fiveMinutes, round = 4, alerting = false), continuousAlert = false, {}, {}, {})
    }

    @Test
    fun settings_light() = snap(TALL_PHONE) { Settings(SettingsUiState(status = SystemStatus(batteryUnrestricted = false, fullScreenAlerts = false))) }

    @Test
    fun settings_dark_continuous() = snap(TALL_PHONE.copy(nightMode = NightMode.NIGHT)) {
        Settings(SettingsUiState(alert = AlertSettings(mode = VibrationMode.CONTINUOUS, sound = true)))
    }

    private fun snap(device: DeviceConfig, content: @Composable () -> Unit) {
        paparazzi.unsafeUpdateConfig(deviceConfig = device)
        paparazzi.snapshot { IntervalTimerTheme(darkTheme = device.nightMode == NightMode.NIGHT) { content() } }
    }

    @Composable
    private fun Timer(state: TimerUiState) = TimerScreen(state, onEvent = {}, onEnableNotifications = {}, onOpenSettings = {})

    @Composable
    private fun Settings(state: SettingsUiState) = SettingsScreen(
        state = state,
        onBack = {},
        onModeChange = {},
        onCountChange = {},
        onSoundChange = {},
        onPreview = {},
        onFixNotifications = {},
        onFixExactAlarms = {},
        onFixFullScreen = {},
        onFixBattery = {},
        onOpenAppDetails = {},
    )

    private companion object {
        val PHONE = DeviceConfig.PIXEL_5
        val SMALL_PHONE = DeviceConfig.NEXUS_5
        val TALL_PHONE = DeviceConfig.PIXEL_5.copy(screenHeight = 3600)
    }
}
