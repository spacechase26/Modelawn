package app.lawnchair.modes

import android.content.Context
import app.lawnchair.modes.core.Mode
import app.lawnchair.modes.core.appsToHide
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.LauncherAppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Applies a mode's side effects: strict app gating (writes Lawnchair's `hiddenApps`, which
 * gates the drawer via its onSet and the workspace via a forced model reload), plus the
 * optional alarm and Do-Not-Disturb actions.
 */
class ModeEngine(
    private val context: Context,
    private val appList: AppListProvider = LauncherAppListProvider(context),
    private val alarmAction: ModeAlarmAction = ModeAlarmAction(context),
    private val dndController: ModeDndController = ModeDndController(context),
    private val grayscaleController: ModeGrayscaleController = ModeGrayscaleController(context),
    private val wallpaperController: ModeWallpaperController = ModeWallpaperController(context),
) {
    private val prefs2 = PreferenceManager2.getInstance(context)

    suspend fun applyMode(mode: Mode) {
        // Run off the main thread: callers activate from the UI/gesture (Main dispatcher), and
        // enumerating all apps + forcing a model reload here would otherwise block the UI (ANR).
        withContext(Dispatchers.Default) {
            val hide = appsToHide(appList.allComponentKeys(), mode)
            prefs2.hiddenApps.set(hide)
            if (mode.alarm.enabled) runCatching { alarmAction.set(mode.alarm) }
            // Always enforce the mode's DND state: a mode that doesn't enable DND (null/false)
            // turns it off, so switching to Off (or any non-DND mode) clears it.
            if (dndController.hasAccess()) dndController.setEnabled(mode.dnd == true)
            grayscaleController.setEnabled(mode.grayscale)
            // Set the wallpaper BEFORE reloading: Lawnchair's themed icons re-tint only during a
            // reload, so applying the wallpaper first lets the rebind pick up the new colors.
            runCatching { wallpaperController.apply(mode.wallpaperPath) }
            // The drawer observes hiddenApps directly, but the workspace does not — force a
            // re-bind so placed home-screen icons are gated too (see ModeWorkspaceGating).
            runCatching { LauncherAppState.getInstance(context).model.forceReload() }
        }
    }
}
