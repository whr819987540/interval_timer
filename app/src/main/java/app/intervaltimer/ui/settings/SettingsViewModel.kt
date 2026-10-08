package app.intervaltimer.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.intervaltimer.appContainer
import app.intervaltimer.service.Alerter
import app.intervaltimer.settings.AlertSettings
import app.intervaltimer.settings.SettingsRepository
import app.intervaltimer.settings.VibrationMode
import app.intervaltimer.system.SystemChecks
import app.intervaltimer.system.SystemStatus
import app.intervaltimer.timer.TimerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
data class SettingsUiState(
    val alert: AlertSettings = AlertSettings(),
    val status: SystemStatus = SystemStatus(),
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val alerter: Alerter,
    private val controller: TimerController,
    private val readStatus: () -> SystemStatus,
) : ViewModel() {
    private val status = MutableStateFlow(readStatus())

    val uiState: StateFlow<SettingsUiState> =
        combine(settings.alert, status) { alert, status -> SettingsUiState(alert, status) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(settings.alert.value, status.value))

    fun refreshStatus() {
        status.value = readStatus()
    }

    /** After the permission dialog: show the countdown notification right away if now allowed. */
    fun onNotificationAccessResult() {
        refreshStatus()
        controller.ensureService()
    }

    fun setMode(mode: VibrationMode) = settings.updateAlert { it.copy(mode = mode) }

    fun changeCount(delta: Int) = settings.updateAlert { it.copy(vibrationCount = it.vibrationCount + delta) }

    fun setSound(enabled: Boolean) = settings.updateAlert { it.copy(sound = enabled) }

    fun preview() = alerter.preview()

    fun consumeNotificationPrompt(): Boolean = settings.consumeNotificationPrompt()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val container = app.appContainer
                SettingsViewModel(container.settings, container.alerter, container.controller) { SystemChecks.read(app) }
            }
        }
    }
}
