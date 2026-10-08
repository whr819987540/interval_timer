package app.intervaltimer.ui.timer

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.intervaltimer.appContainer
import app.intervaltimer.settings.AlertSettings
import app.intervaltimer.settings.SettingsRepository
import app.intervaltimer.system.SystemChecks
import app.intervaltimer.timer.DurationDigits
import app.intervaltimer.timer.TimerController
import app.intervaltimer.timer.TimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
data class TimerUiState(
    val timer: TimerState = TimerState.Idle,
    val entry: DurationDigits = DurationDigits.Empty,
    val alert: AlertSettings = AlertSettings(),
    val notificationsEnabled: Boolean = true,
)

sealed interface TimerEvent {
    data class Key(val key: String) : TimerEvent
    data object Backspace : TimerEvent
    data object Clear : TimerEvent
    data class Preset(val ms: Long) : TimerEvent
    data object Start : TimerEvent
    data object Pause : TimerEvent
    data object Resume : TimerEvent
    data object Restart : TimerEvent
    data object Silence : TimerEvent
    data object Stop : TimerEvent
}

class TimerViewModel(
    private val controller: TimerController,
    private val settings: SettingsRepository,
    private val savedState: SavedStateHandle,
    private val readNotificationsEnabled: () -> Boolean,
) : ViewModel() {
    private val entryDigits = savedState.getStateFlow(KEY_ENTRY, DurationDigits.fromMillis(settings.lastDurationMs).digits)
    private val notificationsEnabled = MutableStateFlow(readNotificationsEnabled())

    val uiState: StateFlow<TimerUiState> =
        combine(controller.state, entryDigits, settings.alert, notificationsEnabled) { timer, digits, alert, notifications ->
            TimerUiState(timer, DurationDigits(digits), alert, notifications)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimerUiState(
                timer = controller.state.value,
                entry = DurationDigits(entryDigits.value),
                alert = settings.alert.value,
                notificationsEnabled = notificationsEnabled.value,
            ),
        )

    fun onEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.Key -> updateEntry { it.append(event.key) }
            TimerEvent.Backspace -> updateEntry { it.backspace() }
            TimerEvent.Clear -> updateEntry { DurationDigits.Empty }
            is TimerEvent.Preset -> updateEntry { DurationDigits.fromMillis(event.ms) }
            TimerEvent.Start -> controller.start(DurationDigits(entryDigits.value).totalMs)
            TimerEvent.Pause -> controller.pause()
            TimerEvent.Resume -> controller.resume()
            TimerEvent.Restart -> controller.restart()
            TimerEvent.Silence -> controller.silence()
            TimerEvent.Stop -> controller.stop()
        }
    }

    fun refreshSystemStatus() {
        notificationsEnabled.value = readNotificationsEnabled()
    }

    /** After the permission dialog: show the countdown notification right away if now allowed. */
    fun onNotificationAccessResult() {
        refreshSystemStatus()
        controller.ensureService()
    }

    /** True when the system permission dialog should be shown instead of opening settings. */
    fun consumeNotificationPrompt(): Boolean = settings.consumeNotificationPrompt()

    private inline fun updateEntry(transform: (DurationDigits) -> DurationDigits) {
        savedState[KEY_ENTRY] = transform(DurationDigits(entryDigits.value)).digits
    }

    companion object {
        private const val KEY_ENTRY = "entry_digits"

        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val container = app.appContainer
                TimerViewModel(container.controller, container.settings, createSavedStateHandle()) {
                    SystemChecks.read(app).notifications
                }
            }
        }
    }
}
