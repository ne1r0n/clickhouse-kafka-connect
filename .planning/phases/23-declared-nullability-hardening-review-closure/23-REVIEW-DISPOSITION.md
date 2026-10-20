---
phase: 23
review: 23-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "The escaped-quote branch of `normalise` is not pinned by any test"
  - id: WR-02
    severity: warning
    disposition: open
    title: "`discoverAsDescribeTableV2` copies the production DESCRIBE loop into the test"
  - id: IN-01
    severity: info
    disposition: open
    title: "`apply` Javadoc still says \"all or none\""
  - id: IN-02
    severity: info
    disposition: open
    title: "Null-name check sits inside the `try` and logs a null column slot"
  - id: IN-03
    severity: info
    disposition: open
    title: "Live-test premise loops can pass vacuously, and lookups can NPE instead of failing with a message"
  - id: IN-04
    severity: info
    disposition: open
    title: "The unpaired-element WARN repeats at every refresh for a known, unfixed defect"
open: 6
total: 6
recorded: 2026-10-20T12:29:36.081Z
---

# Phase 23: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
