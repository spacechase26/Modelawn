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
 * When the launcher's accent color is set to "Wallpaper", changing the wallpaper here also recolors
 * the launcher accent live via Material You (see ThemeProvider's OnColorsChangedListener).
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

    private fun setWallpaperFromFile(path: String) {
        val bitmap = decodeSampled(path) ?: return
        runCatching { wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM) }
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
                java.io.FileInputStream(pfd.fileDescriptor).use { input ->
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

    private fun targetSize(): Pair<Int, Int> {
        val w = wallpaperManager.desiredMinimumWidth.takeIf { it > 0 } ?: 1080
        val h = wallpaperManager.desiredMinimumHeight.takeIf { it > 0 } ?: 1920
        return w to h
    }

    private fun decodeSampled(path: String): Bitmap? {
        val (tw, th) = targetSize()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply { inSampleSize = computeInSampleSize(bounds, tw, th) }
        return BitmapFactory.decodeFile(path, opts)
    }

    companion object {
        private const val DIR_NAME = "mode_wallpapers"
        private const val STASH_NAME = "_baseline.jpg"

        private fun dir(context: Context): File = File(context.filesDir, DIR_NAME)

        private fun fileFor(context: Context, modeId: String): File {
            val safe = modeId.replace(Regex("[^A-Za-z0-9_-]"), "_")
            return File(dir(context), "wp_$safe.jpg")
        }

        /** Copy + downsample a picked image into app storage. Returns the stored path, or null. */
        fun importPicked(context: Context, uri: Uri, modeId: String): String? = runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val wm = WallpaperManager.getInstance(context)
            val tw = wm.desiredMinimumWidth.takeIf { it > 0 } ?: 1080
            val th = wm.desiredMinimumHeight.takeIf { it > 0 } ?: 1920
            val opts = BitmapFactory.Options().apply { inSampleSize = computeInSampleSize(bounds, tw, th) }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val file = fileFor(context, modeId)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
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
