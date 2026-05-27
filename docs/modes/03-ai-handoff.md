# Modes — AI maintenance & handoff guide

> For a future AI (or human) picking this up to fix or extend. Read `01-overview.md` for the
> architecture first; this doc is the operational playbook.

## Environment (as built)

- **Repo:** `git@github.com:spacechase26/Modelawn.git`, remote name `fork`. Upstream
  Lawnchair is remote `origin` (`LawnchairLauncher/lawnchair`).
- **Local clone:** `/home/coder/lawnchair-modes/launcher`.
- **Work branch:** `modes-dev`. Default branch on the fork is `16-dev` (upstream tracking).
  No `main`. Released tag: `v1.1.0` (per-mode wallpaper + the mode-switch ANR fix).
- **Toolchain:** JDK 21 at `/home/coder/java21`; ktlint 1.8.0 at `/home/coder/bin/ktlint`.
- **The host cannot compile the full app** (Lawnchair is too big for this VM). All real
  builds happen on **GitHub Actions**.
- **Spec/plan/recon notes:** `/home/coder/lawnchair-modes/docs/superpowers/`.

## The build/test loop

```bash
# 1. edit Kotlin/XML
# 2. format + sanity-check style locally
JAVA_HOME=/home/coder/java21 /home/coder/bin/ktlint --format <changed .kt files>
grep -nE " = *$" <changed .kt files>     # catch the spotless single-expr trap (see below)
# 3. commit on modes-dev and push to the fork
git add -A && git commit -m "…"          # NO Co-Authored-By trailers; author harendra12912@gmail.com
git push fork modes-dev                  # triggers CI (ci.yml builds the Debug + Release variants)
# 4. watch CI, then have the device-owner install the APK artifact and test
```

There is **no emulator** on this host (no `/dev/kvm`). On-device testing is done by the
repo owner. Pure logic, however, *can* be tested here — see "Logic tests" below.

### Watching CI with the API

A **read-only** GitHub token lives at `/home/coder/.modelawn_token` (treat as secret; never
print or commit it). See `[[reference_modelawn_token]]` in memory.

```bash
TOKEN=$(cat /home/coder/.modelawn_token)
# Latest runs on the branch — note BOTH the numeric `id` and the human `run_number`:
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://api.github.com/repos/spacechase26/Modelawn/actions/runs?branch=modes-dev&per_page=5" \
  | python3 -c "import sys,json;d=json.load(sys.stdin);[print(r['id'],r['run_number'],r['status'],r['conclusion'],r['head_sha'][:7],r['name']) for r in d['workflow_runs']]"
```

> **Gotcha I hit:** `…/actions/runs/{X}` takes the **`id`** (the big number), *not* the
> `run_number` you see in the UI as `#22`. Using the run_number returns `404 Not Found`.
>
> **Second gotcha:** `…/actions/runs?branch=modes-dev` lists runs from **all** workflows,
> including the **Crowdin** job that fires on the same push (and finishes instantly as
> `skipped`). Filter to the build: pick the run whose `name == 'CI'` for your `head_sha`,
> then poll that `id`.

Poll a run to completion, then fetch its APK artifact:

```bash
RID=<run id>
curl -s -H "Authorization: Bearer $TOKEN" \
  "https://api.github.com/repos/spacechase26/Modelawn/actions/runs/$RID/artifacts" \
  | python3 -c "import sys,json;d=json.load(sys.stdin);[print(a['name'],a['archive_download_url']) for a in d['artifacts']]"
# The token is read-only: you can READ runs/logs and DOWNLOAD artifacts, but you CANNOT
# push via the API, create releases, or upload assets. Pushing commits/tags works over SSH.
```

On a red build, dump the failing job's log and read the spotless/compiler error:

```bash
curl -s -L -H "Authorization: Bearer $TOKEN" \
  "https://api.github.com/repos/spacechase26/Modelawn/actions/runs/$RID/logs" -o /tmp/ci.zip
```

### Logic tests (run locally, fast)

The pure `modes.core` logic compiles without Android. A standalone Gradle project exists for
it:

```bash
JAVA_HOME=/home/coder/java21 ./gradlew -p /home/coder/lawnchair-modes/logic-test test
```

When you change anything in `modes/core` (gating math, schedule math, state ops), add/update
a test in `lawnchair/tests-modes/` and run it here before pushing — it's far faster than a
CI round-trip.

## The #1 style trap (causes most red builds)

CI's `check-style` runs **spotless with the `intellij_idea` Kotlin style**, which is
*stricter* than the local ktlint 1.8.0 (`ktlint_official`). The recurring failure:

```kotlin
// ❌ spotless rejects a single-expression body wrapped after '='
fun foo() =
    someLongExpression()

// ✅ keep '=' on the signature line, OR use a block body for multi-line
fun foo() = someLongExpression()
fun foo() { return someLongExpression() }
```

Local ktlint will pass these and CI will still fail. **Prefer block bodies for anything
multi-line**, and grep `" = *$"` before pushing. The APK itself still compiles when
check-style fails, so if you only need to verify a behavior, the artifact is usable — but
keep the tree green.

## Common change recipes

- **Add a routine side-effect** (e.g. toggle Wi-Fi/Bluetooth): write a small controller in
  `modes/`, add a field to `Mode` (with a default for back-compat), call it from
  `ModeEngine.applyMode`, add a switch in `ModesPreferences.kt`. Declare any permission in
  `AndroidManifest.xml`.
- **Add a field to `Mode`:** give it a default value so old persisted JSON still decodes
  (`PrefsModeStore` uses `ignoreUnknownKeys = true` and `encodeDefaults = true`). No
  migration needed for additive fields.
- **Change gating behavior:** it all flows from `appsToHide` + the `hiddenApps` write in
  `ModeEngine`. Workspace/folder/widget gating is in `ModeWorkspaceGating` (called from the
  Java hooks). Keep it **non-destructive** — filter at bind/display, never mutate the DB.
- **Touch scheduling:** `ScheduleMath.nextTriggerMillis` is pure (unit-test it).
  `ModeScheduler` arms `on:`/`off:` alarms. Keep exact alarms (`setExactAndAllowWhileIdle`)
  — inexact ones reintroduce the 30–60s lag.
- **Add a per-mode visual / side-effect that touches Lawnchair theming** (like the wallpaper):
  drive it from `ModeEngine.applyMode` via a small controller, and remember **icons/accent only
  re-tint on a model reload** — set the visual *before* `forceReload()`. Keep heavy work off the
  main thread (applyMode is already `Dispatchers.Default`).
- **Syncing a new upstream Lawnchair release:** see [`05-syncing-upstream.md`](./05-syncing-upstream.md)
  for the full merge playbook. In short: the risk areas are the ~11 `// Modes:` hook files
  (`Launcher.java`, `Folder.java`, `FolderIcon.java`, the gesture-config + preference-navigation
  files); the `modes` package is self-contained and rarely conflicts. After merging, re-verify
  the four bind points (`git grep ModeWorkspaceGating`) and build on a `*-dev` branch.

## Known limitations / good next tasks

- A gated folder's **badge count** can still include hidden apps (cosmetic).
- **No location triggers** (intentionally out of scope for v1).
- **Per-mode wallpaper:** third-party home-screen **widgets can't be recolored** by the launcher
  (OS-drawn from system Material You) — only Lawnchair's own UI + themed icons follow the
  wallpaper, and only with Accent = "Wallpaper". Baseline revert is best-effort (some OEMs block
  reading the current wallpaper); the reliable revert is to give **Off** its own wallpaper. Themed
  icons may lag the wallpaper by a beat if `getWallpaperColors()` updates async — if so, trigger
  the reload from a wallpaper-colors-changed callback instead of inline after `setStream`.
- Switching *away* from a mode that set a **Clock alarm** doesn't cancel that alarm (we only
  create it via `ACTION_SET_ALARM`; we don't own it).
- The widget/switcher chip text can't be made bold per-chip via RemoteViews; the active chip
  uses a stronger background + a `●` marker instead.
- `tests-modes/` isn't wired into the app module's Gradle build — only into the standalone
  `logic-test` project. Wiring it into a real `testDebugUnitTest` task would let CI run it.

## House rules

- Commits: **no `Co-Authored-By` trailers**; author email **harendra12912@gmail.com**.
- Don't push to `16-dev`. Work on `modes-dev`; tag releases (`vX.Y.Z`) at its HEAD.
- The owner vibe-codes — keep change descriptions to 1–2 lines and explain *what changed*.
