---
phase: 21
review: 21-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "runs-on rule rejects a pinned runner whose trailing comment mentions the floating label"
  - id: WR-02
    severity: warning
    disposition: open
    title: "`ktsCode` comment stripping is not string- or line-comment-aware"
  - id: WR-03
    severity: warning
    disposition: open
    title: "forkTaskProblem and the fork-count step rule accept text that is not live code"
  - id: IN-01
    severity: info
    disposition: open
    title: "dependabot parsing is stricter than YAML and silently depends on one indent style"
  - id: IN-02
    severity: info
    disposition: open
    title: "Fixture list duplicates the production constant"
  - id: IN-03
    severity: info
    disposition: open
    title: "FORK-DELTA BLD-14 evidence not re-established for Lombok 1.18.48"
  - id: IN-04
    severity: info
    disposition: open
    title: "slf4j-reload4j is versioned independently of the slf4j catalog entry"
open: 7
total: 7
recorded: 2026-10-18T17:22:55.477Z
---

# Phase 21: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
