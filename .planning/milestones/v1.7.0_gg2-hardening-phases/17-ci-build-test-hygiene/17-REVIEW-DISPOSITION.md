---
phase: 17
review: 17-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "Unobserved `exclude-paths` key could invalidate the whole Dependabot config, and the guard does not pin it"
  - id: IN-01
    severity: info
    disposition: open
    title: "The guard does not pin the workflow's use of `reportTestForks`"
  - id: IN-02
    severity: info
    disposition: open
    title: "Pin guard regexes are brittle in two small ways"
  - id: IN-03
    severity: info
    disposition: open
    title: "The jar-origin check in `Lz4RoundTripLiveTest` now duplicates the Docker-free unit test"
open: 4
total: 4
recorded: 2026-10-12T08:28:47.317Z
---

# Phase 17: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
