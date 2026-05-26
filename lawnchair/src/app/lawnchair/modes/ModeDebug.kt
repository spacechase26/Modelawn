package app.lawnchair.modes

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Temporary on-device diagnostics for the routines layer (alarm / DND / schedule).
 * Posts a toast on the main thread so background activation paths are observable.
 * Remove once the routines are confirmed working.
 */
internal object ModeDebug {
    fun toast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}
