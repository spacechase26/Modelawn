# Modes — documentation

The **Modes** feature: focus profiles that strictly gate which apps are visible, with
optional routines (alarm / DND / grayscale), time-based auto activate/deactivate, and a
per-mode wallpaper. Released as `v1.1.0` on branch `modes-dev`.

| Doc | What's in it |
|---|---|
| [`01-overview.md`](./01-overview.md) | What Modes is, the design philosophy, architecture, how gating works, build/CI, and gotchas. **Start here.** |
| [`02-changed-files.md`](./02-changed-files.md) | Every file added/modified vs. upstream Lawnchair, and how to regenerate the list. |
| [`03-ai-handoff.md`](./03-ai-handoff.md) | Operational playbook for maintaining/extending it: the CI build loop, reading CI via the API, the spotless style trap, change recipes. |
| [`04-upstream-proposal.md`](./04-upstream-proposal.md) | A ready-to-adapt pitch to the Lawnchair maintainers to land Modes upstream. |
| [`05-syncing-upstream.md`](./05-syncing-upstream.md) | How to pull a new upstream Lawnchair release into the fork while keeping Modes (the merge playbook + conflict hot-spots). |

## First-time setup

**Grayscale** (the per-mode "Grayscale screen" toggle) needs the `WRITE_SECURE_SETTINGS`
permission, which a normal app can't request at runtime. Grant it once over ADB.

First confirm the installed package name — it differs by build variant
(`app.lawnchair.debug` for Debug, `app.lawnchair` for Release):

```sh
adb shell pm list packages | grep lawnchair
```

Then grant the permission against **that** package:

```sh
adb shell pm grant app.lawnchair.debug android.permission.WRITE_SECURE_SETTINGS
# Release build? use the release id instead:
adb shell pm grant app.lawnchair android.permission.WRITE_SECURE_SETTINGS
```

Granting against the wrong variant fails with `Unknown package` — the usual reason the
toggle stays a no-op even after running the command. (MIUI blocks `pm grant` over
*wireless* ADB — use a USB cable or LADB.)

The toggle also only appears for a non-"Off" mode: Settings → **Modes** → tap a real
mode → scroll to **Grayscale screen**. Once granted, its subtitle changes from "Needs a
one-time ADB grant" to "Desaturates the whole screen…".

## Back up your modes

Settings → **Modes** has **Export modes to a file** / **Import modes from a file** — a
standalone `.json` backup, separate from Lawnchair's official Backup & Restore. Import is
non-destructive (modes are added, never overwritten). Wallpapers are not included.

Deeper background (original spec, plan, recon notes) lives outside the app tree at
`docs/superpowers/` in the repo root.
