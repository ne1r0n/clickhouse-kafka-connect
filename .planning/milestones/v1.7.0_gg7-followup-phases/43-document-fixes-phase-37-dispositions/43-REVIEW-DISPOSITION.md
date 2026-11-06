---
phase: 43
review: 43-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "gg1 item 6 still overclaims that client V1 fails on the connector's `compress=true` plus `setCompressionMethod=false` combination, and contradicts FORK-DELTA"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "FORK-DELTA's closing sentence for the gg6 paragraph, and the status labels of items 12 and 19, disagree with the items Phase 43 closed"
  - id: WR-03
    severity: warning
    disposition: skipped
    title: "The repointing in FORK-DELTA is partial, so two published reproduction commands still give different numbers than the document states"
  - id: IN-01
    severity: info
    disposition: open
    title: "FORK-DELTA row 91 does not mention the pin that IN-04 added"
  - id: IN-02
    severity: info
    disposition: open
    title: "gg1 notice sentence names \"the release\" without saying which tree the measurements are from"
open: 4
total: 5
recorded: 2026-11-02T19:52:43.744Z
---

# Phase 43: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | 43-REVIEW-FIX.md |
| WR-02 | warning | fixed | 43-REVIEW-FIX.md |
| WR-03 | warning | skipped | 43-REVIEW-FIX.md |
| IN-01 | info | open | - |
| IN-02 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
