package app.lawnchair.modes

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.lawnchair.modes.core.ModeSchedule
import app.lawnchair.modes.core.ModesState
import app.lawnchair.modes.core.OFF_MODE_ID
import app.lawnchair.modes.core.nextTriggerMillis
import java.time.ZoneId

/**
 * Schedules mode auto-activation AND auto-deactivation with [AlarmManager]. Uses inexact,
 * doze-friendly alarms (`setAndAllowWhileIdle`) so no exact-alarm permission is needed.
 * Auto-deactivate simply activates the built-in "Off" mode at its time, sharing the mode's days.
 */
class ModeScheduler(private val context: Context) {
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Cancels then re-sets each mode's activate + deactivate alarms. */
    fun reschedule(state: ModesState, now: Long = System.currentTimeMillis()) {
        val zone = ZoneId.systemDefault()
        state.modes.forEach { mode ->
            // Auto-activate this mode at its schedule time.
            arm("on:${mode.id}", nextTriggerMillis(mode.schedule, now, zone), mode.id)
            // Auto-deactivate → return to Off, on the same days as the activate schedule.
            val offSchedule = ModeSchedule(
                enabled = mode.autoOffEnabled,
                hour = mode.autoOffHour,
                minute = mode.autoOffMinute,
                daysOfWeek = mode.schedule.daysOfWeek,
            )
            arm("off:${mode.id}", nextTriggerMillis(offSchedule, now, zone), OFF_MODE_ID)
        }
    }

    private fun arm(key: String, at: Long?, targetModeId: String) {
        val pi = pendingIntent(key, targetModeId)
        am.cancel(pi)
        if (at != null) {
            runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) }
        }
    }

    private fun pendingIntent(key: String, targetModeId: String): PendingIntent {
        val intent = Intent(context, ModeScheduleReceiver::class.java).apply {
            action = ModeScheduleReceiver.ACTION_ACTIVATE
            putExtra(ModeScheduleReceiver.EXTRA_MODE_ID, targetModeId)
            data = Uri.parse("modelawn://schedule/$key")
        }
        return PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
