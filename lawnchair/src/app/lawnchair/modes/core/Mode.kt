package app.lawnchair.modes.core

import kotlinx.serialization.Serializable

/** Id of the built-in "Off" mode (no gating). */
const val OFF_MODE_ID = "default"

/** A wake alarm a mode sets on activation. */
@Serializable
data class ModeAlarm(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val message: String = "",
)

/** Optional time-of-day auto-activation. daysOfWeek uses java.util.Calendar values (1=Sun..7=Sat). */
@Serializable
data class ModeSchedule(
    val enabled: Boolean = false,
    val hour: Int = 22,
    val minute: Int = 0,
    val daysOfWeek: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
)

/**
 * A named profile that gates which apps are visible and can fire actions on activation.
 *
 * @property allowedApps component-key strings visible while this mode is active.
 * @property allowAll when true, no gating (the Default mode); [allowedApps] ignored.
 * @property dnd null = leave Do Not Disturb untouched; true/false = force on/off.
 */
@Serializable
data class Mode(
    val id: String,
    val name: String,
    val icon: String = "🎯",
    val allowedApps: Set<String> = emptySet(),
    val allowAll: Boolean = false,
    val alarm: ModeAlarm = ModeAlarm(),
    val dnd: Boolean? = null,
    val grayscale: Boolean = false,
    val schedule: ModeSchedule = ModeSchedule(),
)

@Serializable
data class ModesState(
    val modes: List<Mode> = emptyList(),
    val activeModeId: String? = null,
)
