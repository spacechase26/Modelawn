# Proposal: add "Modes" (focus profiles with strict app gating) to Lawnchair

> A ready-to-adapt pitch for the Lawnchair maintainers — paste/trim this into a GitHub
> **Discussion** (Ideas) or a feature issue on `LawnchairLauncher/lawnchair`, and offer the
> branch as a PR. Written from the contributor's point of view.

---

Hi Lawnchair team 👋

I've built a feature I'd love to contribute upstream: **Modes** — named focus profiles that
**strictly gate which apps are visible**, with a few optional routines. It's been running on
my fork ([Modelawn], branch `modes-dev`, tag `v1.0.0`) and is stable on-device.

## Why

The "digital wellbeing" tools that ship with stock launchers (Samsung Modes & Routines,
Apple Focus) are about *nudging*. What actually helps people who fight doomscrolling is
**hard gating**: when you're in **Work** or **Sleep**, the distracting apps simply aren't
reachable from the launcher. Lawnchair already has a "hidden apps" mechanism — Modes
generalizes it into switchable, schedulable profiles. It's the kind of power-user control
Lawnchair's audience tends to want, and I couldn't find it in any open-source launcher.

## What it adds (user-facing)

- **Named modes** (e.g. Work / Gym / Sleep / **Off**) with an emoji and an allowed-app set.
  `Off` = everything visible (no gating).
- **Strict gating** across the drawer, workspace, folders (preview + open), and widgets —
  **non-destructively** (nothing is moved/deleted; items reappear when the mode clears).
- **Routines on activation** (all opt-in): set a Clock alarm, toggle Do Not Disturb, toggle
  system grayscale.
- **Scheduling**: per-mode auto-activate and auto-deactivate (→ Off) on day-of-week toggles,
  with exact, reboot-surviving alarms.
- **Switchers**: a top-level Settings section, a home-screen widget (chip grid), and a
  bindable gesture (a Material-You bottom sheet).

## How it's designed to be mergeable

I tried hard to keep the upstream footprint tiny and idiomatic:

- **Self-contained package.** Almost all code is new files under
  `app.lawnchair.modes` (+ a pure, Android-free `modes.core` with unit tests). Nothing in
  the launcher depends back on it.
- **Reuses existing infra.** Gating works by writing Lawnchair's existing `hiddenApps`
  preference, so the drawer needs zero new code. Persistence is one new `PreferenceManager2`
  entry.
- **Only ~11 existing files touched**, each at a small, commented `// Modes:` hook:
  - `Launcher.java` `bindInflatedItems` — skip hidden items on the workspace.
  - `Folder.java` / `FolderIcon.java` — filter open/preview folder contents; allow a
    single-item gated folder to open.
  - gesture-config trio — register an "Open mode switcher" gesture.
  - preference navigation/dashboard — add the Modes settings section.
  - `AndroidManifest.xml`, `strings.xml` — perms, receiver/widget, labels.
- **Opt-in everything.** With only the default `Off` mode, behavior is identical to stock
  Lawnchair. Routines that need sensitive permissions (DND, `WRITE_SECURE_SETTINGS` for
  grayscale) are off until the user grants them.

A full file-by-file breakdown is in [`02-changed-files.md`](./02-changed-files.md), and the
architecture in [`01-overview.md`](./01-overview.md).

## Open questions for maintainers (happy to adjust)

1. **Scope for a first PR.** I can split this: a minimal "profiles + gating" PR first, then
   routines/scheduling/widget as follow-ups. Which slice would you want to review first?
2. **`USE_EXACT_ALARM`.** It gives on-time scheduling but Google Play restricts which apps
   may declare it. Acceptable for the F-Droid/GitHub builds? Should the exact-alarm path be
   feature-flagged or fall back to inexact for Play?
3. **Grayscale via `WRITE_SECURE_SETTINGS`.** It's ADB-granted only; I've gated it so it's a
   no-op without the permission. Keep it, or drop it from an upstream version?
4. **Settings placement & naming.** I made "Modes" a top-level section — would you prefer it
   nested, or named differently?
5. **Coding conventions.** I've matched ktlint/spotless and the existing patterns; let me
   know your preferences on the new `modes` package layout before I open the PR.

I'm glad to rebase onto current `16-dev`, write up the PR(s), and iterate on review.

Thanks for Lawnchair — it's a joy to build on. 🙏

[Modelawn]: https://github.com/spacechase26/Modelawn/tree/modes-dev
