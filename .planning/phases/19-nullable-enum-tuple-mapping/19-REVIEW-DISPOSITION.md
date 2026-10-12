---
phase: 19
review: 19-REVIEW.md
titles: json
findings:
  - id: CR-01
    severity: critical
    disposition: open
    title: "Tuple element wrapped in SimpleAggregateFunction(Nullable(...)) is flipped to non-nullable"
  - id: WR-01
    severity: warning
    disposition: open
    title: "\"No exception may escape\" holds only for the parse; `collect` is unguarded and describeTableV1 does not catch RuntimeException"
  - id: WR-02
    severity: warning
    disposition: open
    title: "Operator-facing release documents still describe the removed WARN and the unfixed limitation"
  - id: WR-03
    severity: warning
    disposition: open
    title: "Control test passes on any exception"
  - id: IN-01
    severity: info
    disposition: open
    title: "Fallback WARN has no database or table context"
  - id: IN-02
    severity: info
    disposition: open
    title: "Dead STORES_WRONG machinery in the matrix test"
  - id: IN-03
    severity: info
    disposition: open
    title: "Test re-implements production normalisation and duplicates helpers"
  - id: IN-04
    severity: info
    disposition: open
    title: "A Tuple Column that lacks a declared element still \"applies\""
open: 8
total: 8
recorded: 2026-10-12T21:49:16.780Z
---

# Phase 19: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| CR-01 | critical | open | - |
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
