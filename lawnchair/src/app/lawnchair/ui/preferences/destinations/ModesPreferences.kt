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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lawnchair.modes.ModeProvider
import app.lawnchair.modes.core.Mode
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.AppItem
import app.lawnchair.ui.preferences.components.controls.SwitchPreference
import app.lawnchair.ui.preferences.components.layout.PreferenceLazyColumn
import app.lawnchair.ui.preferences.components.layout.PreferenceScaffold
import app.lawnchair.ui.preferences.components.layout.preferenceGroupItems
import app.lawnchair.util.appComparator
import app.lawnchair.util.appsState
import java.util.UUID
import kotlinx.coroutines.launch

private const val DEFAULT_ID = "default"

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
        if (state.modes.none { it.id == DEFAULT_ID }) {
            repo.upsert(Mode(id = DEFAULT_ID, name = "Off (all apps)", icon = "🏠", allowAll = true))
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
                        label = "Auto-activate daily",
                        description = if (schedule.enabled) "Every day at $scheduleTime — tap to change" else null,
                        onClick = openScheduleTimePicker,
                    )
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
            }
        }
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
