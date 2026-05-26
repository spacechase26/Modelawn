package app.lawnchair.gestures.handlers

import android.app.AlertDialog
import android.content.Context
import androidx.lifecycle.lifecycleScope
import app.lawnchair.LawnchairLauncher
import app.lawnchair.modes.ModeProvider
import com.android.launcher3.R
import kotlinx.coroutines.launch

/** Pops a quick dialog listing the modes; tapping one activates it. Bound via a home-screen gesture. */
class ModeSwitcherGestureHandler(context: Context) : GestureHandler(context) {
    override suspend fun onTrigger(launcher: LawnchairLauncher) {
        val repo = ModeProvider.repository(launcher)
        val state = repo.currentState()
        val modes = state.modes
        if (modes.isEmpty()) return
        val labels = modes.map { mode ->
            val active = if (mode.id == state.activeModeId) "  ✓" else ""
            "${mode.icon}  ${mode.name}$active"
        }.toTypedArray()
        launcher.runOnUiThread {
            AlertDialog.Builder(launcher)
                .setTitle(launcher.getString(R.string.gesture_handler_open_mode_switcher))
                .setItems(labels) { _, which ->
                    launcher.lifecycleScope.launch { repo.activate(modes[which].id) }
                }
                .show()
        }
    }
}
