package app.lawnchair.modes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.lawnchair.modes.core.ModesState
import app.lawnchair.modes.core.nextTriggerMillis
import java.time.ZoneId

/**
 * Schedules mode auto-activation with [AlarmManager]. Uses inexact, doze-friendly alarms
 * (`setAndAllowWhileIdle`) so no exact-alarm permission is needed — "around 11pm" is fine for a mode.
 */
class ModeScheduler(private val context: Context) {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Cancels then re-sets the next alarm for every mode with an enabled schedule. */
    fun reschedule(state: ModesState, now: Long = System.currentTimeMillis()) {
        state.modes.forEach { mode ->
            val pi = pendingIntent(mode.id)
            am.cancel(pi)
            val at = nextTriggerMillis(mode.schedule, now, ZoneId.systemDefault()) ?: return@forEach
            runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
        }
    }

    private fun pendingIntent(modeId: String): PendingIntent {
        val intent = Intent(context, ModeScheduleReceiver::class.java).apply {
            action = ModeScheduleReceiver.ACTION_ACTIVATE
            putExtra(ModeScheduleReceiver.EXTRA_MODE_ID, modeId)
        }
        return PendingIntent.getBroadcast(
            context,
            modeId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
