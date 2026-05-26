package app.lawnchair.modes

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import com.android.launcher3.util.ComponentKey

/** Supplies all launchable apps as ComponentKey strings (matching the drawer filter format). */
interface AppListProvider {
    fun allComponentKeys(): Set<String>
}

class LauncherAppListProvider(private val context: Context) : AppListProvider {
    override fun allComponentKeys(): Set<String> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val keys = HashSet<String>()
        for (user in userManager.userProfiles) {
            for (activity in launcherApps.getActivityList(null, user)) {
                keys.add(ComponentKey(activity.componentName, user).toString())
            }
        }
        return keys
    }
}
