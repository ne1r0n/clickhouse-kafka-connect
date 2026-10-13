---
phase: 19
review: 19-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: open
    title: "The totality guarantee covers only the body of `apply`, not its handler or its caller"
  - id: WR-02
    severity: warning
    disposition: open
    title: "Several WARN-content assertions are vacuous and cannot fail"
  - id: WR-03
    severity: warning
    disposition: open
    title: "FORK-DELTA states per-twin server error codes that the test does not pin"
  - id: IN-01
    severity: info
    disposition: open
    title: "The backslash-n constant is copied into three test classes"
  - id: IN-02
    severity: info
    disposition: open
    title: "Brittle and indirect test pins"
  - id: IN-03
    severity: info
    disposition: open
    title: "`normalise` rewrites every backslash-n pair, including ones inside names and enum labels"
  - id: CR-01
    severity: critical
    disposition: fixed
    title: "Tuple element wrapped in SimpleAggregateFunction(Nullable(...)) is flipped to non-nullable"
  - id: IN-04
    severity: info
    disposition: deferred
    title: "A Tuple Column that lacks a declared element still \"applies\""
open: 6
total: 8
recorded: 2026-10-13T10:38:14.394Z
---

# Phase 19: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | open | - |
| WR-02 | warning | open | - |
| WR-03 | warning | open | - |
| IN-01 | info | open | - |
| IN-02 | info | open | - |
| IN-03 | info | open | - |
| CR-01 | critical | fixed | 4e133d4e (19-07: DeclaredNullability unwraps SimpleAggregateFunction before reading nullability, as Column.extractColumn does; pinned by DeclaredNullabilityTest COLS-10 CR-01 and root-wrapper cases and SimpleAggregateTupleRoundTripLiveTest on both twins; LowCardinality and boundary pins and the mutation in 19-09) (not in the current review) |
| IN-04 | info | deferred | deferred, carried forward in the ROADMAP Phase 20 entry: a shortfall needs a Table pairing that drops a declared element, and no DESCRIBE shape that does so was found (the matrix, the Docker-free pins and the CR-01 live test pair every element); logging or failing open on it needs a decision on whether legitimately unpaired elements exist; owner Phase 20, which copies it into the milestone close record; trigger to re-open is a clickhouse-data version change or an observed DESCRIBE shape that drops a declared Tuple element; until then missingDeclaredElementDoesNotShiftPairing pins the current behaviour (not in the current review) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
