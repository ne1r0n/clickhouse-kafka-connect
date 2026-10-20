---
phase: 19
review: 19-REVIEW.md
titles: json
findings:
  - id: WR-01
    severity: warning
    disposition: fixed
    title: "The totality guarantee covers only the body of `apply`, not its handler or its caller"
  - id: WR-02
    severity: warning
    disposition: fixed
    title: "Several WARN-content assertions are vacuous and cannot fail"
  - id: WR-03
    severity: warning
    disposition: fixed
    title: "FORK-DELTA states per-twin server error codes that the test does not pin"
  - id: IN-01
    severity: info
    disposition: fixed
    title: "The backslash-n constant is copied into three test classes"
  - id: IN-02
    severity: info
    disposition: fixed
    title: "Brittle and indirect test pins"
  - id: IN-03
    severity: info
    disposition: fixed
    title: "`normalise` rewrites every backslash-n pair, including ones inside names and enum labels"
  - id: CR-01
    severity: critical
    disposition: fixed
    title: "Tuple element wrapped in SimpleAggregateFunction(Nullable(...)) is flipped to non-nullable"
  - id: IN-04
    severity: info
    disposition: fixed
    title: "A Tuple Column that lacks a declared element still \"applies\""
open: 0
total: 8
recorded: 2026-10-14T13:31:33.000Z
---

# Phase 19: Code Review Disposition

| Finding | Severity | Disposition | Source |
|---------|----------|-------------|--------|
| WR-01 | warning | fixed | 4bbc9627 (COLS-12: apply and containsTuple are total for a null root and a null-type Column; DeclaredNullabilityTest COLS-12 pins and the D-08 flip, 23-EVIDENCE.md E-02); previously deferred (20-MILESTONE-CLOSE-RECORD.md) |
| WR-02 | warning | fixed | 075e6466 (DEBT-10: sentinel root and delimited column segment; every column assertion seen red against both D-17 mutations, 23-EVIDENCE.md E-03, and for the unpaired WARN E-05); previously deferred (20-MILESTONE-CLOSE-RECORD.md). Note: this is the current review's vacuous WARN-assertion finding; the different item ROADMAP and STATE call WR-02 (ledger L-38, from the earlier review recorded by ea20cf0a, whose ID this review reused) is fulfilled, not deferred: docs/ROLLOUT-v1.7.0_gg2.md item 2 and the 1.7.0_gg2 CHANGELOG section state it |
| WR-03 | warning | fixed | 3e39867d (docs only, Phase 20: FORK-DELTA states a read-past-end code) |
| IN-01 | info | fixed | 4478016a (DEBT-11: DeclaredNullability.ESCAPED_NEWLINE is public and the three test copies are gone, 23-EVIDENCE.md E-01); previously deferred (20-MILESTONE-CLOSE-RECORD.md) |
| IN-02 | info | fixed | 252cf8f3 (DEBT-11: the NullableRoundTrip pass-through is gone and DISCOVERED_TABLE sits in the constants block) and 4bbc9627 (D-08: the NPE-provoking test is flipped to the structure mismatch reason with a written rationale, 23-EVIDENCE.md E-02); previously deferred (20-MILESTONE-CLOSE-RECORD.md) |
| IN-03 | info | fixed | f55165ee (COLS-13: the quote-aware normalise keeps a backslash-n inside quoted names and labels; Docker-free pins on the live capture and the NullableTupleMatrixLiveTest live premise, 23-EVIDENCE.md E-04); previously deferred (20-MILESTONE-CLOSE-RECORD.md) |
| CR-01 | critical | fixed | 4e133d4e (19-07: DeclaredNullability unwraps SimpleAggregateFunction before reading nullability, as Column.extractColumn does; pinned by DeclaredNullabilityTest COLS-10 CR-01 and root-wrapper cases and SimpleAggregateTupleRoundTripLiveTest on both twins; LowCardinality and boundary pins and the mutation in 19-09) (not in the current review) |
| IN-04 | info | fixed | f83ac147 (COLS-14, D-01: a Tuple Column never legitimately lacks a declared element, so the pass logs one metadata-only WARN and still applies; the re-open trigger fired when R-01 observed DESCRIBE dropping an element named size or size0, now backlog 999.4; missingDeclaredElementDoesNotShiftPairing unchanged and green, 23-EVIDENCE.md E-05); previously deferred (ROADMAP Phase 20 entry) |

Dispositions: `open` (recorded, not yet triaged), `fixed`, `skipped`, `deferred`.
Set `deferred` by hand and put the reason in the Source cell; both are preserved. A `|` in the reason is kept as prose and escaped on the next run.
Re-running the gate keeps every row it can. A row the current review no longer reports is kept and its Source cell flagged, so a finding does not leave this record silently. ONE exception: when a finding id is REUSED by a different finding, the earlier decision cannot keep a row — the id is taken — and it is dropped. A RECORDED decision (anything but `open`) is named on the console when that happens; a row still at `open` is replaced silently, because `open` records no decision to lose.
