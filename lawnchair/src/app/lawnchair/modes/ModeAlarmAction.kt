package app.lawnchair.modes

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import app.lawnchair.modes.core.ModeAlarm

/** Sets a one-off alarm in the system Clock app when a mode is activated. No special permission. */
class ModeAlarmAction(private val context: Context) {
    fun set(alarm: ModeAlarm) {
        if (!alarm.enabled) return
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, alarm.hour)
            putExtra(AlarmClock.EXTRA_MINUTES, alarm.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, alarm.message.ifBlank { "Mode alarm" })
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
