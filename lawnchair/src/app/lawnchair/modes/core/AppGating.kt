package app.lawnchair.modes.core

/**
 * Component keys to hide so only [mode]'s allowed apps remain visible in the drawer.
 * Returns empty (hide nothing) for an allow-all mode.
 */
fun appsToHide(allComponentKeys: Set<String>, mode: Mode): Set<String> {
    if (mode.allowAll) return emptySet()
    return allComponentKeys - mode.allowedApps
}
