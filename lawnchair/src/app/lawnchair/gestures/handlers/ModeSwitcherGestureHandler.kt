package app.lawnchair.gestures.handlers

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import app.lawnchair.LawnchairLauncher
import app.lawnchair.modes.ModeProvider
import com.android.launcher3.R
import kotlinx.coroutines.launch

/**
 * Pops a bottom-sheet chooser of mode chips (matching the home-screen widget); tapping one
 * activates it. Bound via a home-screen gesture.
 */
class ModeSwitcherGestureHandler(context: Context) : GestureHandler(context) {
    override suspend fun onTrigger(launcher: LawnchairLauncher) {
        val repo = ModeProvider.repository(launcher)
        val state = repo.currentState()
        val modes = state.modes
        if (modes.isEmpty()) return

        launcher.runOnUiThread {
            val inflater = LayoutInflater.from(launcher)
            val sheet = inflater.inflate(R.layout.modes_switcher_sheet, null) as LinearLayout
            val container = sheet.findViewById<LinearLayout>(R.id.switcher_container)

            val dialog = Dialog(launcher)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            modes.chunked(2).forEach { pair ->
                val row = inflater.inflate(R.layout.modes_widget_row, container, false) as LinearLayout
                pair.forEach { mode ->
                    val isActive = mode.id == state.activeModeId
                    val label = "${mode.icon}  ${mode.name}"
                    val chip = inflater.inflate(R.layout.modes_widget_chip, row, false) as TextView
                    chip.text = if (isActive) "●  $label" else label
                    chip.setBackgroundResource(
                        if (isActive) R.drawable.modes_widget_chip_active else R.drawable.modes_widget_chip,
                    )
                    chip.setOnClickListener {
                        launcher.lifecycleScope.launch { repo.activate(mode.id) }
                        dialog.dismiss()
                    }
                    row.addView(chip)
                }
                if (pair.size == 1) {
                    val spacer = inflater.inflate(R.layout.modes_widget_spacer, row, false)
                    row.addView(spacer)
                }
                container.addView(row)
            }

            dialog.setContentView(sheet)
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.BOTTOM)
                setDimAmount(0.5f)
            }
            dialog.show()
        }
    }
}
