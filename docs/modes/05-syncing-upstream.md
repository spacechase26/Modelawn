# Updating to a newer Lawnchair (beginner-friendly guide)

> Official Lawnchair released a new version you want, and you'd like your **Modes** feature to
> ride on top of it. This guide walks you through it **from scratch** — no Git expertise assumed,
> and **no special computer required**. You can do this from any laptop with Git installed; the
> actual app build happens on GitHub's servers, not on your machine.

---

## First, the calm version of what's going on

You have **two copies** of the Lawnchair code online:

- **Lawnchair's original** — `github.com/LawnchairLauncher/lawnchair`. You only ever *read* from it.
- **Your fork** — `github.com/spacechase26/Modelawn`. This is yours; it has the original code
  **plus** your Modes feature. You push your work here.

"Updating to a newer Lawnchair" just means: **take Lawnchair's new code and re-apply your Modes
changes on top of it.** The good news — your Modes feature is almost entirely in its own folder
(`modes/`), so it barely collides with Lawnchair's code. Only about **11 files** are shared, and
every shared spot is marked with a `// Modes:` comment so you can find it instantly.

### A 2-minute vocabulary (everything you need)

| Word | What it means, plainly |
|---|---|
| **repo** | A project folder tracked by Git (your code + its history). |
| **clone** | Download a repo onto your computer. |
| **remote** | A nickname for an online copy of the repo. You'll have two: one for *your* fork, one for *Lawnchair's original*. |
| **branch** | A separate line of work. Your feature lives on the branch **`modes-dev`**. |
| **commit** | A saved snapshot of changes, with a message. |
| **fetch** | Download the latest code from a remote (without changing your files yet). |
| **merge** | Combine another branch's changes into yours. |
| **conflict** | When the same lines were edited in both places and Git needs *you* to choose. (We'll handle this — it's not scary.) |
| **push** | Upload your commits to GitHub. |
| **tag** | A name pinned to a snapshot, like `v1.1.0`, marking a release. |
| **CI** | GitHub's robot that builds the app for you when you push. |

> **You never build the app on your own computer.** Lawnchair is huge. You push your code to
> GitHub, GitHub's CI builds the APK, and you download it. So all you need locally is Git.

---

## Step 0 — One-time setup on a new computer

Skip this if you already have the project on the machine you're using (just run `git remote -v`
to check your remote names — see the note in Step 2).

1. **Install Git** if you don't have it ([git-scm.com](https://git-scm.com/downloads)), and make
   sure you can log in to GitHub from the command line (SSH key or `gh auth login`).

2. **Download your fork** (the `--recurse-submodules` part matters — Lawnchair pulls in another
   sub-project):
   ```bash
   git clone --recurse-submodules git@github.com:spacechase26/Modelawn.git
   cd Modelawn
   ```

3. **Add Lawnchair's original as a second remote**, named `upstream` (the usual name for "the
   project I forked from"):
   ```bash
   git remote add upstream https://github.com/LawnchairLauncher/lawnchair.git
   ```

4. **Check your remotes** so you know their names:
   ```bash
   git remote -v
   ```
   You want to see two: `origin` → your `Modelawn`, and `upstream` → `LawnchairLauncher/lawnchair`.

> ⚠️ **Names can differ!** On the temporary VPS this was set up the *other* way around: there
> `origin` = Lawnchair and `fork` = your Modelawn. **Always run `git remote -v` first** and use
> whatever names *you* see. In this guide: **`upstream`** = Lawnchair's original, **`origin`** =
> your Modelawn. Swap the names if yours are different.

---

## Step 1 — Save your current work first (always)

Make sure nothing is half-finished, and back up your branch to GitHub before you start:

```bash
git status            # should say "nothing to commit, working tree clean"
git checkout modes-dev
git push origin modes-dev
```

If `git status` shows changes you care about, commit them first:
```bash
git add -A
git commit -m "wip: save before updating Lawnchair"
git push origin modes-dev
```

---

## Step 2 — Get Lawnchair's latest code

This downloads the new code but doesn't touch your files yet:

```bash
git fetch upstream
git submodule update --init        # in case Lawnchair bumped its sub-project
```

See which versions Lawnchair has:
```bash
git branch -r | grep upstream
```
You'll see things like `upstream/16-dev`, maybe `upstream/17-dev`. Lawnchair develops on a branch
named after the Android version (e.g. `16-dev`). **The newer number is the new version.**

---

## Step 3 — Bring your Modes feature onto the new version

> **One rule that bites people:** GitHub's CI only builds branches whose name **ends in `-dev`**.
> If your branch doesn't end in `-dev`, you'll get no APK. So name it e.g. `modes-17-dev`.

Say the new version is `17-dev`. Create a fresh branch from Lawnchair's new code, then merge your
feature into it:

```bash
# Make a new branch that starts as an exact copy of Lawnchair 17:
git checkout -b modes-17-dev upstream/17-dev

# Pour your Modes feature on top:
git merge modes-dev
```

One of two things happens:

- **"Merge made by the 'recursive' strategy" / no conflicts** 🎉 → skip to Step 5.
- **"CONFLICT (content): ..."** → Git needs your help on a few files. Go to Step 4. Don't panic.

> Just want the latest fixes on the **same** version (still `16-dev`, no new number)? It's the
> same idea, simpler: `git checkout modes-dev` then `git merge upstream/16-dev`.

---

## Step 4 — Fixing conflicts (gently)

A conflict just means Lawnchair *and* you both edited the same lines, and Git won't guess. List
the files that need attention:

```bash
git status        # the files under "Unmerged paths" are the ones to fix
```

Open each one. Git inserts markers that look like this:

```
<<<<<<< HEAD
   ...Lawnchair's new version of these lines...
=======
   ...your version of these lines...
>>>>>>> modes-dev
```

Your job: edit that section so it has the **correct final code**, then delete the three marker
lines (`<<<<<<<`, `=======`, `>>>>>>>`).

**The shortcut that makes this easy:** every place your Modes feature touches Lawnchair's code is
labelled. Find them all with:

```bash
git grep -n "// Modes:"
```

So for a conflict, the recipe is almost always: **keep Lawnchair's new version, then paste your
`// Modes:` block back in.** The list of exactly which files have these hooks and what each hook
does is in [`02-changed-files.md`](./02-changed-files.md) (section B). They're mostly:
`Launcher.java`, `Folder.java`, `FolderIcon.java`, a few gesture/preference files, the Android
manifest, and `strings.xml`. (For `.github/workflows/ci.yml`, keep **your** version.)

If a conflicted file is one of *your own* new files (anything in the `modes/` folder, the widget,
the settings screen), just keep your version — Lawnchair doesn't have it.

After fixing each file, tell Git it's resolved:
```bash
git add <the-file-you-fixed>
```
When all are added:
```bash
git commit          # finishes the merge (a default message is fine — just save & close)
```

> **Stuck or it looks like a lot?** This is the one genuinely technical step. You don't have to do
> it alone — hand the job to an AI assistant (like me) and point it at this folder's
> [`03-ai-handoff.md`](./03-ai-handoff.md) and [`02-changed-files.md`](./02-changed-files.md). To
> abandon a messy merge and start over, run `git merge --abort` — it puts everything back exactly
> as it was. Nothing is lost.

---

## Step 5 — Let GitHub build it, then test

Push your new branch. Because its name ends in `-dev`, GitHub's robot builds an APK automatically:

```bash
git push origin modes-17-dev
```

Then, **in your web browser**:
1. Go to your repo → the **Actions** tab → click the latest **CI** run.
2. Wait for the green check (≈10–15 min). If it's **red**, open the failed job and read the
   error — usually Lawnchair renamed something your code uses; fix that line, commit, push again.
3. Scroll to **Artifacts** at the bottom → download
   **`assembleLawnWithQuickstepGithubRelease`** (that's the optimized app).
4. Unzip it, put the `.apk` on your phone, and install. Check that switching modes, gating, and
   wallpapers all still work.

> A quick sanity check you *can* run from the command line (optional, only if you have a dev
> setup): the pure logic tests. On the VPS that was
> `JAVA_HOME=/home/coder/java21 ./gradlew -p /home/coder/lawnchair-modes/logic-test test`. It
> doesn't need a phone or the full build. Skip it if you're not set up for it — CI is the real check.

---

## Step 6 — Make it official

Once it's green and works on your phone, this new branch is your new home base. Mark the release:

```bash
git tag -a v1.2.0 -m "Modes on Lawnchair 17"
git push origin v1.2.0
```

(Optional, on the GitHub website: Settings → Branches → set `modes-17-dev` as the default branch,
and delete the old `modes-dev` once you're happy.)

That's the whole thing. Your Modes feature now runs on the new Lawnchair. 🎉

---

## Cheat sheet (once you've done it once)

```bash
git remote -v                                   # what are my remote names?
git checkout modes-dev && git push origin modes-dev   # back up first
git fetch upstream && git submodule update --init     # get new Lawnchair
git checkout -b modes-17-dev upstream/17-dev          # new branch off it (must end in -dev)
git merge modes-dev                             # add your feature
#   ...if conflicts: fix files (git grep "// Modes:"), git add, git commit...
git push origin modes-17-dev                    # GitHub builds the APK
#   ...test the APK from the Actions tab...
git tag -a v1.2.0 -m "..." && git push origin v1.2.0
```

**Escape hatch:** `git merge --abort` undoes a merge in progress and puts everything back.
