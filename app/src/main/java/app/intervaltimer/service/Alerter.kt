package app.intervaltimer.service

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.intervaltimer.settings.AlertSettings
import app.intervaltimer.settings.VibrationMode

/** Plays the time-up alert: vibration pulses and, optionally, the default alarm sound. */
class Alerter(private val context: Context) {
    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    private val powerManager = context.getSystemService(PowerManager::class.java)

    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    var isActive = false
        private set

    fun start(settings: AlertSettings) {
        stop()
        isActive = true
        val effect = when (settings.mode) {
            VibrationMode.COUNT -> VibrationEffect.createWaveform(pulseTimings(settings.vibrationCount), NO_REPEAT)
            VibrationMode.CONTINUOUS -> VibrationEffect.createWaveform(longArrayOf(0, PULSE_ON_MS, PULSE_OFF_MS), 0)
        }
        vibrate(effect)
        if (settings.sound) playSound()
        // Keeps the CPU awake so the auto-stop in COUNT mode and the looping sound run on time.
        val timeout = when (settings.mode) {
            VibrationMode.COUNT -> durationMs(settings.vibrationCount) + WAKE_LOCK_SLACK_MS
            VibrationMode.CONTINUOUS -> CONTINUOUS_WAKE_LOCK_MS
        }
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IntervalTimer:alert").apply {
            setReferenceCounted(false)
            acquire(timeout)
        }
    }

    /** Lets the user feel the pulse pattern from the settings screen. */
    fun preview() {
        if (isActive) return
        vibrate(VibrationEffect.createWaveform(pulseTimings(PREVIEW_PULSES), NO_REPEAT))
    }

    fun stop() {
        if (!isActive) return
        vibrator.cancel()
        ringtone?.stop()
        ringtone = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        isActive = false
    }

    private fun vibrate(effect: VibrationEffect) {
        if (!vibrator.hasVibrator()) return
        // Alarm usage keeps the vibration working while the app is in the background.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, alarmAudioAttributes())
        }
    }

    private fun playSound() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
            audioAttributes = alarmAudioAttributes()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            play()
        }
    }

    private fun alarmAudioAttributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    companion object {
        const val PULSE_ON_MS = 500L
        const val PULSE_OFF_MS = 500L
        private const val NO_REPEAT = -1
        private const val PREVIEW_PULSES = 3
        private const val WAKE_LOCK_SLACK_MS = 5_000L
        private const val CONTINUOUS_WAKE_LOCK_MS = 30 * 60_000L

        /** How long [pulses] vibration pulses take; COUNT mode silences itself after this. */
        fun durationMs(pulses: Int): Long = pulses * (PULSE_ON_MS + PULSE_OFF_MS)

        private fun pulseTimings(pulses: Int): LongArray =
            LongArray(1 + pulses * 2) { i -> if (i == 0) 0 else if (i % 2 == 1) PULSE_ON_MS else PULSE_OFF_MS }
    }
}
