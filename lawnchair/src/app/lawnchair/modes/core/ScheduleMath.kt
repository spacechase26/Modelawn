package app.lawnchair.modes.core

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Calendar day-of-week (1=Sun..7=Sat) for a ZonedDateTime (java.time MON=1..SUN=7). */
private fun ZonedDateTime.calendarDow(): Int = (dayOfWeek.value % 7) + 1

/**
 * Next epoch-millis this schedule should fire, strictly after [nowMillis],
 * or null if disabled / no days selected. Scans up to 8 days ahead.
 */
fun nextTriggerMillis(schedule: ModeSchedule, nowMillis: Long, zone: ZoneId): Long? {
    if (!schedule.enabled || schedule.daysOfWeek.isEmpty()) return null
    val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
    for (offset in 0..7) {
        val candidate = now.plusDays(offset.toLong())
            .withHour(schedule.hour).withMinute(schedule.minute)
            .withSecond(0).withNano(0)
        if (!schedule.daysOfWeek.contains(candidate.calendarDow())) continue
        val millis = candidate.toInstant().toEpochMilli()
        if (millis > nowMillis) return millis
    }
    return null
}
