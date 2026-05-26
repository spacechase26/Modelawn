package app.lawnchair.modes

import android.content.Context
import app.lawnchair.modes.core.ModeStore
import app.lawnchair.modes.core.ModesState
import app.lawnchair.preferences2.PreferenceManager2
import com.patrykmichalik.opto.core.firstBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

/**
 * Android-backed [ModeStore]: persists [ModesState] as JSON in the existing
 * PreferenceManager2 (key "modes_state_json"). Uses the explicit serializer form
 * for encode/decode to match Lawnchair's serialization style.
 */
class PrefsModeStore(context: Context) : ModeStore {
    private val prefs = PreferenceManager2.getInstance(context)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _state = MutableStateFlow(load())
    override val state: StateFlow<ModesState> = _state

    private fun load(): ModesState = runCatching {
        json.decodeFromString(ModesState.serializer(), prefs.modesJson.firstBlocking())
    }.getOrDefault(ModesState())

    override suspend fun update(transform: (ModesState) -> ModesState) {
        val next = transform(_state.value)
        _state.value = next
        prefs.modesJson.set(json.encodeToString(ModesState.serializer(), next))
    }
}
