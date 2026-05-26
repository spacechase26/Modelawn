package app.lawnchair.modes

import android.content.Context
import app.lawnchair.modes.core.Mode
import app.lawnchair.modes.core.appsToHide
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.LauncherAppState

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
) {
    private val prefs2 = PreferenceManager2.getInstance(context)

    suspend fun applyMode(mode: Mode) {
        val report = StringBuilder("Mode: ${mode.name}")
        runCatching {
            val hide = appsToHide(appList.allComponentKeys(), mode)
            prefs2.hiddenApps.set(hide)
            report.append(" · gated ${hide.size}")
        }.onFailure { report.append(" · gate ERR") }
        // The drawer observes hiddenApps directly, but the workspace does not — force a
        // re-bind so placed home-screen icons are gated too (see ModeWorkspaceGating).
        runCatching { LauncherAppState.getInstance(context).model.forceReload() }
        if (mode.alarm.enabled) {
            runCatching {
                alarmAction.set(mode.alarm)
                report.append(" · alarm %02d:%02d".format(mode.alarm.hour, mode.alarm.minute))
            }.onFailure { report.append(" · alarm ERR: ${it.message}") }
        }
        mode.dnd?.let { wanted ->
            if (dndController.hasAccess()) {
                dndController.setEnabled(wanted)
                report.append(" · dnd ${if (wanted) "on" else "off"}")
            } else {
                report.append(" · dnd NO-ACCESS")
            }
        }
        ModeDebug.toast(context, report.toString())
    }
}
