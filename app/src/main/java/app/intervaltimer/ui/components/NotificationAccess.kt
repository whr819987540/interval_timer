package app.intervaltimer.ui.components

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.intervaltimer.system.SystemChecks
import app.intervaltimer.system.openSystemSettings

/**
 * Requests notification access. The system dialog can only be shown usefully once (Android 13+);
 * after that the user is sent to the app's notification settings instead.
 */
class NotificationAccess internal constructor(
    private val context: Context,
    private val launcher: ManagedActivityResultLauncher<String, Boolean>,
    private val consumePrompt: () -> Boolean,
) {
    /** Shows the system dialog if it has never been shown. Returns whether it was shown. */
    fun promptOnce(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !consumePrompt()) return false
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        return true
    }

    /** Shows the dialog the first time, otherwise opens notification settings. */
    fun request() {
        if (!promptOnce()) context.openSystemSettings(SystemChecks.notificationSettings(context))
    }
}

@Composable
fun rememberNotificationAccess(consumePrompt: () -> Boolean, onResult: () -> Unit): NotificationAccess {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult() }
    return remember(context, launcher) { NotificationAccess(context, launcher, consumePrompt) }
}
