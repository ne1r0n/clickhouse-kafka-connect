---
phase: 19
review: 19-REVIEW.md
titles: json
findings:
  - id: CR-01
    severity: critical
    disposition: fixed
    title: "Tuple element wrapped in SimpleAggregateFunction(Nullable(...)) is flipped to non-nullable"
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "\"No exception may escape\" holds only for the parse; `collect` is unguarded and describeTableV1 does not catch RuntimeException"
  - id: WR-02
    severity: warning
    disposition: deferred
    title: "Operator-facing release documents still describe the removed WARN and the unfixed limitation"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "Control test passes on any exception"
  - id: IN-01
    severity: info
    disposition: fixed
    title: "Fallback WARN has no database or table context"
  - id: IN-02
    severity: info
    disposition: skipped
    title: "Dead STORES_WRONG machinery in the matrix test"
  - id: IN-03
    severity: info
    disposition: fixed
    title: "Test re-implements production normalisation and duplicates helpers"
  - id: IN-04
    severity: info
    disposition: deferred
    title: "A Tuple Column that lacks a declared element still \"applies\""
open: 0
total: 8
recorded: 2026-10-12T21:49:16.780Z
---

# Phase 19: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| CR-01 | critical | fixed | 4e133d4e (19-07: DeclaredNullability unwraps SimpleAggregateFunction before reading nullability, as Column.extractColumn does; pinned by DeclaredNullabilityTest COLS-10 CR-01 and root-wrapper cases and SimpleAggregateTupleRoundTripLiveTest on both twins; LowCardinality and boundary pins and the mutation in 19-09) |
| WR-01 | warning | fixed | 88e4e854 (19-08: apply is total; any RuntimeException falls back with one metadata-only WARN; pinned by unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning) |
| WR-02 | warning | deferred | deferred to Phase 20 (ROLL-02, PUB-07), carried forward in the ROADMAP Phase 20 entry: the cited text is in the published 1.7.0_gg1 rollout and CHANGELOG, where it is historically accurate; the 1.7.0_gg2 rollout note and CHANGELOG section must say the limitation is closed and the WARN is gone, that rows stored by earlier releases are not repaired, and carry ledger L-38 (a single-field Nullable(Tuple) with a null field now throws a DataException, which stops the task under errors.tolerance=none) |
| WR-03 | warning | fixed | 448a5ba5 (19-08: the control asserts a read-past-end server code and logs the matched code per twin) |
| IN-01 | info | fixed | 88e4e854 (19-08: the fallback WARN names database and table; pinned by fallbackWarningNamesDatabaseAndTable) |
| IN-02 | info | skipped | skipped: NullableTupleMatrixLiveTest is the permanent D-05 record and STORES_WRONG is the outcome kind a future regression cell pins; the unused branch asserts nothing today and removing it changes no behaviour while churning an 800-line test |
| IN-03 | info | fixed | 448a5ba5 (19-08: NullableRoundTrip delegates to the public DeclaredNullability.normalise; one shared ColumnNullabilityPaths flattener) |
| IN-04 | info | deferred | deferred, carried forward in the ROADMAP Phase 20 entry: a shortfall needs a Table pairing that drops a declared element, and no DESCRIBE shape that does so was found (the matrix, the Docker-free pins and the CR-01 live test pair every element); logging or failing open on it needs a decision on whether legitimately unpaired elements exist; owner Phase 20, which copies it into the milestone close record; trigger to re-open is a clickhouse-data version change or an observed DESCRIBE shape that drops a declared Tuple element; until then missingDeclaredElementDoesNotShiftPairing pins the current behaviour |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
