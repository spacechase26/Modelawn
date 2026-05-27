package app.lawnchair.modes.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeStore(initial: ModesState = ModesState()) : ModeStore {
    val flow = MutableStateFlow(initial)
    override val state = flow
    override suspend fun update(transform: (ModesState) -> ModesState) {
        flow.value = transform(flow.value)
    }
}

class ModeRepositoryTest {
    @Test fun addThenActivateInvokesEngine() = runBlocking {
        val store = FakeStore()
        var applied: Mode? = null
        val repo = ModeRepository(store) { applied = it }
        repo.upsert(Mode(id = "gym", name = "Gym", allowedApps = setOf("strava")))
        repo.activate("gym")
        assertEquals("gym", store.flow.value.activeModeId)
        assertEquals("gym", applied?.id) // engine applied the activated mode
    }

    @Test fun deleteActiveClearsActive() = runBlocking {
        val store = FakeStore(ModesState(listOf(Mode("a", "A")), "a"))
        val repo = ModeRepository(store) {}
        repo.delete("a")
        assertTrue(store.flow.value.modes.isEmpty())
        assertNull(store.flow.value.activeModeId)
    }

    @Test fun mergeAddsImportedModesWithoutTouchingActive() = runBlocking {
        val store = FakeStore(ModesState(listOf(Mode("a", "A")), "a"))
        val repo = ModeRepository(store) {}
        val backup = ModeBackup(modes = listOf(Mode("x", "Gym")))
        repo.merge(backup) { "fresh" }
        assertEquals(listOf("a", "fresh"), store.flow.value.modes.map { it.id })
        assertEquals("a", store.flow.value.activeModeId) // active untouched
    }

    @Test fun activateUnknownDoesNotChangeOrInvoke() = runBlocking {
        val store = FakeStore(ModesState(listOf(Mode("a", "A")), "a"))
        var calls = 0
        val repo = ModeRepository(store) { calls++ }
        repo.activate("nope")
        assertEquals("a", store.flow.value.activeModeId) // unchanged
        assertEquals(0, calls) // engine NOT invoked for a non-existent mode
    }
}
