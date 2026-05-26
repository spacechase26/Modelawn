package app.lawnchair.modes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import app.lawnchair.modes.ModeProvider
import app.lawnchair.modes.core.ModesState
import com.android.launcher3.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Home-screen widget: a Material You chip grid of modes; tap a chip to switch to it. */
class ModesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val state = ModeProvider.repository(context.applicationContext).currentState()
        for (id in ids) render(context, manager, id, state)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_SWITCH) return
        val modeId = intent.getStringExtra(EXTRA_MODE_ID) ?: return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ModeProvider.repository(appContext).activate(modeId)
            } finally {
                refresh(appContext)
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_SWITCH = "app.lawnchair.modes.WIDGET_SWITCH"
        const val EXTRA_MODE_ID = "mode_id"

        /** Rebuild every placed widget instance (call after a switch or any mode change). */
        @JvmStatic
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, ModesWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val state = ModeProvider.repository(context.applicationContext).currentState()
            for (id in ids) render(context, manager, id, state)
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int, state: ModesState) {
            val views = RemoteViews(context.packageName, R.layout.modes_widget)
            views.removeAllViews(R.id.modes_container)
            state.modes.chunked(2).forEach { pair ->
                val row = RemoteViews(context.packageName, R.layout.modes_widget_row)
                pair.forEach { mode ->
                    val chip = RemoteViews(context.packageName, R.layout.modes_widget_chip)
                    chip.setTextViewText(R.id.chip, "${mode.icon}  ${mode.name}")
                    chip.setInt(
                        R.id.chip,
                        "setBackgroundResource",
                        if (mode.id == state.activeModeId) {
                            R.drawable.modes_widget_chip_active
                        } else {
                            R.drawable.modes_widget_chip
                        },
                    )
                    chip.setOnClickPendingIntent(R.id.chip, switchIntent(context, mode.id))
                    row.addView(R.id.row_container, chip)
                }
                views.addView(R.id.modes_container, row)
            }
            manager.updateAppWidget(id, views)
        }

        private fun switchIntent(context: Context, modeId: String): PendingIntent {
            val intent = Intent(context, ModesWidgetProvider::class.java).apply {
                action = ACTION_SWITCH
                putExtra(EXTRA_MODE_ID, modeId)
                data = Uri.parse("modelawn://mode/$modeId")
            }
            return PendingIntent.getBroadcast(
                context,
                modeId.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
