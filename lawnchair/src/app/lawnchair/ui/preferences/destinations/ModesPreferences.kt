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

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

private const val FOCUS_ID = "focus"
private const val DEFAULT_ID = "default"

/**
 * v1 Modes screen: a single "Focus" mode plus a Default (all-apps) mode.
 * Toggle Focus on -> the drawer shows only the checked apps (strict gating).
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
            repo.upsert(Mode(id = DEFAULT_ID, name = "Default", icon = "🏠", allowAll = true))
        }
        if (state.modes.none { it.id == FOCUS_ID }) {
            repo.upsert(Mode(id = FOCUS_ID, name = "Focus", icon = "🎯"))
        }
        if (state.activeModeId == null) repo.activate(DEFAULT_ID)
    }

    val focus = state.modes.firstOrNull { it.id == FOCUS_ID }
    val allowed = focus?.allowedApps ?: emptySet()
    val focusActive = state.activeModeId == FOCUS_ID

    PreferenceScaffold(
        label = "Modes",
        isExpandedScreen = LocalIsExpandedScreen.current,
        modifier = modifier,
    ) {
        PreferenceLazyColumn(it) {
            item {
                SwitchPreference(
                    checked = focusActive,
                    onCheckedChange = { on ->
                        scope.launch { repo.activate(if (on) FOCUS_ID else DEFAULT_ID) }
                    },
                    label = "Focus mode",
                    description = "When on, the drawer shows only the apps checked below",
                )
            }
            preferenceGroupItems(
                items = apps,
                isFirstChild = true,
            ) { _, app ->
                val key = app.key.toString()
                AppItem(
                    app = app,
                    onClick = {
                        val current = state.modes.firstOrNull { mode -> mode.id == FOCUS_ID }
                        if (current != null) {
                            val newAllowed = current.allowedApps.toMutableSet().apply {
                                if (contains(key)) remove(key) else add(key)
                            }
                            scope.launch {
                                repo.upsert(current.copy(allowedApps = newAllowed))
                                if (state.activeModeId == FOCUS_ID) repo.activate(FOCUS_ID)
                            }
                        }
                    },
                ) {
                    Checkbox(
                        checked = allowed.contains(key),
                        onCheckedChange = null,
                    )
                }
            }
        }
    }
}
