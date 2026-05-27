# Modes — Files changed vs. upstream Lawnchair

Diff base: merge-base with `origin/16-dev` (`bb67e81`). Regenerate this list with:

```bash
BASE=$(git merge-base origin/16-dev modes-dev)
git diff --name-status "$BASE"..modes-dev | sort
```

The footprint is deliberately small: **almost everything is new files in a self-contained
`modes` package**, and only **11 existing files** are modified — each at a clearly commented
`// Modes:` hook.

---

## A. New files (the feature itself)

### Pure logic — `lawnchair/src/app/lawnchair/modes/core/` (no Android, unit-tested)
| File | Responsibility |
|---|---|
| `Mode.kt` | Data model: `Mode`, `ModeSchedule`, `ModeAlarm`, `ModesState`; `OFF_MODE_ID` |
| `ModesStateOps.kt` | `upsertMode` / `deleteMode` / `activate` / `activeMode` |
| `AppGating.kt` | `appsToHide()` — set math (all installed − allowed) |
| `ScheduleMath.kt` | `nextTriggerMillis()` — next fire time for a schedule |
| `ModeStore.kt` | persistence interface (StateFlow + `update`) |
| `ModeRepository.kt` | coordinates persistence with the activation side-effect |
| `WallpaperAction.kt` | `WallpaperAction` + `decideWallpaper()` — pure per-mode-wallpaper decision (apply / restore baseline / none) |

### Android integration — `lawnchair/src/app/lawnchair/modes/`
| File | Responsibility |
|---|---|
| `ModeProvider.kt` | app-scoped singletons; wires repo → engine → scheduler; widget sync |
| `PrefsModeStore.kt` | persists `ModesState` as JSON in `PreferenceManager2` (mutex-guarded) |
| `ModeEngine.kt` | `applyMode()` — writes `hiddenApps`, runs routines + wallpaper, forces reload; runs off the main thread (`Dispatchers.Default`) to avoid a mode-switch ANR |
| `ModeWallpaperController.kt` | applies the per-mode wallpaper via `setStream`; imports picked images downsampled; stashes/restores the baseline |
| `AppListProvider.kt` | enumerates installed component keys |
| `ModeWorkspaceGating.kt` | the bridge Launcher3 (Java) calls to gate workspace/folders/widgets |
| `ModeAlarmAction.kt` | sets a Clock alarm via `ACTION_SET_ALARM` |
| `ModeDndController.kt` | toggles Do Not Disturb (interruption filter) |
| `ModeGrayscaleController.kt` | toggles the accessibility daltonizer |
| `ModeScheduler.kt` | arms exact activate/deactivate alarms |
| `ModeScheduleReceiver.kt` | fires on schedule + `BOOT_COMPLETED`; re-arms |
| `widget/ModesWidgetProvider.kt` | the home-screen chip-grid widget |

### UI + gesture
| File | Responsibility |
|---|---|
| `ui/preferences/destinations/ModesPreferences.kt` | the Settings → Modes screen (Compose); includes the per-mode wallpaper picker row |
| `gestures/handlers/ModeSwitcherGestureHandler.kt` | the bottom-sheet mode switcher |

### Resources — `lawnchair/res/`
| File(s) | Purpose |
|---|---|
| `layout/modes_widget.xml`, `modes_widget_row.xml`, `modes_widget_chip.xml`, `modes_widget_spacer.xml` | widget layout |
| `layout/modes_switcher_sheet.xml` | gesture bottom-sheet layout |
| `xml/modes_widget_info.xml` | `AppWidgetProviderInfo` for the widget |
| `drawable/` + `drawable-night/`: `modes_widget_bg`, `modes_widget_chip`, `modes_widget_chip_active`, `modes_switcher_bg`, `modes_switcher_handle` | Material-You day/night chip + sheet backgrounds |
| `values/` + `values-night/`: `modes_widget_colors.xml` | day/night `@android:color/system_*` color refs |

### Tests — `lawnchair/tests-modes/`
`AppGatingTest.kt`, `ModeRepositoryTest.kt`, `ModesStateOpsTest.kt`, `ScheduleMathTest.kt`,
`WallpaperOpsTest.kt` — JUnit tests for the pure core. (Held here; run via the standalone
`logic-test` Gradle project, see `03-ai-handoff.md`. Not wired into the app module's build.)

---

## B. Modified upstream files (the integration touchpoints)

These are the only places the original Lawnchair / Launcher3 code was touched. Each edit is
small and tagged with a `// Modes:` comment.

| File | Change |
|---|---|
| **`src/com/android/launcher3/Launcher.java`** | In `bindInflatedItems`, read the active mode's hidden set once and `continue` past hidden items → **workspace gating**. |
| **`src/com/android/launcher3/folder/Folder.java`** | `animateOpen` wraps contents in `ModeWorkspaceGating.filterVisible(...)`; `shouldAnimateOpen` relaxed from `size() <= 1` to `isEmpty()` so a folder gated to one app still opens. |
| **`src/com/android/launcher3/folder/FolderIcon.java`** | `getPreviewItemsOnPage` filters the closed-folder preview through `filterVisible(...)`. |
| **`lawnchair/src/app/lawnchair/preferences2/PreferenceManager2.kt`** | Adds one preference: `modesJson` (key `modes_state_json`) — where `ModesState` is persisted. |
| **`lawnchair/src/app/lawnchair/gestures/config/GestureHandlerConfig.kt`** | Adds the `OpenModeSwitcher` gesture handler config (`@SerialName("openModeSwitcher")`). |
| **`lawnchair/src/app/lawnchair/gestures/config/GestureHandlerOption.kt`** | Registers the new handler as a selectable gesture option. |
| **`lawnchair/src/app/lawnchair/gestures/config/GestureHandlerOptions.kt`** | Adds it to the options list shown in the gesture picker. |
| **`lawnchair/src/app/lawnchair/ui/preferences/navigation/PreferenceRoutes.kt`** | Adds the top-level `Modes` route (`PreferenceRootRoute` + deep link). |
| **`lawnchair/src/app/lawnchair/ui/preferences/navigation/PreferenceNavigation.kt`** | Registers `composable<Modes> { ModesPreferences() }`. |
| **`lawnchair/src/app/lawnchair/ui/preferences/destinations/PreferencesDashboard.kt`** | Adds the top-level **Modes** tile to the Settings dashboard. |
| **`lawnchair/res/values/strings.xml`** | Adds `modes_label`, `modes_description`, `gesture_handler_open_mode_switcher`. |
| **`lawnchair/AndroidManifest.xml`** | Adds perms (`RECEIVE_BOOT_COMPLETED`, `ACCESS_NOTIFICATION_POLICY`, `SET_ALARM`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` maxSdk32, `WRITE_SECURE_SETTINGS`) and registers `ModeScheduleReceiver` + `ModesWidgetProvider`. |
| **`.github/workflows/ci.yml`** | Matrix builds both `assembleLawnWithQuickstepGithubDebug` (pkg `app.lawnchair.debug`) and `assembleLawnWithQuickstepGithubRelease` (R8-minified, pkg `app.lawnchair`) on the fork. |

> Note: `AppDrawerPreferences.kt` is **not** in the modified list — Modes briefly lived
> under App Drawer, then moved to its own top-level section, so the net diff vs. upstream is
> zero there.

### Reverting the feature
Because integration is confined to the `// Modes:` hooks above plus the self-contained
`modes` package, the feature can be removed by reverting the 11 modified files and deleting
the `modes` package + its resources. Nothing else in Lawnchair depends on it.
