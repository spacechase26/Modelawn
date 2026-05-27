/*
 * Copyright 2026, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.ui.preferences.destinations

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lawnchair.modes.ModeProvider
import app.lawnchair.modes.ModeWallpaperController
import app.lawnchair.modes.core.Mode
import app.lawnchair.modes.core.ModeRepository
import app.lawnchair.modes.core.OFF_MODE_ID
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.AppItem
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import app.lawnchair.ui.preferences.components.layout.preferenceGroupItems
import app.lawnchair.util.appComparator
import app.lawnchair.util.appsState
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEFAULT_ID = OFF_MODE_ID

/**
 * Multiple named modes. Tap a mode to activate it (and edit it below); "Off" = all apps.
 * The active non-default mode gates the drawer + home screen to its checked apps.
 */
@Composable
fun ModesPreferences(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repo = remember { ModeProvider.repository(context) }
    val scope = rememberCoroutineScope()
    val state by repo.state.collectAsStateWithLifecycle()
    val apps by appsState(comparator = appComparator)

    LaunchedEffect(Unit) {
        val off = state.modes.firstOrNull { it.id == DEFAULT_ID }
        if (off == null) {
            repo.upsert(Mode(id = DEFAULT_ID, name = "Off", icon = "🏠", allowAll = true))
        } else if (off.name != "Off") {
            repo.upsert(off.copy(name = "Off"))
        }
        if (state.activeModeId == null) repo.activate(DEFAULT_ID)
    }

    val active = state.modes.firstOrNull { it.id == state.activeModeId }

    PreferenceScaffold(
        label = "Modes",
        isExpandedScreen = LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        PreferenceLazyColumn(it) {
            items(state.modes, key = { mode -> mode.id }) { mode ->
                ModeRow(
                    title = "${mode.icon}  ${mode.name}",
                    selected = mode.id == state.activeModeId,
                    onClick = { scope.launch { repo.activate(mode.id) } },
                )
            }
            item {
                ModeRow(
                    title = "➕  Add mode",
                    selected = false,
                    onClick = {
                        val id = UUID.randomUUID().toString()
                        scope.launch {
                            repo.upsert(Mode(id = id, name = "New mode"))
                            repo.activate(id)
                        }
                    },
                    showRadio = false,
                )
            }

            if (active != null && !active.allowAll) {
                item {
                    key(active.id) {
                        var name by remember { mutableStateOf(active.name) }
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newName ->
                                name = newName
                                scope.launch { repo.upsert(active.copy(name = newName)) }
                            },
                            label = { Text("Mode name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                item {
                    val presets = listOf("🎯", "🏋️", "😴", "💼", "📚", "🎮", "🧘", "🚶", "🌙", "🏠")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(presets) { emoji ->
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier
                                    .clickable { scope.launch { repo.upsert(active.copy(icon = emoji)) } }
                                    .padding(8.dp),
                            )
                        }
                    }
                }
                item {
                    ModeWallpaperSection(active = active, repo = repo, scope = scope)
                }
                item {
                    val alarmTime = "Alarm at %02d:%02d".format(active.alarm.hour, active.alarm.minute)
                    val openTimePicker: (() -> Unit)? = if (active.alarm.enabled) {
                        {
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    scope.launch {
                                        repo.upsert(active.copy(alarm = active.alarm.copy(hour = h, minute = m)))
                                    }
                                },
                                active.alarm.hour,
                                active.alarm.minute,
                                true,
                            ).show()
                        }
                    } else {
                        null
                    }
                    SwitchPreference(
                        checked = active.alarm.enabled,
                        onCheckedChange = { on ->
                            scope.launch { repo.upsert(active.copy(alarm = active.alarm.copy(enabled = on))) }
                        },
                        label = "Set alarm when activated",
                        description = if (active.alarm.enabled) "$alarmTime — tap to change" else null,
                        onClick = openTimePicker,
                    )
                }
                item {
                    SwitchPreference(
                        checked = active.dnd == true,
                        onCheckedChange = { on ->
                            scope.launch { repo.upsert(active.copy(dnd = if (on) true else null)) }
                            if (on) {
                                val dnd = app.lawnchair.modes.ModeDndController(context)
                                if (!dnd.hasAccess()) {
                                    context.startActivity(dnd.requestAccessIntent())
                                }
                            }
                        },
                        label = "Turn on Do Not Disturb",
                        description = "Silences notifications while active (needs permission once)",
                    )
                }
                item {
                    val grayscaleReady = remember {
                        app.lawnchair.modes.ModeGrayscaleController(context).isAvailable()
                    }
                    SwitchPreference(
                        checked = active.grayscale,
                        onCheckedChange = { on ->
                            scope.launch { repo.upsert(active.copy(grayscale = on)) }
                        },
                        label = "Grayscale screen",
                        description = if (grayscaleReady) {
                            "Desaturates the whole screen while this mode is active"
                        } else {
                            "Needs a one-time ADB grant before it works"
                        },
                    )
                }
                item {
                    val schedule = active.schedule
                    val scheduleTime = "%02d:%02d".format(schedule.hour, schedule.minute)
                    val openScheduleTimePicker: (() -> Unit)? = if (schedule.enabled) {
                        {
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    scope.launch {
                                        repo.upsert(active.copy(schedule = schedule.copy(hour = h, minute = m)))
                                        ModeProvider.scheduler(context).reschedule(repo.currentState())
                                    }
                                },
                                schedule.hour,
                                schedule.minute,
                                true,
                            ).show()
                        }
                    } else {
                        null
                    }
                    SwitchPreference(
                        checked = schedule.enabled,
                        onCheckedChange = { on ->
                            scope.launch {
                                repo.upsert(active.copy(schedule = schedule.copy(enabled = on)))
                                ModeProvider.scheduler(context).reschedule(repo.currentState())
                            }
                        },
                        label = "Auto-activate",
                        description = if (schedule.enabled) "$scheduleTime on selected days — tap for time" else null,
                        onClick = openScheduleTimePicker,
                    )
                }
                item {
                    val autoOffTime = "%02d:%02d".format(active.autoOffHour, active.autoOffMinute)
                    val openAutoOffTimePicker: (() -> Unit)? = if (active.autoOffEnabled) {
                        {
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    scope.launch {
                                        repo.upsert(active.copy(autoOffHour = h, autoOffMinute = m))
                                        ModeProvider.scheduler(context).reschedule(repo.currentState())
                                    }
                                },
                                active.autoOffHour,
                                active.autoOffMinute,
                                true,
                            ).show()
                        }
                    } else {
                        null
                    }
                    SwitchPreference(
                        checked = active.autoOffEnabled,
                        onCheckedChange = { on ->
                            scope.launch {
                                repo.upsert(active.copy(autoOffEnabled = on))
                                ModeProvider.scheduler(context).reschedule(repo.currentState())
                            }
                        },
                        label = "Auto-deactivate (to Off)",
                        description = if (active.autoOffEnabled) "$autoOffTime on selected days — tap for time" else null,
                        onClick = openAutoOffTimePicker,
                    )
                }
                if (active.schedule.enabled || active.autoOffEnabled) {
                    item {
                        val sched = active.schedule
                        val toggleDay: (Int) -> Unit = { day ->
                            val newDays = sched.daysOfWeek.toMutableSet().apply {
                                if (contains(day)) remove(day) else add(day)
                            }
                            scope.launch {
                                repo.upsert(active.copy(schedule = sched.copy(daysOfWeek = newDays)))
                                ModeProvider.scheduler(context).reschedule(repo.currentState())
                            }
                        }
                        val labels = listOf("S", "M", "T", "W", "T", "F", "S")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            labels.forEachIndexed { index, label ->
                                val day = index + 1
                                val on = sched.daysOfWeek.contains(day)
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (on) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier
                                        .clickable { toggleDay(day) }
                                        .padding(8.dp),
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "Apps allowed in this mode",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                    )
                }
                preferenceGroupItems(
                    items = apps,
                    isFirstChild = true,
                ) { _, app ->
                    val appKey = app.key.toString()
                    AppItem(
                        app = app,
                        onClick = {
                            val current = state.modes.firstOrNull { it.id == active.id }
                            if (current != null) {
                                val newAllowed = current.allowedApps.toMutableSet().apply {
                                    if (contains(appKey)) remove(appKey) else add(appKey)
                                }
                                scope.launch {
                                    repo.upsert(current.copy(allowedApps = newAllowed))
                                    if (state.activeModeId == current.id) repo.activate(current.id)
                                }
                            }
                        },
                    ) {
                        Checkbox(
                            checked = active.allowedApps.contains(appKey),
                            onCheckedChange = null,
                        )
                    }
                }
                item {
                    TextButton(
                        onClick = {
                            scope.launch {
                                ModeWallpaperController.deleteFor(context, active.id)
                                repo.delete(active.id)
                                repo.activate(DEFAULT_ID)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        Text("Delete this mode")
                    }
                }
            } else {
                item {
                    Text(
                        text = "“Off” shows all apps. Tap a mode above, or add one, to gate which apps appear.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
                if (active != null) {
                    item {
                        ModeWallpaperSection(active = active, repo = repo, scope = scope)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeWallpaperSection(
    active: Mode,
    repo: ModeRepository,
    scope: CoroutineScope,
) {
    val context = LocalContext.current
    val current by rememberUpdatedState(active)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val mode = current
        scope.launch {
            val path = withContext(Dispatchers.IO) { ModeWallpaperController.importPicked(context, uri, mode.id) }
            if (path != null) {
                repo.upsert(mode.copy(wallpaperPath = path))
                // Re-apply if this mode is active so the new wallpaper takes effect now.
                if (repo.currentState().activeModeId == mode.id) repo.activate(mode.id)
            }
        }
    }
    val hasWallpaper = active.wallpaperPath != null
    Column(modifier = Modifier.fillMaxWidth()) {
        ClickablePreference(
            label = "Wallpaper",
            subtitle = if (hasWallpaper) "Image set — tap to change" else "Tap to choose an image",
            onClick = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("image/*")
                runCatching { picker.launch(intent) }
            },
        )
        if (hasWallpaper) {
            ClickablePreference(
                label = "Remove wallpaper",
                confirmationText = "Remove this mode's wallpaper?",
                onClick = {
                    val mode = current
                    scope.launch {
                        repo.upsert(mode.copy(wallpaperPath = null))
                        ModeWallpaperController.deleteFor(context, mode.id)
                        if (repo.currentState().activeModeId == mode.id) repo.activate(mode.id)
                    }
                },
            )
        }
        Text(
            text = "Tip: set Accent color to “Wallpaper” (Settings → General) so the accent follows each mode's wallpaper.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ModeRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showRadio: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showRadio) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(12.dp))
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}
