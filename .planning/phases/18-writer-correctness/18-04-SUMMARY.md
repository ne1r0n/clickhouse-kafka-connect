---
phase: 18-writer-correctness
plan: 04
subsystem: sink-writer-tests
tags: [debezium-cdc, empty-record-skip, exactly-once, byte-identity, mutation-check, phase-gate, JSONP-09]
requires:
  - phase: 18-writer-correctness
    provides: "18-03 skipEmptyCdcRecord helper, CdcTestFixtures, ledger row L-30; 18-02 LogCapture"
provides:
  - "CapturingClickHouseWriter: a real ClickHouseWriter whose two network insert methods capture the exact body, route and dedup token (no Mockito)"
  - "EmptyCdcSkipUnitTest: 19 Docker-free byte, edge and log-line pins across explicit-json, bypass-json, rowbinary-defaults and rowbinary-plain"
  - "EmptyCdcExactlyOnceStateTest: 5 pins that Processing ends AFTER_PROCESSING over the full range, keeps the full-range dedup token, and rebuilds an identical body on retry"
  - "Phase 18 gate verdict: cache-busted testUnit and full suite on ClickHouse latest x V2 green, anchors invariant PASS, 15-path scope, FORK-DELTA regions and ledger L-23..L-30 verified"
affects: [19, 20-release]
plan_head_before: 25c621ce0bfcd509831900c645abf253ae1d733b
plan_head_after: dc4890196e54067213988bf05ef7216544db6b23
actuals:
  tokens: 5800
  tasks: 2
  commits: 1
tech-stack:
  added: []
  patterns:
    - "Real writer, replaced network seam: override the protected doInsertJson / doInsertRawBinary of a real ClickHouseWriter and call the private serializers reflectively, instead of mocking the writer or its helper client"
    - "Mutation check never committed: remove the production change in the working tree, record the red case names, git checkout the one file, prove git diff --quiet -- src/main"
key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/db/CapturingClickHouseWriter.java
    - src/test/java/com/clickhouse/kafka/connect/sink/db/EmptyCdcSkipUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/processing/EmptyCdcExactlyOnceStateTest.java
  modified:
    - docs/FORK-DELTA.md
    - .planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md
key-decisions:
  - "CapturingClickHouseWriter overrides doInsertJson and doInsertRawBinary, not the V2 hooks, so no ClickHouseHelperClient (and no Mockito inline mock) is needed. Inline-mocking ClickHouseHelperClient crashed the Semeru/OpenJ9 test JVM natively in the full suite"
  - "The unit pins assert bytes with assertArrayEquals and never row counts (D-03)"
requirements-completed: [JSONP-09, WRT-10, WRT-11, WRT-12]
coverage:
  - id: D1
    description: "JSONP-09: on every route (explicit-json, bypass-json, rowbinary-defaults, rowbinary-plain) the mixed batch body equals, byte for byte, the body of the real-only records in batch order, with the full-range token"
    requirement: JSONP-09
    verification:
      - kind: unit
        ref: "EmptyCdcSkipUnitTest#JSONP-09: the mixed batch body equals the real-only body in batch order on <route> (4 cases)"
        status: pass
    human_judgment: false
  - id: D2
    description: "JSONP-09 / D-01: an all-empty batch still calls the insert hook exactly once with a zero-length body (no short-circuit); a null value map and a one-record empty batch serialize to zero bytes"
    requirement: JSONP-09
    verification:
      - kind: unit
        ref: "EmptyCdcSkipUnitTest#JSONP-09: an all-empty batch still opens the insert ...; a CDC record with a null value map ... (8 cases)"
        status: pass
    human_judgment: false
  - id: D3
    description: "JSONP-09 idempotency: the same batch twice on one writer gives byte-identical bodies and equal tokens on every route"
    requirement: JSONP-09
    verification:
      - kind: unit
        ref: "EmptyCdcSkipUnitTest#JSONP-09: the same mixed batch twice gives byte-identical bodies and tokens on <route> (4 cases)"
        status: pass
    human_judgment: false
  - id: D4
    description: "D-02: the mixed batch logs exactly the two DEBUG lines Skipping empty CDC record (no values) topic [cdc_topic] partition [0] offset [11] and [12], identical on all four routes"
    requirement: JSONP-09
    verification:
      - kind: unit
        ref: "EmptyCdcSkipUnitTest#JSONP-09: the mixed batch logs one DEBUG skip line per empty record, identical on every route"
        status: pass
    human_judgment: false
  - id: D5
    description: "Predicate before cleanup: on the bypass route a schemaless record with only unknown fields still writes the defaults-only row {} and a CDC record holding only fields the table lacks is not skipped"
    requirement: JSONP-09
    verification:
      - kind: unit
        ref: "EmptyCdcSkipUnitTest#JSONP-09: on the bypass route ... (2 cases)"
        status: pass
    human_judgment: false
  - id: D6
    description: "D-03 state: Processing over InMemoryState ends AFTER_PROCESSING with the min and max offset of the full batch for a mixed and an all-empty batch, with InMemoryDBWriter and with the real writer (token cdc_topic-0-10-<max>, real-only body); a BEFORE_PROCESSING retry rebuilds the identical body and token"
    requirement: JSONP-09
    verification:
      - kind: unit
        ref: "EmptyCdcExactlyOnceStateTest (5 cases)"
        status: pass
    human_judgment: false
  - id: D7
    description: "Mutation check: with the two skip call sites removed, 16 of 24 cases fail (mixed, all-empty, null-map and log-line on every route, plus the real-writer state test), and src/main matches HEAD after the restore"
    requirement: JSONP-09
    verification:
      - kind: other
        ref: "temporary removal of skipEmptyCdcRecord call sites; git diff --quiet -- src/main afterwards"
        status: pass
    human_judgment: false
  - id: D8
    description: "Phase 18 gate: cache-busted testUnit (43 classes, 516 tests) and full suite (79 classes, 902 tests, 18 named skips) on latest x V2 green; anchors invariant PASS; exactly the 15 expected non-.planning paths changed; FORK-DELTA changed only in Phase 18 regions; ledger L-23..L-30"
    requirement: WRT-12
    verification:
      - kind: integration
        ref: "Task 2 first automated (PHASE-18-SUITE-GREEN) and second automated (PHASE-18-GATE-OK)"
        status: pass
    human_judgment: false
duration: 29min
completed: 2026-10-12
status: complete
---

# Phase 18 Plan 04: Empty-CDC pins and Phase 18 gate Summary

**The JSONP-09 empty-CDC skip is now pinned Docker-free on exact insert bytes, the dedup token, the skip log line and exactly-once state across four write routes (mutation-checked: 16 of 24 cases go red without the skip), and the Phase 18 gate is green on ClickHouse 26.9.16.11 with client V2 (902 tests, 18 named skips, anchors PASS).**

## Performance
- **Duration:** 29 min
- **Started:** 2026-10-12T14:06:44Z
- **Completed:** 2026-10-12T14:35Z
- **Tasks:** 2
- **Files modified:** 5 (3 new test classes, FORK-DELTA, ledger); nothing under `src/main`

## Accomplishments
- `CapturingClickHouseWriter` extends the real `ClickHouseWriter` and replaces only `doInsertJson` and `doInsertRawBinary`. Routing (`doInsertBatch`), table lookup and the private serialization loops run unchanged; it captures route, exact body bytes, dedup token and the defaults flag. It is the "never mock the writer" substitute for state tests.
- `EmptyCdcSkipUnitTest` (19 cases, all `JSONP-09: `): mixed body equals the real-only body, all-empty batch opens the insert with a zero-length body, same batch twice is byte-identical, null value map serializes to zero bytes, identical DEBUG skip lines on all four routes, and two bypass defaults-only pins (`{}`).
- `EmptyCdcExactlyOnceStateTest` (5 cases): `Processing` with `InMemoryDBWriter` and with the real writer ends AFTER_PROCESSING over offsets 10..13 (mixed) and 10..12 (all-empty), the token is `cdc_topic-0-10-<max>`, and a batch retried from BEFORE_PROCESSING [10, 13] produces the same body and token.
- FORK-DELTA JSONP-09 row and ledger L-30 now name both Docker-free classes; ledger "Source plans" gained `18-04 (L-30 pins)`.
- Phase gate passed (below). No JMX change (`git diff 3b9c381b..HEAD -- src/main/.../util/jmx` is empty, D-04).

## Task Commits
1. **Task 1: Docker-free pins, mutation-checked, with the FORK-DELTA and ledger pinned-by updates** - `dc489019` (test). Amended once, before anything was pushed, see Deviations.
2. **Task 2: Phase 18 gate** - no commit by design (no tracked file changes).

**Plan metadata:** see the `docs(18-04)` commit at the tip of this plan.

## Mutation check (never committed)
Both `if (skipEmptyCdcRecord(record)) { continue; }` statements were deleted from `ClickHouseWriter.java`, then `EmptyCdcSkipUnitTest` and `EmptyCdcExactlyOnceStateTest` were rerun: 24 cases, 8 passed, 16 failed. Red cases:
- `the mixed batch body equals the real-only body in batch order` on explicit-json, bypass-json, rowbinary-defaults, rowbinary-plain (4)
- `an all-empty batch still opens the insert with a zero-length body` on all four routes (4)
- `a CDC record with a null value map serializes to zero bytes` on all four routes (4)
- `the same mixed batch twice gives byte-identical bodies and tokens` on rowbinary-plain (1; the other routes stay deterministic even with phantom rows, so only the plain table, which throws without the skip, fails)
- `the mixed batch logs one DEBUG skip line per empty record, identical on every route` (1)
- `Processing over the real writer ... for the mixed batch` and `... for the all-empty batch` (2)

explicit-json went red as well, which is a superset of the plan's expectation: 18-03 moved the metadata-free skip into the shared helper, so removing the call sites removes it there too. Restore: `git checkout -- src/main/java/.../ClickHouseWriter.java`, then `git diff --quiet -- src/main` succeeded, and the same 38 cases (with `ProcessingTest`) were green again. The mutation was run twice (before and after the harness fix below) with identical red sets.

## Phase 18 gate
- **First automated (`PHASE-18-SUITE-GREEN`)**, run by the gate's own command on the final code:
  - `testUnit`: 43 classes, 516 tests, 0 failures, 0 errors, 2 skipped (testUnit has no `clickhouse`-tagged tests).
  - Full suite `test`: 79 classes, 902 tests, 0 failures, 0 errors, 18 skipped, exactly the 18 named skips (7 named plus the 11 `exactlyOnceStateMismatchTest` cases). All Phase 18 classes present; no stale results.
  - Server: ClickHouse **26.9.16.11**, image `clickhouse/clickhouse-server:latest` created **2026-10-11T16:49:40Z**.
- **Second automated (`PHASE-18-GATE-OK`)**: `extra [] missing []` (exactly the 15 expected non-`.planning` paths), FORK-DELTA `outside-identical True gone 1 new 7 retired-changed 1`, order `[9] [10, 11, 12, 13, 14] 15 utils [21]`, ledger `L-23..L-30 ok True`, clean tree for `src docs .planning/scripts ROADMAP.md ledger`.
- **Anchors gate line:** `invariants: provenance_paths=21 anchor_commits=245 anchor_rows=55 marker_commits=292 result=PASS`

## Files Created/Modified
- `src/test/java/com/clickhouse/kafka/connect/sink/db/CapturingClickHouseWriter.java` - public capture writer, `Capture` record, `stubCdcTable`, `convertAll`
- `src/test/java/com/clickhouse/kafka/connect/sink/db/EmptyCdcSkipUnitTest.java` - 19 byte, edge, log and bypass pins
- `src/test/java/com/clickhouse/kafka/connect/sink/processing/EmptyCdcExactlyOnceStateTest.java` - 5 state, token and retry pins
- `docs/FORK-DELTA.md` - JSONP-09 row's pinned-by cell
- `.planning/phases/18-writer-correctness/18-BEHAVIOUR-LEDGER.md` - L-30 pinned-by cell and Source plans line

## Decisions Made
- Override `doInsertJson` and `doInsertRawBinary` in the capture writer instead of the V2 hooks plus a mocked `ClickHouseHelperClient` (see Deviations). The pins are unaffected: they target the serialization loops, the routing and `Processing`.
- Added `stubCdcTable` and `convertAll` as public statics on `CapturingClickHouseWriter` rather than editing `CdcTestFixtures`, because the plan's commit contract allows exactly five files and `CdcTestFixtures` is not among them.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] The capture harness crashed the OpenJ9 test JVM through Mockito inline mocking**
- **Found during:** Task 2 (first two runs of the gate's full-suite command)
- **Issue:** Following the plan, the `CapturingClickHouseWriter` constructor created `mock(ClickHouseHelperClient.class)`. In the full suite (JaCoCo agent on, 6 forks, Semeru 17.0.9 / OpenJ9) the test worker died with exit value 255 twice, always on the first `EmptyCdcSkipUnitTest` case. The IBM javacores in the repo root showed a native GPF in `libj9vm29` (`getJNIMethodID`) inside `sun.instrument.InstrumentationImpl.retransformClasses0`, called from Mockito's `InlineBytecodeGenerator` from `CapturingClickHouseWriter.<init>`. Three later full-suite runs on the same code passed, so the crash is an intermittent JVM bug; the isolated class always passed.
- **Fix:** Removed Mockito from the harness. The writer now overrides the protected `doInsertJson(records, table, queryId)` and `doInsertRawBinary(records, table, queryId, supportDefaults, retry)`, one level above the V2 hooks, so the helper client is never touched and only the `csc` field is set reflectively. The commit was amended (local, unpushed) so the plan's single `test(18-04)` commit holds.
- **Files modified:** `src/test/java/com/clickhouse/kafka/connect/sink/db/CapturingClickHouseWriter.java`
- **Verification:** unit pins green (38/38 with `ProcessingTest`), mutation check re-run with the identical 16 red cases, Task 1 verify printed `JSONP09-DOCKER-FREE-OK`, then the gate command went green on the final code.
- **Commit:** `dc489019`
- **Side effect:** the two crash attempts left `core.*`, `javacore.*`, `Snap.*` and `jitdump.*` files in the repo root; they were untracked and were deleted individually (no `git clean`).

**Total deviations:** 1 auto-fixed (1 Rule 1). **Impact:** one harness detail differs from the plan text (which method pair is overridden); every behavioural assertion and acceptance criterion is as planned. The plan's claim that the V1/V2 selection and the insert error path "run unchanged" no longer holds for this harness; no pin depends on them.

## Issues Encountered
- Two full-suite gate attempts failed with the worker crash above, plus one diagnostic run with `--info` and one with `JAVA_TOOL_OPTIONS` crash settings, which both passed. All were diagnostic; the accepted verdict comes only from the gate command run on the final code.
- The project-root pin guard was run in a condensed form (`/tmp` script comparing `git rev-parse --show-toplevel` with the pinned root, which also covers the primary-checkout case); the superproject branch was not needed because the cwd was always the pinned root.

## Deferred Items
- 18-01's `ClickHouseWriterNullMessageUnitTest` also inline-mocks `ClickHouseHelperClient` and so carries the same intermittent OpenJ9 crash risk on this machine (it passed in every run of this plan). Recorded in `deferred-items.md`; out of scope here.

## Known Stubs
None.

## Threat Flags
None. No new network surface; the plan adds test code and doc cells only. T-18-14 (mutation tampering) is mitigated as designed: `src/main` verified clean after the restore and again by the anchors gate. T-18-15 (stale results) is mitigated by the gate command deleting the result directories and checking timestamps.

## Next Phase Readiness
Phase 18 is complete and gate-verified for `/gsd-verify-work` and the review. Nothing is pushed. Phase 20 can take the L-30 pinned-by classes straight from the ledger.

## Self-Check: PASSED
- Created files exist: the three test classes, FORK-DELTA, ledger (all FOUND).
- Commit `dc489019` is an ancestor of HEAD; `check evaluation-scope --plan 18-04 --commits-only` resolved with that one commit; `git diff --quiet -- src/main` succeeds; jmx diff empty.
- Task 1 acceptance (`JSONP09-DOCKER-FREE-OK`, mutation names recorded, jmx diff empty) and Task 2 acceptance (`PHASE-18-SUITE-GREEN`, `PHASE-18-GATE-OK`, totals, server version and image date, anchors line) all re-run and recorded above.

---
*Phase: 18-writer-correctness*
*Completed: 2026-10-12*
