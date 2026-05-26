package app.lawnchair.modes

import android.content.Context
import app.lawnchair.modes.core.Mode
import app.lawnchair.modes.core.appsToHide
import app.lawnchair.preferences2.PreferenceManager2

/**
 * Applies a mode's side effects. v1: strict app gating by writing Lawnchair's
 * `hiddenApps` set (its onSet reloads the drawer). Alarm + DND are added later.
 */
class ModeEngine(
    context: Context,
    private val appList: AppListProvider = LauncherAppListProvider(context),
) {
    private val prefs2 = PreferenceManager2.getInstance(context)

    suspend fun applyMode(mode: Mode) {
        val hide = appsToHide(appList.allComponentKeys(), mode)
        prefs2.hiddenApps.set(hide)
    }
}
