---
phase: 23-declared-nullability-hardening-review-closure
plan: "02"
subsystem: testing
tags: [java, junit5, declared-nullability, null-totality, review-closure, fail-open]

requires:
  - phase: 19-nullable-enum-tuple-mapping
    provides: DeclaredNullability and the 19-REVIEW WR-01 / WR-02 / IN-02 findings this plan closes
  - phase: 23-declared-nullability-hardening-review-closure
    provides: 23-01 public DeclaredNullability.ESCAPED_NEWLINE and the E-01 evidence index
provides:
  - DeclaredNullability.apply and containsTuple are total for a null root and a Column with a null type at any depth
  - every null in the Column tree (type, tupleFields entry, field name, Tuple name, root name) is a (structure mismatch) fallback WARN that leaves the root untouched
  - DeclaredNullabilityFixtures (ROOT_SENTINEL, assertWarnNamesColumn) for plan 23-03's DEBT-10 conversion
  - 23-BEHAVIOUR-LEDGER.md with L-49 and evidence entry E-02
affects: [23-03 DEBT-10, 23-04 COLS-13, 23-05 COLS-14, 23-06 phase gate, 24 CHANGELOG / FORK-DELTA / ROLL-03]

actuals:
  tokens: 9700
  tasks: 3
  commits: 2
plan_head_before: d2dd9586d31c475cc039f96b2b6268ab3e68d5fa
plan_head_after: 17d9ac901d3f4a2318acf41988cbff4bd4eaf9f9

tech-stack:
  added: []
  patterns:
    - "A WARN-content test asserts the delimited column segment first (assertWarnNamesColumn), so a column-slot mutation fails on it"
    - "A hand-built Column tree with every node nullable makes 'root untouched' a meaningful assertion, because a pass that ran would flip the flags"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java
    - .planning/phases/23-declared-nullability-hardening-review-closure/23-BEHAVIOUR-LEDGER.md
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md

key-decisions:
  - "A null root name is a structure mismatch (reason structure mismatch, column slot null), not IllegalArgumentException (RESEARCH Open Question 2)"
  - "The D-08 flip keeps the method name and null-type input and replaces only the NullPointerException class-name assertion with (structure mismatch), under a scoped AGENTS.md exemption comment"
  - "Shared pin tails live in test helpers (onlyWarning, assertMismatchWarning) but every pin calls assertWarnNamesColumn itself as its first content assertion"

patterns-established:
  - "Guards only, no new production symbol: null checks in containsTuple, apply, collect and collectTuple plus a local columnName"

requirements-completed: [COLS-12, DEBT-10]

coverage:
  - id: D1
    description: "apply and containsTuple return without throwing for a null root and a Column with a null type at the root, as an Array element and as a Map value (19-REVIEW WR-01)"
    requirement: COLS-12
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#COLS-12 (D-05), COLS-12 (D-06): red on the unchanged code (rc=1, 3 failed), green after (38 tests, 0 failures)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Every null that threw a NullPointerException, plus a null root name and a null-named element Tuple, is a (structure mismatch) fallback with one metadata-only WARN and the root untouched; kept shapes (Array root without element, Map root without value) and the D-09 catch path are pinned unchanged"
    requirement: COLS-12
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#COLS-12 (D-07) / (D-09) pins: 8 shape pins red on the unchanged code, 3 kept and D-09 pins green on both (rc=1, 11 failed of 38)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The NPE-provoking pin is flipped in place, not deleted, with a written rationale and a scoped AGENTS.md exemption (ROADMAP SC1, D-08)"
    requirement: COLS-12
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#unexpectedExceptionInThePassFailsOpenWithOneMetadataWarning (COLS-12 (D-08 flip))"
        status: pass
    human_judgment: false
  - id: D4
    description: "The sentinel root and the delimited column assertion exist before DEBT-10 needs them, and the FORK-DELTA clause, the COLS-12 anchor row and ledger row L-49 land in the same commit"
    requirement: DEBT-10
    verification:
      - kind: other
        ref: "fork-anchors mirror rows=66 bad=0; fix(23-02) commit holds exactly six files; spotless numstat 46 files md5 f2e32c7b"
        status: pass
    human_judgment: false

duration: 14min
completed: 2026-10-20
status: complete
---

# Phase 23 Plan 02: DeclaredNullability Null Totality Summary

**DeclaredNullability.apply and containsTuple no longer throw for a null root or a null-typed Column anywhere in the tree: every null becomes a (structure mismatch) fallback with one metadata-only WARN, the NPE-provoking pin is flipped in place under a scoped AGENTS.md exemption, and the sentinel root plus delimited column assertion are introduced as DeclaredNullabilityFixtures (COLS-12, 19-REVIEW WR-01 / IN-02)**

## Performance

- **Duration:** 14 min
- **Started:** 2026-10-20T11:32:00Z
- **Completed:** 2026-10-20T11:50:00Z
- **Tasks:** 3
- **Files modified:** 7 (2 created, 5 modified)

## Accomplishments
- `containsTuple` and `collect` return false for a null Column or a null type; `apply` returns false with no log for a null root; `collectTuple` rejects a null Tuple name, a null field and a null field name (so an element Tuple with a null name can no longer pair a field literally named `null.a`).
- A null root name now gives `(structure mismatch)` with the column slot `null`, replacing `(IllegalArgumentException)`; `root.getName()` is read once into `columnName` and all three fallback WARN calls pass it.
- Tracer pins (D-05, D-06) and the D-08 flip were red on the unchanged code (rc=1, exactly 3 failed of 27), then green. The expansion run on the unchanged code failed exactly the 8 D-07 shape pins plus the 3 tracer pins (11 of 38), while the 2 kept-behaviour pins and the D-09 pin passed on both versions.
- `DeclaredNullabilityFixtures` holds `ROOT_SENTINEL`, `COLUMN_SEGMENT_FORMAT`, `COLUMN_SEGMENT_FAILURE` and `assertWarnNamesColumn`; 11 lines in the test class call it.
- FORK-DELTA COLS-10 row carries the D-07 clause and `COLS-10, COLS-12`; anchors row added (mirror rows=66 bad=0); ledger row L-49 written.

## Task Commits

1. **Task 1: tracer (sentinel fixture, D-05/D-06 pins, D-08 flip, production guards)** - folded into `4bbc9627` (fix)
2. **Task 2: expansion (seven D-07 shapes, idempotency, kept pins, D-09 pin)** - folded into `4bbc9627` (fix)
3. **Task 3: FORK-DELTA clause, anchors row, ledger with L-49, gates, E-02** - `4bbc9627` (fix, six files) and `17d9ac90` (docs, E-02 evidence)

The plan calls for one `fix(23-02)` commit (Phase 19 D-13 same-commit rule), so Tasks 1 and 2 are not committed separately.

**Plan metadata:** the `docs(23-02)` close-out commit that holds this SUMMARY (and, in the following commit, STATE / ROADMAP / REQUIREMENTS).

## Files Created/Modified
- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java` - null guards in `containsTuple`, `apply`, `collect`, `collectTuple`; Javadoc updated
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java` - shared sentinel root and delimited column assertion
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` - 13 new testcases (25 to 38) and the D-08 flip
- `docs/FORK-DELTA.md` - COLS-10 row: D-07 clause and `COLS-10, COLS-12`
- `.planning/scripts/fork-anchors.txt` - Phase 23 comment and the COLS-12 row (count 2)
- `.planning/phases/23-declared-nullability-hardening-review-closure/23-BEHAVIOUR-LEDGER.md` - new, L-49
- `.planning/phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md` - E-02

## Decisions Made
- A null root name is a structure mismatch rather than `IllegalArgumentException` (RESEARCH Open Question 2); the null-root-name pin asserts the column segment `null` (documented exception to D-16, the null name being the input under test).
- Shared pin tails are in helpers (`onlyWarning`, `assertMismatchWarning`, `nullableNode`, `flags`) for DRY, but each pin keeps `assertWarnNamesColumn` as its own first content assertion so plan 23-03's column-slot mutations fail on it.
- Hand-built trees mark every node nullable so that "root untouched" is a real assertion (a pass that ran on the plain declared types would flip the flags to false).

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- A zsh-specific pitfall in my own scratch check: the anchors-mirror loop from the plan uses a variable named `path`, which zsh ties to `PATH`. Run under zsh it clobbered `PATH` for the rest of that one command; I re-ran the mirror under `bash -c` (rows=66 bad=0) and the commit steps in a fresh shell. No repository content was affected.
- No OpenJ9 exit 255 occurred in any Gradle run.

## Known Stubs

None.

## Threat Flags

None - no new network, auth, file-access or schema surface; the change only removes exceptions from an existing in-process pass.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Plan 23-03 (DEBT-10) can convert the older WARN-content assertions to `assertWarnNamesColumn` and rely on `ROOT_SENTINEL` and on `FALLBACK_WARNING, columnName, ` standing contiguous in each of the three WARN calls.
- Full `testUnit`: 808 tests, 806 succeeded, 0 failed, 2 skipped (baseline 795 plus the 13 new testcases).

## Self-Check: PASSED

- Created files exist: DeclaredNullabilityFixtures.java, 23-BEHAVIOUR-LEDGER.md, this SUMMARY.
- Commits `4bbc9627` and `17d9ac90` are ancestors of HEAD.
- Task acceptance gates printed TRACER-SEEN-RED, TRACER-GREEN, EXPANSION-DISCRIMINATES, EXPANSION-GREEN, NO-MUTABLE-STATIC-STATE, SPOTLESS-BASELINE-EQUAL, anchors mirror rows=66 bad=0, and the ledger / evidence check.

---
*Phase: 23-declared-nullability-hardening-review-closure*
*Completed: 2026-10-20*
