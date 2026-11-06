---
phase: 41
review: 41-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "Insert-timeout documentation now describes a mechanism that is a no-op while `async=false`"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "`setInsertSocketTimeoutV2` is public and only clamps the upper bound"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "The operator-override guard's parse edge cases are mostly unpinned"
  - id: IN-01
    severity: info
    disposition: open
    title: "Three identical two-line `InsertSettings` setups"
  - id: IN-02
    severity: info
    disposition: open
    title: "Redundant blank check in `operatorNamesSocketTimeout`"
  - id: IN-03
    severity: info
    disposition: open
    title: "Stall-pin `POST` count assertion depends on the stub having recorded the request"
open: 4
total: 6
recorded: 2026-11-02T10:14:30.494Z
---

# Phase 41: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | 41-REVIEW-FIX.md |
| WR-02 | warning | fixed | 41-REVIEW-FIX.md |
| WR-03 | warning | fixed | 41-REVIEW-FIX.md |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
