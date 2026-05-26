package app.lawnchair.modes

import android.content.Context
import app.lawnchair.modes.core.ModeRepository

/**
 * App-scoped, lazily-built singleton wiring the [ModeRepository] to the [ModeEngine].
 * Avoids touching Dagger; built from the application context on first access.
 */
object ModeProvider {
    @Volatile
    private var repo: ModeRepository? = null

    fun repository(context: Context): ModeRepository =
        repo ?: synchronized(this) {
            repo ?: build(context.applicationContext).also { repo = it }
        }

    private fun build(appContext: Context): ModeRepository {
        val engine = ModeEngine(appContext)
        return ModeRepository(PrefsModeStore(appContext)) { engine.applyMode(it) }
    }
}
