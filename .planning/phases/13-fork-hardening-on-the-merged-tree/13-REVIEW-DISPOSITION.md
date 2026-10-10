---
phase: 13
review: 13-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: deferred
    title: "Empty-CDC-record skip guard exists only on the metadata-free route; the `bypassRowBinary=true` JSON route still emits all-defaults rows"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "`io.debezium.time.Timestamp` millisecond longs pass through unconverted and are silently misread as seconds"
  - id: WR-03
    severity: warning
    disposition: deferred
    title: "The \"not nullable and no value\" failure throws a message-less `RuntimeException`, and this phase makes it reachable for `Array(Tuple)`"
  - id: WR-04
    severity: warning
    disposition: deferred
    title: "Null-deref on `e.getMessage()` in the `doInsertRawBinary` catch block can mask the real failure (pre-existing, in a high-risk file)"
  - id: WR-05
    severity: warning
    disposition: deferred
    title: "Nested nulls in `Array(Nullable(Enum))`, `Map(K, Nullable(Enum))` and `Array(Nullable(Tuple))` now fail the whole batch, including arrays that are entirely null (regression for previously working data)"
  - id: IN-01
    severity: info
    disposition: deferred
    title: "Nested-null check logic is duplicated three times (ARRAY, MAP, TUPLE)"
  - id: IN-02
    severity: info
    disposition: deferred
    title: "Expected data-quality failures are logged at ERROR with a full stack trace, once per recursion level"
  - id: IN-03
    severity: info
    disposition: deferred
    title: "`NESTED_NULL_*` constants and helpers are declared mid-class, between unrelated methods"
  - id: IN-04
    severity: info
    disposition: deferred
    title: "`writeRecordsJson` now has three nested serialization strategies in one loop body"
open: 0
total: 9
recorded: 2026-10-08T18:14:28.735Z
---

# Phase 13: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | deferred | upstream bypassRowBinary=true JSON route kept as upstream wrote it (prefer-upstream); operator-visible, listed in FORK-DELTA Known costs |
| WR-02 | warning | fixed | fixed by fix(15-01) commit 981a89d6: Debezium Timestamp and NanoTimestamp converted to decimal seconds on the metadata-free route (ledger L-20) |
| WR-03 | warning | deferred | message-less RuntimeException is an upstream line in the high-risk ClickHouseWriter; operator-visible, listed in FORK-DELTA Known costs |
| WR-04 | warning | deferred | pre-existing upstream line in a high-risk file (e.getMessage() in the doInsertRawBinary catch block); prefer-upstream, not introduced by this milestone |
| WR-05 | warning | deferred | WARN added (D-02); root cause backlog 999.1 |
| IN-01 | info | deferred | code-quality only: duplicated nested-null check, no behaviour change; refactor would add diff churn against upstream rebasing |
| IN-02 | info | deferred | code-quality only: expected data-quality failures logged at ERROR with stack trace; no behaviour change to the data path |
| IN-03 | info | deferred | code-quality only: constants declared mid-class; style move adds rebase churn for no behaviour change |
| IN-04 | info | deferred | code-quality only: writeRecordsJson has three nested serialization strategies in one loop; no behaviour change |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
