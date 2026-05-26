package app.lawnchair.modes.core

/** The currently active mode, or null if none/unknown. */
fun ModesState.activeMode(): Mode? = modes.firstOrNull { it.id == activeModeId }

/** Adds [mode], or replaces the existing mode with the same id. */
fun ModesState.upsertMode(mode: Mode): ModesState {
    val idx = modes.indexOfFirst { it.id == mode.id }
    val newModes = if (idx >= 0) modes.toMutableList().also { it[idx] = mode } else modes + mode
    return copy(modes = newModes)
}

/** Removes the mode with [id]; clears the active id if it was the one removed. */
fun ModesState.deleteMode(id: String): ModesState = copy(
    modes = modes.filterNot { it.id == id },
    activeModeId = if (activeModeId == id) null else activeModeId,
)

/** Sets the active mode to [id] if such a mode exists; otherwise no change. */
fun ModesState.activate(id: String): ModesState = if (modes.any { it.id == id }) copy(activeModeId = id) else this
