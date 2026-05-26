package app.lawnchair.modes

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Toggles system-wide grayscale (the accessibility daltonizer in monochrome mode).
 *
 * Requires `WRITE_SECURE_SETTINGS`, which a normal app can't request at runtime — it's granted
 * once over ADB:
 *   `adb shell pm grant <packageName> android.permission.WRITE_SECURE_SETTINGS`
 * Until then [isAvailable] is false and [setEnabled] is a no-op.
 */
class ModeGrayscaleController(private val context: Context) {

    fun isAvailable(): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun setEnabled(enabled: Boolean) {
        if (!isAvailable()) return
        runCatching {
            val resolver = context.contentResolver
            if (enabled) {
                // 0 = MONOCHROMACY (full grayscale).
                Settings.Secure.putInt(resolver, DALTONIZER_MODE, 0)
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 1)
            } else {
                Settings.Secure.putInt(resolver, DALTONIZER_ENABLED, 0)
            }
        }
    }

    private companion object {
        const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
        const val DALTONIZER_MODE = "accessibility_display_daltonizer"
    }
}
