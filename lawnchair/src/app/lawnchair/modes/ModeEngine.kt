package app.lawnchair.modes

import android.content.Context
import app.lawnchair.modes.core.Mode
import app.lawnchair.modes.core.appsToHide
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.LauncherAppState

/**
 * Applies a mode's side effects. v1: strict app gating by writing Lawnchair's
 * `hiddenApps` set, which gates the drawer (via its onSet) and the workspace (via a
 * forced model reload). Alarm + DND are added later.
 */
class ModeEngine(
    private val context: Context,
    private val appList: AppListProvider = LauncherAppListProvider(context),
) {
    private val prefs2 = PreferenceManager2.getInstance(context)

    suspend fun applyMode(mode: Mode) {
        val hide = appsToHide(appList.allComponentKeys(), mode)
        prefs2.hiddenApps.set(hide)
        // The drawer observes hiddenApps directly, but the workspace does not — force a
        // re-bind so placed home-screen icons are gated too (see ModeWorkspaceGating).
        runCatching { LauncherAppState.getInstance(context).model.forceReload() }
    }
}
