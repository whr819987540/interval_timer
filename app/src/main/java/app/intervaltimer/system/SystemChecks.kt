package app.intervaltimer.system

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import app.intervaltimer.service.TimerNotifications

/** System settings that decide whether the timer survives in the background and on the lock screen. */
data class SystemStatus(
    val notifications: Boolean = true,
    val lockScreenNotifications: Boolean = true,
    val batteryUnrestricted: Boolean = true,
    val exactAlarms: Boolean = true,
    val fullScreenAlerts: Boolean = true,
)

object SystemChecks {
    /** Exact-alarm access can be revoked from Android 12. */
    val hasExactAlarmSetting: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Full-screen intents need user consent from Android 14. */
    val hasFullScreenSetting: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    fun read(context: Context): SystemStatus {
        val notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return SystemStatus(
            notifications = notifications,
            lockScreenNotifications = notifications && channelsShowOnLockScreen(context) && systemShowsLockScreenNotifications(context),
            batteryUnrestricted = context.getSystemService(PowerManager::class.java)
                .isIgnoringBatteryOptimizations(context.packageName),
            exactAlarms = !hasExactAlarmSetting ||
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
            fullScreenAlerts = !hasFullScreenSetting ||
                context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent(),
        )
    }

    /** False when the user blocked one of our channels or set it to "don't show on lock screen". */
    private fun channelsShowOnLockScreen(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        return listOf(TimerNotifications.CHANNEL_RUNNING, TimerNotifications.CHANNEL_ALERT).all { id ->
            val channel = manager.getNotificationChannel(id) ?: return@all true
            channel.importance != NotificationManager.IMPORTANCE_NONE &&
                channel.lockscreenVisibility != Notification.VISIBILITY_SECRET
        }
    }

    /**
     * The system-wide "show notifications on lock screen" switch. It is not public API, so a ROM
     * that hides or renames it counts as "on" rather than as a false alarm.
     */
    private fun systemShowsLockScreenNotifications(context: Context): Boolean = runCatching {
        Settings.Secure.getInt(context.contentResolver, "lock_screen_show_notifications", 1) != 0
    }.getOrDefault(true)

    fun notificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    // A timer that must fire on time is an accepted use of this request.
    @SuppressLint("BatteryLife")
    fun batteryOptimizationRequest(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context))

    @SuppressLint("InlinedApi")
    fun exactAlarmSettings(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))

    @SuppressLint("InlinedApi")
    fun fullScreenSettings(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri(context))

    fun appDetails(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))

    private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")
}

/** Opens a settings screen, falling back to the app details page on ROMs that lack it. */
fun Context.openSystemSettings(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        startActivity(SystemChecks.appDetails(this))
    }
}
