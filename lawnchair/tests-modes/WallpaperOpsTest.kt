package app.lawnchair.modes.core

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperOpsTest {

    @Test fun noWallpaperAndNoStashDoesNothing() {
        assertEquals(WallpaperAction.None, decideWallpaper(modeWallpaperPath = null, hasStashedBaseline = false))
    }

    @Test fun noWallpaperWithStashRestores() {
        assertEquals(WallpaperAction.RestoreBaseline, decideWallpaper(modeWallpaperPath = null, hasStashedBaseline = true))
    }

    @Test fun firstWallpaperCapturesBaselineThenApplies() {
        assertEquals(
            WallpaperAction.Apply("/p.jpg", captureBaselineFirst = true),
            decideWallpaper(modeWallpaperPath = "/p.jpg", hasStashedBaseline = false),
        )
    }

    @Test fun laterWallpaperAppliesWithoutRecapturing() {
        assertEquals(
            WallpaperAction.Apply("/p.jpg", captureBaselineFirst = false),
            decideWallpaper(modeWallpaperPath = "/p.jpg", hasStashedBaseline = true),
        )
    }
}
