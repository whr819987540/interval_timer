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
import app.intervaltimer.timer.DurationEntry
import app.intervaltimer.timer.TimeField
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
    val entry: DurationEntry = DurationEntry(),
    val alert: AlertSettings = AlertSettings(),
    val notificationsEnabled: Boolean = true,
)

sealed interface TimerEvent {
    data class SelectField(val field: TimeField) : TimerEvent
    data class Digit(val digit: Int) : TimerEvent
    data object Backspace : TimerEvent
    data object ClearField : TimerEvent
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
    private val entryText = savedState.getStateFlow(KEY_ENTRY, DurationEntry.fromMillis(settings.lastDurationMs).encode())
    private val currentEntry: DurationEntry get() = decodeEntry(entryText.value)
    private val notificationsEnabled = MutableStateFlow(readNotificationsEnabled())

    val uiState: StateFlow<TimerUiState> =
        combine(controller.state, entryText, settings.alert, notificationsEnabled) { timer, entry, alert, notifications ->
            TimerUiState(timer, decodeEntry(entry), alert, notifications)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimerUiState(
                timer = controller.state.value,
                entry = currentEntry,
                alert = settings.alert.value,
                notificationsEnabled = notificationsEnabled.value,
            ),
        )

    fun onEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.SelectField -> updateEntry { it.select(event.field) }
            is TimerEvent.Digit -> updateEntry { it.type(event.digit) }
            TimerEvent.Backspace -> updateEntry { it.backspace() }
            TimerEvent.ClearField -> updateEntry { it.clearField() }
            is TimerEvent.Preset -> updateEntry { DurationEntry.fromMillis(event.ms, it.selected) }
            TimerEvent.Start -> controller.start(currentEntry.totalMs)
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

    private fun decodeEntry(text: String): DurationEntry =
        DurationEntry.decode(text) ?: DurationEntry.fromMillis(settings.lastDurationMs)

    private inline fun updateEntry(transform: (DurationEntry) -> DurationEntry) {
        savedState[KEY_ENTRY] = transform(currentEntry).encode()
    }

    companion object {
        private const val KEY_ENTRY = "duration_entry"

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
