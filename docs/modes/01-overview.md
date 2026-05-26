# Modes — Overview & Developer Guide

> What the Modes feature is, how it's built, and the things you need to know before
> touching it. Read this first.

## 1. What it is

**Modes** is a focus / anti-doomscroll feature added to this Lawnchair fork (a.k.a.
"Modelawn"). A *mode* is a named profile (e.g. **Work**, **Gym**, **Sleep**, **Off**)
that **strictly gates which apps are visible** and optionally fires a few routines when
you switch to it. It's inspired by Samsung Modes & Routines and Apple Focus, but the
defining idea is **hard gating**: when a mode is active, apps that aren't allowed simply
aren't there — not on the home screen, not in the drawer, not in folders, not as widgets.

### Feature list (v1.0.0)

- **Named modes**: create / rename / pick an emoji / choose allowed apps / delete.
  One built-in mode, **Off**, allows everything (no gating).
- **Strict app gating** across every surface: app drawer, home-screen workspace,
  folder previews + open folders, and home-screen widgets.
- **Routines on activation**:
  - Set a one-off **alarm** in the system Clock app.
  - Toggle **Do Not Disturb** (enforced both ways — a non-DND mode turns it off).
  - Toggle system **grayscale** (accessibility daltonizer; needs an ADB-granted permission).
- **Scheduling**: per-mode **auto-activate** and **auto-deactivate** (back to Off) at chosen
  times on shared day-of-week toggles. Exact, on-time alarms; survives reboot.
- **Switchers**:
  - A **top-level Settings → Modes** section (the editor + mode list).
  - A home-screen **widget** (active-mode header + a chip grid; tap a chip to switch).
  - A bindable **gesture** (Settings → Gestures → "Open mode switcher") that pops a
    Material-You bottom sheet of mode chips.

Out of scope for v1: location-based triggers.

## 2. Design philosophy

- **Non-destructive.** Gating never deletes or moves anything. Hidden items are *skipped
  at bind time* and reappear when the mode clears. Your layout is always intact.
- **Reuse, don't reinvent.** Gating is implemented by writing Lawnchair's existing
  `hiddenApps` preference, so the drawer already does the right thing for free; the
  workspace/folders just needed thin filters at their bind points.
- **Pure core, thin Android shell.** All the real logic (data model, gating math, schedule
  math, state transitions) lives in an Android-free `…modes.core` package so it's trivially
  unit-testable. Android side-effects are isolated in small controller classes.
- **Minimal upstream footprint.** Only a handful of Launcher3/Lawnchair files are touched,
  each with a clearly commented `// Modes:` hook. See `02-changed-files.md`.

## 3. Architecture

```
app.lawnchair.modes.core         (pure Kotlin, no Android — unit-testable)
  Mode, ModeSchedule, ModeAlarm, ModesState   data model (kotlinx.serialization)
  ModesStateOps                                upsert/delete/activate/activeMode
  AppGating.appsToHide()                       set math: allInstalled − allowed
  ScheduleMath.nextTriggerMillis()             next fire time for a schedule
  ModeStore (interface), ModeRepository        persistence contract + coordinator

app.lawnchair.modes              (Android integration)
  ModeProvider          app-scoped singletons; wires repo → engine → scheduler
  PrefsModeStore        persists ModesState as JSON in PreferenceManager2 (mutex-guarded)
  ModeEngine            applyMode(): the side-effects orchestrator
  AppListProvider       enumerates installed component keys
  ModeWorkspaceGating   the bridge Launcher3 (Java) calls to gate workspace/folders/widgets
  ModeAlarmAction       sets a Clock alarm (ACTION_SET_ALARM)
  ModeDndController      toggles Do Not Disturb (interruption filter)
  ModeGrayscaleController toggles the accessibility daltonizer (WRITE_SECURE_SETTINGS)
  ModeScheduler         arms exact activate/deactivate alarms
  ModeScheduleReceiver  BroadcastReceiver: fires on schedule + BOOT_COMPLETED, re-arms
  widget/ModesWidgetProvider   the home-screen chip-grid widget

app.lawnchair.ui.preferences.destinations.ModesPreferences   the Settings UI (Compose)
app.lawnchair.gestures.handlers.ModeSwitcherGestureHandler   the gesture bottom sheet
```

### How gating actually works (the important part)

`ModeEngine.applyMode(mode)` does three things:

1. Computes `appsToHide = allInstalledComponentKeys − mode.allowedApps` (empty for an
   allow-all mode) and writes it to `PreferenceManager2.hiddenApps`.
   - The **app drawer** observes `hiddenApps` directly (Lawnchair behavior) → gated for free.
   - The **workspace** does *not*, so we additionally call `LauncherModel.forceReload()`.
     On re-bind, `Launcher.bindInflatedItems` asks `ModeWorkspaceGating.isHidden(...)` per
     item and `continue`s past hidden ones. Folders consult
     `ModeWorkspaceGating.filterVisible(...)` for their preview and open grid.
2. Runs the routine side-effects: alarm, DND (always enforced to the mode's state),
   grayscale.
3. The widget refreshes via a `repository.state` collector in `ModeProvider`.

`hiddenApps` is an **existing** Lawnchair preference — we reuse it as the single source of
truth for "what's hidden right now," which is why a mode switch is consistent everywhere.

### Scheduling

`ModeScheduler.reschedule(state)` arms, per mode, **two** alarms with distinct
`PendingIntent`s (keyed `on:<id>` / `off:<id>`):
- *activate* at `nextTriggerMillis(mode.schedule)` → activates the mode.
- *deactivate* at the auto-off time (sharing the mode's day toggles) → activates **Off**.

Alarms use `setExactAndAllowWhileIdle` when `canScheduleExactAlarms()` (the `USE_EXACT_ALARM`
permission auto-grants this on API 33+). `ModeScheduleReceiver` runs the activation and
re-arms the next occurrences; it's also registered for `BOOT_COMPLETED` so schedules survive
reboots.

## 4. Build & dev loop

- **Stack:** Astro? No — this is Android. Kotlin + Jetpack Compose (Material3) for settings,
  RemoteViews for the widget. minSdk 26, JDK 21, kotlinx.serialization, the `opto`
  preferences lib (`PreferenceManager2`).
- **The build does not run locally on small machines** (Lawnchair is large). Builds go
  through **GitHub Actions CI** (`.github/workflows/ci.yml`, trimmed to build only
  `assembleLawnWithQuickstepGithubDebug`). The loop:

  ```
  edit → commit on modes-dev → git push fork modes-dev → CI builds the debug APK
       → install the APK artifact on a device → test
  ```

- **Branches:** work happens on `modes-dev`. The fork's default branch is `16-dev`
  (Lawnchair upstream). There is **no `main`**. v1 is the `v1.0.0` tag at `modes-dev` HEAD;
  the APK is the green CI run's artifact (CI builds `*-dev`/PRs, **not tags**).

## 5. Gotchas (don't rediscover these)

- **`check-style` (spotless `intellij_idea`) is stricter than local ktlint 1.8.0.** The big
  one: a multi-line single-expression function body must keep the `=` on the signature line
  (`fun f() = expr`), never `fun f() =⏎    expr`. Prefer **block bodies** for anything
  multi-line. Scan with `grep -nE " = *$"`. The APK still *builds* when check-style fails —
  read the spotless diff from the CI log; it prints the exact fix.
- **`ACTION_SET_ALARM`** needs `com.android.alarm.permission.SET_ALARM` or it throws a
  Permission Denial.
- **DND** needs `android.permission.ACCESS_NOTIFICATION_POLICY` *declared* or the app won't
  even appear in the system "Do Not Disturb access" grant list. The user still grants access
  once.
- **Grayscale** needs `WRITE_SECURE_SETTINGS`, granted once over ADB:
  `adb shell pm grant app.lawnchair.debug android.permission.WRITE_SECURE_SETTINGS`.
  (MIUI blocks `pm grant` over *wireless* ADB; a USB cable or LADB works.) Until granted,
  grayscale is a silent no-op.
- **Exact alarms:** don't revert the trigger path to `setAndAllowWhileIdle` — inexact alarms
  caused a visible 30–60s lag in auto activate/deactivate.
- **Don't add our own widget to the hidden set.** `ModeWorkspaceGating.isHidden` exempts
  providers whose package starts with `app.lawnchair`, so the Modes widget stays visible
  during gating.
