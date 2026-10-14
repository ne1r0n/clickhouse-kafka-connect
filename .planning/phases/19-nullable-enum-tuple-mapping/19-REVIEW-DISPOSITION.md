---
phase: 19
review: 19-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: deferred
    title: "The totality guarantee covers only the body of `apply`, not its handler or its caller"
  - id: WR-02
    severity: warning
    disposition: deferred
    title: "Several WARN-content assertions are vacuous and cannot fail"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "FORK-DELTA states per-twin server error codes that the test does not pin"
  - id: IN-01
    severity: info
    disposition: deferred
    title: "The backslash-n constant is copied into three test classes"
  - id: IN-02
    severity: info
    disposition: deferred
    title: "Brittle and indirect test pins"
  - id: IN-03
    severity: info
    disposition: deferred
    title: "`normalise` rewrites every backslash-n pair, including ones inside names and enum labels"
  - id: CR-01
    severity: critical
    disposition: fixed
    title: "Tuple element wrapped in SimpleAggregateFunction(Nullable(...)) is flipped to non-nullable"
  - id: IN-04
    severity: info
    disposition: deferred
    title: "A Tuple Column that lacks a declared element still \"applies\""
open: 0
total: 8
recorded: 2026-10-14T13:31:33.000Z
---

# Phase 19: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | deferred | carried to the milestone close record (20-MILESTONE-CLOSE-RECORD.md): no code churn between Phase 19's green gate and the release (D-08); the finding concerns totality paths that are unreachable today (a null root or a Column with a null type), so no operator-visible behaviour depends on it |
| WR-02 | warning | deferred | carried to the milestone close record (20-MILESTONE-CLOSE-RECORD.md): no code churn between Phase 19's green gate and the release (D-08); the finding is that several WARN-content assertions are vacuous because the column name is one letter that also appears in the fixed text, so the tests still pass if the name were dropped from the line. Note: this is the current review's vacuous WARN-assertion finding; the different item ROADMAP and STATE call WR-02 (ledger L-38, from the earlier review recorded by ea20cf0a, whose ID this review reused) is fulfilled, not deferred: docs/ROLLOUT-v1.7.0_gg2.md item 2 and the 1.7.0_gg2 CHANGELOG section state it |
| WR-03 | warning | fixed | 3e39867d (docs only, Phase 20: FORK-DELTA states a read-past-end code) |
| IN-01 | info | deferred | carried to the milestone close record (20-MILESTONE-CLOSE-RECORD.md): no code churn between Phase 19's green gate and the release (D-08); a duplicated test constant for the escaped newline, with no effect on behaviour |
| IN-02 | info | deferred | carried to the milestone close record (20-MILESTONE-CLOSE-RECORD.md): no code churn between Phase 19's green gate and the release (D-08); brittle or indirect test pins (an NPE provoked through a null type, a pass-through wrapper, a misplaced constant), with no effect on behaviour |
| IN-03 | info | deferred | carried to the milestone close record (20-MILESTONE-CLOSE-RECORD.md): no code churn between Phase 19's green gate and the release (D-08); the normaliser rewrites a backslash-n inside a name or an enum label too, which fails open to DESCRIBE's nullability with a WARN, is rare and has no test |
| CR-01 | critical | fixed | 4e133d4e (19-07: DeclaredNullability unwraps SimpleAggregateFunction before reading nullability, as Column.extractColumn does; pinned by DeclaredNullabilityTest COLS-10 CR-01 and root-wrapper cases and SimpleAggregateTupleRoundTripLiveTest on both twins; LowCardinality and boundary pins and the mutation in 19-09) (not in the current review) |
| IN-04 | info | deferred | deferred, carried forward in the ROADMAP Phase 20 entry: a shortfall needs a Table pairing that drops a declared element, and no DESCRIBE shape that does so was found (the matrix, the Docker-free pins and the CR-01 live test pair every element); logging or failing open on it needs a decision on whether legitimately unpaired elements exist; owner Phase 20, which copies it into the milestone close record; trigger to re-open is a clickhouse-data version change or an observed DESCRIBE shape that drops a declared Tuple element; until then missingDeclaredElementDoesNotShiftPairing pins the current behaviour (not in the current review) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
