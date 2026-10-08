package app.intervaltimer.system

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** System settings that decide whether the timer survives in the background and on the lock screen. */
data class SystemStatus(
    val notifications: Boolean = true,
    val batteryUnrestricted: Boolean = true,
    val exactAlarms: Boolean = true,
    val fullScreenAlerts: Boolean = true,
)

object SystemChecks {
    /** Exact-alarm access can be revoked from Android 12. */
    val hasExactAlarmSetting: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Full-screen intents need user consent from Android 14. */
    val hasFullScreenSetting: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    fun read(context: Context): SystemStatus = SystemStatus(
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        batteryUnrestricted = context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName),
        exactAlarms = !hasExactAlarmSetting ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
        fullScreenAlerts = !hasFullScreenSetting ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent(),
    )

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
