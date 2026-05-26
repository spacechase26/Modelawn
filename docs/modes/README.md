# Modes — documentation

The **Modes** feature: focus profiles that strictly gate which apps are visible, with
optional routines (alarm / DND / grayscale) and time-based auto activate/deactivate.
Released as `v1.0.0` on branch `modes-dev`.

| Doc | What's in it |
|---|---|
| [`01-overview.md`](./01-overview.md) | What Modes is, the design philosophy, architecture, how gating works, build/CI, and gotchas. **Start here.** |
| [`02-changed-files.md`](./02-changed-files.md) | Every file added/modified vs. upstream Lawnchair, and how to regenerate the list. |
| [`03-ai-handoff.md`](./03-ai-handoff.md) | Operational playbook for maintaining/extending it: the CI build loop, reading CI via the API, the spotless style trap, change recipes. |
| [`04-upstream-proposal.md`](./04-upstream-proposal.md) | A ready-to-adapt pitch to the Lawnchair maintainers to land Modes upstream. |

Deeper background (original spec, plan, recon notes) lives outside the app tree at
`docs/superpowers/` in the repo root.
