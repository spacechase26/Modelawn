package app.lawnchair.modes.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModesStateOpsTest {
    private val gym = Mode(id = "gym", name = "Gym", allowedApps = setOf("a", "b"))
    private val sleep = Mode(id = "sleep", name = "Sleep", allowedApps = setOf("c"))
    private val base = ModesState(modes = listOf(gym, sleep), activeModeId = "gym")

    @Test fun activeModeReturnsCurrent() {
        assertEquals(gym, base.activeMode())
    }

    @Test fun activeModeNullWhenMissing() {
        assertNull(base.copy(activeModeId = "nope").activeMode())
    }

    @Test fun upsertAddsNewMode() {
        val work = Mode(id = "work", name = "Work")
        val next = base.upsertMode(work)
        assertEquals(3, next.modes.size)
        assertEquals(work, next.modes.last())
    }

    @Test fun upsertReplacesExisting() {
        val next = base.upsertMode(gym.copy(name = "Gym 2"))
        assertEquals(2, next.modes.size)
        assertEquals("Gym 2", next.modes.first().name)
    }

    @Test fun deleteRemovesAndClearsActive() {
        val next = base.deleteMode("gym")
        assertEquals(1, next.modes.size)
        assertNull(next.activeModeId)
    }

    @Test fun activateSetsId() {
        assertEquals("sleep", base.activate("sleep").activeModeId)
    }

    @Test fun activateUnknownIsNoOp() {
        assertEquals("gym", base.activate("nope").activeModeId)
    }
}
