package app.lawnchair.modes

import android.content.Context
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.model.data.LauncherAppWidgetInfo
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

    /**
     * True if [item] should be hidden under the active mode: an app whose component is in
     * [hidden]; a widget whose app is hidden; or a folder whose every item is hidden.
     */
    @JvmStatic
    fun isHidden(hidden: Set<String>, item: ItemInfo): Boolean {
        if (hidden.isEmpty()) return false
        return when (item) {
            is FolderInfo -> {
                val contents = item.getContents()
                contents.isNotEmpty() && contents.all { isHidden(hidden, it) }
            }

            is LauncherAppWidgetInfo -> {
                val pkg = item.providerName?.packageName
                pkg != null && hidden.any { it.substringBefore('/') == pkg }
            }

            else -> {
                val component = item.targetComponent
                component != null && hidden.contains(ComponentKey(component, item.user).toString())
            }
        }
    }

    /** [items] minus apps hidden by the active mode — for gating folder previews + open folders. */
    @JvmStatic
    fun filterVisible(context: Context, items: List<ItemInfo>): ArrayList<ItemInfo> {
        val hidden = hiddenComponentKeys(context)
        val result = ArrayList<ItemInfo>(items.size)
        for (item in items) if (!isHidden(hidden, item)) result.add(item)
        return result
    }
}
