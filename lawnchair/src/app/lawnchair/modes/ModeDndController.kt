package app.lawnchair.modes

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Toggles Do Not Disturb on mode activation. Needs one-time "DND access" granted by the user. */
class ModeDndController(private val context: Context) {
    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun hasAccess(): Boolean = nm.isNotificationPolicyAccessGranted

    /** Intent to the system screen where the user grants DND access (one-time). */
    fun requestAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun setEnabled(enabled: Boolean) {
        if (!hasAccess()) return
        runCatching {
            nm.setInterruptionFilter(
                if (enabled) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                },
            )
        }
    }
}
