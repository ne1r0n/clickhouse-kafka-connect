---
phase: 16
review: 16-REVIEW.md
titles: json
findings:
  - id: IN-01
    severity: info
    disposition: fixed
    title: "Item 8 tells the operator to merge `sync/v1.7.0`, a branch that is not published"
  - id: WR-01
    severity: warning
    disposition: open
    title: "Item 8 step 1 `git revert -m 1 <merge-commit>` conflicts on the published `custom_build` tip, and E-14 does not cover that state"
open: 1
total: 2
recorded: 2026-10-10T18:14:08.382Z
---

# Phase 16: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| IN-01 | info | fixed | fd99a476 (docs only, Phase 16) |
| WR-01 | warning | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
