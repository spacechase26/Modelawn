package app.lawnchair.modes.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Coordinates mode persistence with the activation side effect.
 *
 * [onActivate] is the engine call (injected so the repository stays Android-free and
 * unit-testable). It runs only when [activate] makes the requested mode the active one.
 */
class ModeRepository(
    private val store: ModeStore,
    private val onActivate: suspend (Mode) -> Unit,
) {
    val state: StateFlow<ModesState> get() = store.state

    fun currentState(): ModesState = store.state.value

    suspend fun upsert(mode: Mode) = store.update { it.upsertMode(mode) }

    suspend fun delete(id: String) = store.update { it.deleteMode(id) }

    suspend fun activate(id: String) {
        store.update { it.activate(id) }
        val active = store.state.value.activeMode()
        if (active?.id == id) onActivate(active)
    }
}
