package app.lawnchair.modes.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppGatingTest {
    private val all = setOf("ig", "yt", "strava", "spotify", "clock")

    @Test fun hidesEverythingNotAllowed() {
        val mode = Mode(id = "gym", name = "Gym", allowedApps = setOf("strava", "spotify"))
        assertEquals(setOf("ig", "yt", "clock"), appsToHide(all, mode))
    }

    @Test fun allowAllHidesNothing() {
        val mode = Mode(id = "default", name = "Default", allowAll = true)
        assertTrue(appsToHide(all, mode).isEmpty())
    }

    @Test fun ignoresAllowedAppsNotInstalled() {
        val mode = Mode(id = "x", name = "X", allowedApps = setOf("strava", "ghost"))
        assertEquals(setOf("ig", "yt", "spotify", "clock"), appsToHide(all, mode))
    }
}
