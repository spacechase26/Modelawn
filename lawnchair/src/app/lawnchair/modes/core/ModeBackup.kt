package app.lawnchair.modes.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A standalone, shareable snapshot of the user's gating modes.
 *
 * This is intentionally separate from Lawnchair's official Backup & Restore so the
 * feature lives entirely in modes-owned code and never conflicts when the fork merges
 * upstream Lawnchair updates. Wallpapers are not included (only [Mode.wallpaperPath],
 * a device-local file path, exists on a Mode and it is stripped on the way in and out).
 */
@Serializable
data class ModeBackup(
    val version: Int = 1,
    val modes: List<Mode>,
)

private val backupJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

/**
 * Snapshot the current modes for export: drops the built-in "Off"/allowAll mode (it is
 * auto-recreated on every device) and strips each mode's wallpaper path.
 */
fun ModesState.toBackup(): ModeBackup = ModeBackup(
    modes = modes
        .filterNot { it.allowAll || it.id == OFF_MODE_ID }
        .map { it.copy(wallpaperPath = null) },
)

/**
 * Merge an imported backup into the current state, non-destructively: every imported
 * mode is added with a fresh id from [newId] so it can never overwrite an existing mode.
 * Any allowAll entry in the file is skipped, wallpaper paths are dropped, and the active
 * mode selection is left untouched.
 */
fun ModesState.mergeBackup(backup: ModeBackup, newId: () -> String): ModesState {
    val imported = backup.modes
        .filterNot { it.allowAll || it.id == OFF_MODE_ID }
        .map { it.copy(id = newId(), wallpaperPath = null) }
    return copy(modes = modes + imported)
}

/** Serialize a backup to pretty JSON. */
fun encodeBackup(backup: ModeBackup): String {
    return backupJson.encodeToString(ModeBackup.serializer(), backup)
}

/** Parse a backup; returns null on malformed or wrong-shape input. */
fun decodeBackup(text: String): ModeBackup? = runCatching {
    backupJson.decodeFromString(ModeBackup.serializer(), text)
}.getOrNull()
