package app.lawnchair.modes

import android.content.Context
import app.lawnchair.modes.core.ModeRepository

/**
 * App-scoped, lazily-built singletons wiring the [ModeRepository] to the [ModeEngine], plus the
 * [ModeScheduler]. Built from the application context on first access; existing schedules are
 * re-armed when the repository is first created.
 */
object ModeProvider {
    @Volatile
    private var repo: ModeRepository? = null

    @Volatile
    private var sched: ModeScheduler? = null

    fun repository(context: Context): ModeRepository = repo ?: synchronized(this) { repo ?: build(context.applicationContext) }

    fun scheduler(context: Context): ModeScheduler = sched ?: synchronized(this) {
        sched ?: ModeScheduler(context.applicationContext).also { sched = it }
    }

    private fun build(appContext: Context): ModeRepository {
        val engine = ModeEngine(appContext)
        val repository = ModeRepository(PrefsModeStore(appContext)) { engine.applyMode(it) }
        repo = repository
        // Arm any schedules that already exist (e.g. after a process restart).
        runCatching { scheduler(appContext).reschedule(repository.currentState()) }
        return repository
    }
}
