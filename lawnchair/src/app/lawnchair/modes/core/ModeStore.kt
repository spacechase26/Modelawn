package app.lawnchair.modes.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Persists the modes state and exposes it as observable state.
 * The Android-backed implementation (PrefsModeStore) lives outside `core`.
 */
interface ModeStore {
    val state: StateFlow<ModesState>
    suspend fun update(transform: (ModesState) -> ModesState)
}
