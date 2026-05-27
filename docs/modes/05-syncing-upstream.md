# Syncing a new upstream Lawnchair release into Modelawn

> When official Lawnchair ships a new version you want (a bug-fix wave, or a new major like
> `17-dev`), this is how you pull it into your fork **without losing the Modes feature**.
> Written for the owner (vibe-coding) and for a future AI doing the merge.

## The mental model (read this once)

- `origin` = **upstream** Lawnchair (`LawnchairLauncher/lawnchair`). You never push here.
- `fork` = **your** repo (`spacechase26/Modelawn`). You push here.
- The Modes feature lives on branch **`modes-dev`**, which was branched off upstream's
  development branch (currently **`16-dev`**).
- Almost all of Modes is **new files** in a self-contained `modes` package; only ~11 existing
  files carry a small `// Modes:` hook (listed in [`02-changed-files.md`](./02-changed-files.md)
  §B). **Those ~11 files are the only places that can conflict** during a sync.

> **CI naming rule (important):** CI only builds branches whose name **ends in `-dev`**
> (`.github/workflows/ci.yml` triggers on `'*-dev'`). Whatever branch you sync onto must end
> in `-dev`, or you'll get no APK.

## Before you start

```bash
cd /home/coder/lawnchair-modes/launcher
git status                       # make sure your work is committed
git push fork modes-dev          # back it up to the fork first
git fetch origin                 # pull the latest upstream refs
git branch -r | grep origin      # see what upstream branches exist (e.g. origin/16-dev, origin/17-dev)
```

If upstream bumped the `platform_frameworks_libs_systemui` submodule, refresh it too:
```bash
git submodule update --init
```

---

## Case A — minor update on the SAME major (e.g. more commits on `16-dev`)

You're still on `16-dev`; you just want upstream's latest fixes.

```bash
git checkout modes-dev
git merge origin/16-dev          # brings upstream changes under your feature
# ...resolve any conflicts (see "Resolving conflicts" below)...
```

Then **build & verify** (see that section), commit, `git push fork modes-dev`.

---

## Case B — new MAJOR (e.g. upstream moves to `17-dev`)

Make a fresh branch off the new upstream base and bring Modes onto it. **Merge** (not rebase)
is recommended — conflicts are easier to reason about and you keep history.

```bash
# New branch off the new upstream major. MUST end in -dev so CI builds it.
git checkout -b modes-17-dev origin/17-dev

# Bring the whole Modes feature in:
git merge modes-dev
# ...resolve conflicts (only in the ~11 hook files; the modes/ package rarely conflicts)...

git push fork modes-17-dev       # CI builds it (name ends in -dev)
```

`modes-17-dev` is now your new mainline. Keep the old `modes-dev` around until the new one is
verified, then you can retire it.

> Prefer `merge` over `rebase` here. A rebase replays each of your commits onto the new base
> and makes you resolve conflicts repeatedly, commit-by-commit — painful across a major jump.

---

## Resolving conflicts (where they'll be)

Conflicts only happen in the files upstream also edited. For Modes those are the integration
touchpoints from [`02-changed-files.md`](./02-changed-files.md) §B — most likely:

- `src/com/android/launcher3/Launcher.java` — the `bindInflatedItems` workspace-gating hook.
- `src/com/android/launcher3/folder/Folder.java` — `animateOpen` + `shouldAnimateOpen`.
- `src/com/android/launcher3/folder/FolderIcon.java` — `getPreviewItemsOnPage`.
- The gesture-config trio + preference navigation/dashboard files.
- `lawnchair/AndroidManifest.xml`, `lawnchair/res/values/strings.xml`.
- `.github/workflows/ci.yml` — **keep your version** (the matrix that builds Debug + Release);
  re-add only genuinely new upstream steps.

**The trick:** every hook is tagged with a `// Modes:` comment. For each conflict, take
*upstream's* new version of the file and re-insert the `// Modes:` block. To find them all:

```bash
git grep -n "// Modes:"          # lists every integration hook by file + line
```

If a conflicted file is one of *our* new files (anything under `modes/`, `ModesPreferences`,
the widget/gesture, the `modes_*` resources), just keep our version — upstream doesn't have it.

---

## Build & verify after a sync

```bash
# 1. The integration points still exist (4 workspace/folder bind points + the prefs/gesture/manifest):
git grep -n "ModeWorkspaceGating"        # expect hits in Launcher.java, Folder.java, FolderIcon.java
git grep -n "modesJson"                  # PreferenceManager2 still has the pref
git grep -n "OpenModeSwitcher"           # gesture still registered

# 2. Pure logic still passes (fast, local — no device needed):
JAVA_HOME=/home/coder/java21 ./gradlew -p /home/coder/lawnchair-modes/logic-test test

# 3. Format + spotless sanity on anything you touched:
JAVA_HOME=/home/coder/java21 /home/coder/bin/ktlint --format <changed .kt files>
grep -nE " = *$" <changed .kt files>     # the spotless single-expr trap (see 03-ai-handoff.md)

# 4. Push to the *-dev branch → CI builds → install the APK artifact → test on device.
```

**Watch for:** upstream may have **renamed or removed APIs** the Modes code calls
(`PreferenceManager2`, `ColorOption`/`ThemeProvider` for the wallpaper feature, `LauncherModel`,
the gesture-handler base class, Compose preference components). The local logic-test won't catch
these — only the CI compile will. Read the failing CI job's log and fix the call sites.

---

## After it's green and verified

1. Update the doc references to the new base: the diff base in
   [`02-changed-files.md`](./02-changed-files.md) (`origin/16-dev` → `origin/17-dev`) and the
   branch names in [`03-ai-handoff.md`](./03-ai-handoff.md).
2. Tag the release at the new branch's HEAD:
   ```bash
   git tag -a v1.2.0 -m "Modes on Lawnchair 17"
   git push fork v1.2.0
   ```
3. (Optional) Point the fork's default branch at the new branch, and delete the stale one.

That's it — your Modes feature now rides the new Lawnchair version.
