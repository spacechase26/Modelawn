package app.lawnchair.modes.core

/** What [decideWallpaper] resolves a mode activation to, for the Android layer to execute. */
sealed interface WallpaperAction {
    /**
     * Set the mode's wallpaper. When [captureBaselineFirst] is true, stash the current system
     * wallpaper first so it can be restored when no mode wallpaper is active.
     */
    data class Apply(val path: String, val captureBaselineFirst: Boolean) : WallpaperAction

    /** Restore (and clear) the stashed baseline wallpaper. */
    data object RestoreBaseline : WallpaperAction

    /** Leave the wallpaper untouched. */
    data object None : WallpaperAction
}

/**
 * Pure decision for what to do with the wallpaper on activating a mode.
 *
 * The single stashed baseline = "the wallpaper before any mode override". It is captured the first
 * time a mode wallpaper is applied (when none is stashed) and restored when activating a mode with
 * no wallpaper (e.g. plain Off).
 */
fun decideWallpaper(modeWallpaperPath: String?, hasStashedBaseline: Boolean): WallpaperAction {
    return when {
        modeWallpaperPath != null -> WallpaperAction.Apply(modeWallpaperPath, captureBaselineFirst = !hasStashedBaseline)
        hasStashedBaseline -> WallpaperAction.RestoreBaseline
        else -> WallpaperAction.None
    }
}
