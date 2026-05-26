package app.lawnchair.modes.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleMathTest {
    private val zone = ZoneId.of("UTC")

    // Wed 2026-05-27 09:00 UTC
    private val now = ZonedDateTime.of(2026, 5, 27, 9, 0, 0, 0, zone).toInstant().toEpochMilli()

    @Test fun disabledScheduleReturnsNull() {
        assertNull(nextTriggerMillis(ModeSchedule(enabled = false), now, zone))
    }

    @Test fun laterTodaySchedulesToday() {
        // 22:00 today, all days -> today the 27th at 22:00
        val next = nextTriggerMillis(ModeSchedule(enabled = true, hour = 22, minute = 0), now, zone)!!
        val dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(next), zone)
        assertEquals(27, dt.dayOfMonth)
        assertEquals(22, dt.hour)
    }

    @Test fun earlierTimeSchedulesNextEligibleDay() {
        // 08:00 (past today's 09:00); only Thursday allowed -> Thu 2026-05-28 08:00
        val thu = setOf(java.util.Calendar.THURSDAY)
        val next = nextTriggerMillis(ModeSchedule(enabled = true, hour = 8, minute = 0, daysOfWeek = thu), now, zone)!!
        val dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(next), zone)
        assertEquals(DayOfWeek.THURSDAY, dt.dayOfWeek)
        assertEquals(28, dt.dayOfMonth)
    }
}
