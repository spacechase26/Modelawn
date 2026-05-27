package app.lawnchair.modes.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeBackupTest {
    private val off = Mode(id = OFF_MODE_ID, name = "Off", icon = "🏠", allowAll = true)
    private val gym = Mode(id = "gym", name = "Gym", allowedApps = setOf("a", "b"), wallpaperPath = "/data/gym.png")
    private val sleep = Mode(id = "sleep", name = "Sleep", allowedApps = setOf("c"))
    private val state = ModesState(modes = listOf(off, gym, sleep), activeModeId = "gym")

    @Test fun toBackupExcludesOffMode() {
        val backup = state.toBackup()
        assertEquals(listOf("Gym", "Sleep"), backup.modes.map { it.name })
        assertFalse(backup.modes.any { it.allowAll })
    }

    @Test fun toBackupStripsWallpaperPath() {
        val backup = state.toBackup()
        assertTrue(backup.modes.all { it.wallpaperPath == null })
    }

    @Test fun mergeAddsImportedModesWithFreshIds() {
        val backup = ModeBackup(modes = listOf(gym, sleep))
        var n = 0
        val merged = state.mergeBackup(backup) { "new-${n++}" }

        // Originals untouched, two new modes appended.
        assertEquals(5, merged.modes.size)
        assertEquals(listOf(OFF_MODE_ID, "gym", "sleep", "new-0", "new-1"), merged.modes.map { it.id })
        // Existing gym kept its id and wallpaper.
        assertEquals("/data/gym.png", merged.modes.first { it.id == "gym" }.wallpaperPath)
    }

    @Test fun mergeSkipsAllowAllEntriesInFile() {
        val backup = ModeBackup(modes = listOf(off, gym))
        val merged = state.mergeBackup(backup) { "new-id" }
        // Only gym imported; the allowAll "Off" in the file is dropped.
        assertEquals(4, merged.modes.size)
        assertEquals(1, merged.modes.count { it.id == "new-id" })
    }

    @Test fun mergeStripsWallpaperOnImport() {
        val backup = ModeBackup(modes = listOf(gym))
        val merged = state.mergeBackup(backup) { "new-id" }
        assertNull(merged.modes.first { it.id == "new-id" }.wallpaperPath)
    }

    @Test fun mergeLeavesActiveModeUntouched() {
        val backup = ModeBackup(modes = listOf(sleep))
        val merged = state.mergeBackup(backup) { "new-id" }
        assertEquals("gym", merged.activeModeId)
    }

    @Test fun encodeDecodeRoundTrips() {
        val backup = state.toBackup()
        val decoded = decodeBackup(encodeBackup(backup))
        assertEquals(backup, decoded)
    }

    @Test fun decodeReturnsNullOnGarbage() {
        assertNull(decodeBackup("not json at all"))
        assertNull(decodeBackup(""))
        assertNull(decodeBackup("{\"unrelated\": 5}"))
    }

    @Test fun decodeIgnoresUnknownKeys() {
        val json = """{"version":1,"modes":[{"id":"x","name":"X","futureField":true}]}"""
        val decoded = decodeBackup(json)
        assertEquals("X", decoded?.modes?.single()?.name)
    }
}
