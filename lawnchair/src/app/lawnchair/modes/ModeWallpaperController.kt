package app.lawnchair.modes

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import app.lawnchair.modes.core.WallpaperAction
import app.lawnchair.modes.core.decideWallpaper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Applies a mode's per-mode wallpaper as the home-screen wallpaper ([WallpaperManager.FLAG_SYSTEM]).
 *
 * Keeps a single stashed "baseline" (the wallpaper before any mode override): it's captured the
 * first time a mode wallpaper is applied and restored when activating a mode with no wallpaper
 * (e.g. plain Off). Capture is best-effort — some OEMs block reading the current wallpaper, in
 * which case the reliable way to revert is to give the Off mode its own wallpaper.
 *
 * Images are imported at screen size (see [importPicked]) and applied via [WallpaperManager.setStream]
 * so the system decodes from the small stored JPEG — keeping our peak memory low on low-RAM devices.
 *
 * When the launcher's accent color is set to "Wallpaper", changing the wallpaper here also recolors
 * the launcher accent (and themed icons, on the next reload) via Material You.
 */
class ModeWallpaperController(private val context: Context) {

    private val wallpaperManager = WallpaperManager.getInstance(context)
    private val stashFile = File(dir(context), STASH_NAME)

    suspend fun apply(path: String?) = withContext(Dispatchers.IO) {
        when (val action = decideWallpaper(path, stashFile.exists())) {
            is WallpaperAction.Apply -> {
                if (action.captureBaselineFirst) captureBaseline()
                setWallpaperFromFile(action.path)
            }

            WallpaperAction.RestoreBaseline -> restoreBaseline()

            WallpaperAction.None -> Unit
        }
    }

    /** Set the wallpaper by streaming the file — the framework decodes/scales, not us. */
    private fun setWallpaperFromFile(path: String) {
        val file = File(path)
        if (!file.exists()) return
        runCatching {
            FileInputStream(file).use { stream ->
                wallpaperManager.setStream(stream, null, true, WallpaperManager.FLAG_SYSTEM)
            }
        }
    }

    private fun restoreBaseline() {
        if (!stashFile.exists()) return
        setWallpaperFromFile(stashFile.absolutePath)
        stashFile.delete()
    }

    /** Best-effort: copy the current system wallpaper into the stash file. */
    private fun captureBaseline() {
        stashFile.parentFile?.mkdirs()
        val viaFile = runCatching {
            wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)?.use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { input ->
                    FileOutputStream(stashFile).use { output -> input.copyTo(output) }
                }
                true
            } ?: false
        }.getOrDefault(false)
        if (viaFile) return
        // Fallback: render the current wallpaper drawable to a bitmap.
        runCatching {
            val drawable = wallpaperManager.drawable
            val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return
            FileOutputStream(stashFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        }
    }

    companion object {
        private const val DIR_NAME = "mode_wallpapers"
        private const val STASH_NAME = "_baseline.jpg"
        private const val MAX_DIMENSION = 2048

        private fun dir(context: Context): File = File(context.filesDir, DIR_NAME)

        private fun fileFor(context: Context, modeId: String): File {
            val safe = modeId.replace(Regex("[^A-Za-z0-9_-]"), "_")
            return File(dir(context), "wp_$safe.jpg")
        }

        /**
         * Copy a picked image into app storage, downsampled to ~screen size in RGB_565 so the
         * stored JPEG is small and applying it later costs little memory. Returns the path, or null.
         */
        fun importPicked(context: Context, uri: Uri, modeId: String): String? = runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val dm = context.resources.displayMetrics
            val tw = dm.widthPixels.coerceIn(1, MAX_DIMENSION)
            val th = dm.heightPixels.coerceIn(1, MAX_DIMENSION)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = computeInSampleSize(bounds, tw, th)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val file = fileFor(context, modeId)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()

        /** Delete a mode's stored wallpaper file (on remove or mode deletion). */
        fun deleteFor(context: Context, modeId: String) {
            runCatching { fileFor(context, modeId).delete() }
        }

        private fun computeInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
}
