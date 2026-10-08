---
phase: 13
review: 13-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "Empty-CDC-record skip guard exists only on the metadata-free route; the `bypassRowBinary=true` JSON route still emits all-defaults rows"
  - id: WR-02
    severity: warning
    disposition: open
    title: "`io.debezium.time.Timestamp` millisecond longs pass through unconverted and are silently misread as seconds"
  - id: WR-03
    severity: warning
    disposition: open
    title: "The \"not nullable and no value\" failure throws a message-less `RuntimeException`, and this phase makes it reachable for `Array(Tuple)`"
  - id: WR-04
    severity: warning
    disposition: open
    title: "Null-deref on `e.getMessage()` in the `doInsertRawBinary` catch block can mask the real failure (pre-existing, in a high-risk file)"
  - id: WR-05
    severity: warning
    disposition: open
    title: "Nested nulls in `Array(Nullable(Enum))`, `Map(K, Nullable(Enum))` and `Array(Nullable(Tuple))` now fail the whole batch, including arrays that are entirely null (regression for previously working data)"
  - id: IN-01
    severity: info
    disposition: open
    title: "Nested-null check logic is duplicated three times (ARRAY, MAP, TUPLE)"
  - id: IN-02
    severity: info
    disposition: open
    title: "Expected data-quality failures are logged at ERROR with a full stack trace, once per recursion level"
  - id: IN-03
    severity: info
    disposition: open
    title: "`NESTED_NULL_*` constants and helpers are declared mid-class, between unrelated methods"
  - id: IN-04
    severity: info
    disposition: open
    title: "`writeRecordsJson` now has three nested serialization strategies in one loop body"
open: 9
total: 9
recorded: 2026-10-08T18:14:28.735Z
---

# Phase 13: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| WR-04 | warning | open | - |
| WR-05 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| IN-04 | info | open | - |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
