package app.lawnchair.modes

import android.content.Context
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.util.ComponentKey
import com.patrykmichalik.opto.core.firstBlocking

/**
 * Bridges Launcher3's (Java) workspace binding to the same `hiddenApps` set the drawer uses,
 * so an active mode also gates placed home-screen icons.
 *
 * Non-destructive: hidden items are skipped at bind time (never removed from the DB), and they
 * reappear when the set clears and the workspace is reloaded. Called from
 * `Launcher.bindInflatedItems`.
 */
object ModeWorkspaceGating {
    /** The current hidden-app set; read once per workspace bind. */
    @JvmStatic
    fun hiddenComponentKeys(context: Context): Set<String> = PreferenceManager2.getInstance(context).hiddenApps.firstBlocking()

    /** True if [item] is an app icon whose component is in [hidden]. */
    @JvmStatic
    fun isHidden(hidden: Set<String>, item: ItemInfo): Boolean {
        if (hidden.isEmpty()) return false
        val component = item.targetComponent ?: return false
        return hidden.contains(ComponentKey(component, item.user).toString())
    }
}
