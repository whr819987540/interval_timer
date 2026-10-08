package app.intervaltimer.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VibrationMode {
    /** Vibrate [AlertSettings.vibrationCount] pulses, then stop on its own. */
    COUNT,

    /** Vibrate until the user resets, silences or stops the timer. */
    CONTINUOUS,
}

data class AlertSettings(
    val mode: VibrationMode = VibrationMode.COUNT,
    val vibrationCount: Int = DEFAULT_COUNT,
    val sound: Boolean = false,
) {
    companion object {
        const val DEFAULT_COUNT = 5
        const val MIN_COUNT = 1
        const val MAX_COUNT = 30
    }
}

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _alert = MutableStateFlow(readAlert())
    val alert: StateFlow<AlertSettings> = _alert.asStateFlow()

    fun updateAlert(transform: (AlertSettings) -> AlertSettings) {
        val next = transform(_alert.value).let {
            it.copy(vibrationCount = it.vibrationCount.coerceIn(AlertSettings.MIN_COUNT, AlertSettings.MAX_COUNT))
        }
        _alert.value = next
        prefs.edit {
            putString(KEY_MODE, next.mode.name)
            putInt(KEY_COUNT, next.vibrationCount)
            putBoolean(KEY_SOUND, next.sound)
        }
    }

    /** Duration of the most recently started countdown, used to pre-fill the keypad. */
    var lastDurationMs: Long
        get() = prefs.getLong(KEY_LAST_DURATION, DEFAULT_DURATION_MS)
        set(value) = prefs.edit { putLong(KEY_LAST_DURATION, value) }

    /**
     * Returns true exactly once: the first time the app may show the system notification
     * permission dialog. Afterwards callers send the user to system settings instead, because the
     * dialog does not reappear once denied.
     */
    fun consumeNotificationPrompt(): Boolean {
        if (prefs.getBoolean(KEY_NOTIFICATION_PROMPTED, false)) return false
        prefs.edit { putBoolean(KEY_NOTIFICATION_PROMPTED, true) }
        return true
    }

    private fun readAlert() = AlertSettings(
        mode = VibrationMode.entries.firstOrNull { it.name == prefs.getString(KEY_MODE, null) }
            ?: VibrationMode.COUNT,
        vibrationCount = prefs.getInt(KEY_COUNT, AlertSettings.DEFAULT_COUNT)
            .coerceIn(AlertSettings.MIN_COUNT, AlertSettings.MAX_COUNT),
        sound = prefs.getBoolean(KEY_SOUND, false),
    )

    private companion object {
        const val KEY_MODE = "alert_mode"
        const val KEY_COUNT = "vibration_count"
        const val KEY_SOUND = "alert_sound"
        const val KEY_LAST_DURATION = "last_duration_ms"
        const val KEY_NOTIFICATION_PROMPTED = "notification_prompted"
        const val DEFAULT_DURATION_MS = 5 * 60_000L
    }
}
