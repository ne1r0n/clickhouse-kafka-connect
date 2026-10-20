---
phase: 23-declared-nullability-hardening-review-closure
plan: "04"
subsystem: testing
tags: [declared-nullability, tuple, normalise, quote-aware-scan, clickhouse-describe, junit5, live-premise]

requires:
  - phase: 23-declared-nullability-hardening-review-closure
    provides: "23-01 shared public ESCAPED_NEWLINE; 23-02 null-total apply/containsTuple; 23-03 delimited fallback-WARN column assertions"
provides:
  - "Quote-aware DeclaredNullability.normalise: only the server's line break becomes a space, a backslash-n inside a backtick name or single-quoted label is kept"
  - "DeclaredNullabilityFixtures: twelve captured DDL/RAW constants plus the two element suffixes, shared by the Docker-free pins and the live premise"
  - "D-12 live premise: twin round trip and a DESCRIBE-only drift oracle that compares every Docker-free literal with the server's text"
  - "FORK-DELTA COLS-10 row D-10 clause, two COLS-13 anchors rows, ledger row L-50, evidence entry E-04"
affects: [23-05, phase-24-changelog, phase-24-fork-delta, ROLL-03]

actuals:
  tokens: 11000
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns:
    - "One linear quote-state scan instead of a regex or a parser for escaped DESCRIBE text"
    - "Docker-free pin inputs and the live premise compare against the same constants, so they cannot drift"

key-files:
  created: []
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/23-declared-nullability-hardening-review-closure/23-BEHAVIOUR-LEDGER.md
    - .planning/phases/23-declared-nullability-hardening-review-closure/23-EVIDENCE.md

key-decisions:
  - "normalise tracks one quote state; a backslash run inside quotes is copied with the next character and closes the quote only when half the run length is even"
  - "Wrapper columns (Array(Tuple), Map(String, Tuple)) are checked for no-WARN only: research captured no raw literal for them"
  - "The U+00E9 edge is written as a Java escape so the pin does not depend on the compiler's source encoding"

patterns-established:
  - "Captured-literal fixtures with a live drift oracle: a mismatch stops the phase (D-13), the constant is never edited to match"

requirements-completed: [COLS-13]

coverage:
  - id: D1
    description: "A literal backslash-n inside a backtick-quoted Tuple element name survives normalise and pairs without the fallback WARN (Docker-free)"
    requirement: COLS-13
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#COLS-13 (D-10): a literal backslash-n inside a backtick-quoted element name survives normalise and pairs without a fallback WARN"
        status: pass
    human_judgment: false
  - id: D2
    description: "Real-newline names, enum labels, names on two levels, names ending in a backslash and the Nullable(Tuple) literal pin the exact normalise output and pair with zero WARN events"
    requirement: COLS-13
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest COLS-13 (D-10) pins and COLS-13 (D-10) edges"
        status: pass
    human_judgment: false
  - id: D3
    description: "Every type string with no backslash inside quotes normalises exactly as the old plain replace did"
    requirement: COLS-13
    verification:
      - kind: unit
        ref: "DeclaredNullabilityTest#COLS-13 (D-10) unchanged: {0} normalises exactly as the plain replace did (6 cases)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Live round trip of Nullable(Tuple(`a\\\\nb` Int32, d String)) on the PLAIN and DEFAULT twins: raw type equals the Docker-free constant, no WARN, typed read-back 7 and nm"
    requirement: COLS-13
    verification:
      - kind: integration
        ref: "NullableTupleMatrixLiveTest#backslashNInTupleElementNameRoundTripsWithoutFallback (2 twins, ClickHouse 26.9.19.9, client V2)"
        status: pass
    human_judgment: false
  - id: D5
    description: "DESCRIBE-only drift oracle: the raw type of the five multi-line columns equals its constant, and describeTable logs no WARN including the Array and Map wrappers"
    requirement: COLS-13
    verification:
      - kind: integration
        ref: "NullableTupleMatrixLiveTest#describeReturnsTheCapturedBackslashNEscapingAndPairs"
        status: pass
    human_judgment: false
  - id: D6
    description: "The six live classes that exercise the pass stay green on latest x V2 (81 testcases) and the whole testUnit suite is green"
    requirement: COLS-13
    verification:
      - kind: integration
        ref: "live-six-23-04: NullableTupleMatrixLiveTest 33, SimpleAggregateTupleRoundTripLiveTest 2, NullableEnumRoundTripLiveTest 6, NestedNullRowBinaryLiveTest 10, NestedNullableCompositeWriteTest 18, ClickHouseHelperClientForkLiveTest 12"
        status: pass
      - kind: unit
        ref: "./gradlew testUnit --rerun: 822 tests, 820 succeeded, 0 failed, 2 skipped"
        status: pass
    human_judgment: false

plan_head_before: 819c22f8dacb5b66b733a31e745badaa201cc60b
plan_head_after: e9e520e84473a6a17d1d1328d4aef4f94693f38d
commits: 2

duration: 35min
completed: 2026-10-20
status: complete
---

# Phase 23 Plan 04: Quote-aware normalise for backslash-n in Tuple names and enum labels Summary

**`DeclaredNullability.normalise` is now one quote-aware pass, so a literal backslash-n inside a backtick-quoted Tuple element name pairs and the declared nullability applies without the fallback WARN; the server's text is re-checked on every live run against the same constants the Docker-free pins use.**

## Performance

- **Duration:** about 35 min
- **Completed:** 2026-10-20
- **Tasks:** 3
- **Files modified:** 8 (7 in the `fix(23-04)` commit, 1 evidence file in the `docs(23-04)` commit)

## Accomplishments

- Production: `normalise` keeps its signature and no longer calls the plain replace. It tracks backtick and single-quote state. Outside quotes a lone backslash-n (`startsWith(ESCAPED_NEWLINE, i)`) becomes a space. Inside quotes a backslash run is copied with the next character, which closes the quote only when half the run length is even. It is total (empty string, unterminated backtick) and linear, and runs once per root per DESCRIBE.
- Docker-free pins: seven red-before `COLS-13 (D-10)` testcases (literal backslash-n name, real-newline name, enum label, names on two levels, trailing-backslash names, the `Nullable(Tuple)` literal, edges incl. U+00E9) and six `unchanged` cases that pass on both the old and the new code.
- Live premise (D-12): twin round trip (both red on the unchanged code on the fallback WARN, never on the raw-type premise) and the DESCRIBE-only drift oracle for the five multi-line shapes plus the Array and Map wrappers.
- The D-13 contingency was not triggered: the server's line break is a lone backslash-n outside quotes, and every backslash run inside quotes is even. The captured literals matched ClickHouse 26.9.19.9.
- Bookkeeping in the same commit: FORK-DELTA COLS-10 row (D-10 clause, `COLS-10, COLS-12, COLS-13`), two COLS-13 anchors rows (mirror rows=68 bad=0), ledger row L-50, then E-04.

## Task Commits

1. **Task 1 (tracer): backslash-n element name end to end** - not committed on its own (plan: the tracer, expansion and bookkeeping share one commit)
2. **Task 2: other captured shapes, edges, unchanged-shapes pin, drift oracle** - not committed on its own
3. **Task 3: FORK-DELTA, anchors, ledger, six live classes, commit** - `f55165ee8397d0574a81abec348761fef8fb7e67` (fix)

**Evidence:** `e9e520e84473a6a17d1d1328d4aef4f94693f38d` (docs, E-04). **Plan metadata:** the summary and state commits that follow.

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullability.java` - quote-aware `normalise`, new `BACKSLASH`, `BACKTICK`, `SINGLE_QUOTE`, `NO_QUOTE` char constants
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityFixtures.java` - twelve `*_DDL`/`*_RAW` constants and the `BACKSLASH_N_ELEMENT`/`REAL_NEWLINE_ELEMENT` suffixes
- `src/test/java/com/clickhouse/kafka/connect/sink/db/mapping/DeclaredNullabilityTest.java` - 12 new testcases (39 to 52) through one shared `assertCapturedShapePairs` helper
- `src/test/java/com/clickhouse/kafka/connect/sink/NullableTupleMatrixLiveTest.java` - twin round trip and drift oracle (30 to 33 testcases)
- `docs/FORK-DELTA.md`, `.planning/scripts/fork-anchors.txt`, `23-BEHAVIOUR-LEDGER.md` (L-50), `23-EVIDENCE.md` (E-04)

## Decisions Made

- Followed the plan's D-10 rules unchanged; added a private `NO_QUOTE` char constant beside the three planned ones so the quote state has no magic zero.
- The `unchanged` pin asserts against `s.replace(ESCAPED_NEWLINE, " ")`, which is correct there because those strings have no backslash inside quotes; the five defect pins use literal expected strings.

## Deviations from Plan

None - plan executed exactly as written.

Notes that are not deviations: the plan's EXPANSION verify greps the test file for the character `é`, while the must-have asks for a Java escape. The edge pin uses `é` and a comment names the character, so both hold.

## Issues Encountered

None. No OpenJ9 GPF occurred; the live runs and the full `testUnit` passed on the first attempt.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path, file access or schema surface; the change is confined to a pure string function. T-23-08 and T-23-10 mitigations are the pins, the live round trip, the six green live classes and the standing live premise.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- COLS-13 closes ROADMAP Phase 23 SC2. L-50 is queued for the Phase 24 CHANGELOG, FORK-DELTA and ROLL-03 text.
- Nothing is pushed. `.planning/state.json` and `.planning/milestone.lock` were left untouched.

## Self-Check: PASSED

- Key files exist on disk (DeclaredNullability.java, DeclaredNullabilityFixtures.java, DeclaredNullabilityTest.java, NullableTupleMatrixLiveTest.java, docs/FORK-DELTA.md).
- Commits `f55165ee8397d0574a81abec348761fef8fb7e67` and `e9e520e84473a6a17d1d1328d4aef4f94693f38d` are ancestors of HEAD.
- Plan gates printed: TRACER-SEEN-RED, TRACER-GREEN, EXPANSION-DISCRIMINATES, EXPANSION-GREEN, LIVE-SIX-GREEN, TESTUNIT-GREEN, SPOTLESS-BASELINE-EQUAL, COLS13-COMMITTED.

---
*Phase: 23-declared-nullability-hardening-review-closure*
*Completed: 2026-10-20*
