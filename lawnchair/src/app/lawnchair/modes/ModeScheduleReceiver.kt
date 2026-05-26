package app.lawnchair.modes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires when a scheduled mode is due (explicit alarm intent) or on device boot. Activates the
 * requested mode, then reschedules the next occurrences. Also re-arms schedules after a reboot.
 */
class ModeScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val modeId = intent.getStringExtra(EXTRA_MODE_ID)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val repo = ModeProvider.repository(appContext)
                if (intent.action == ACTION_ACTIVATE && modeId != null) {
                    repo.activate(modeId)
                }
                // Re-arm next occurrences (covers both the scheduled activate and BOOT_COMPLETED).
                ModeProvider.scheduler(appContext).reschedule(repo.currentState())
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ACTIVATE = "app.lawnchair.modes.ACTION_ACTIVATE"
        const val EXTRA_MODE_ID = "mode_id"
    }
}
