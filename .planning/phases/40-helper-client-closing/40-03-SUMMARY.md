---
phase: 40-helper-client-closing
plan: "03"
subsystem: sink-state
tags: [helper-client-lifecycle, keeper-state-provider, proxy-sink-task, red-first, gate-b, phase-gate, clickhouse-kafka-connect]

requires:
  - phase: 40-helper-client-closing
    provides: "40-red-keeper.patch (Keeper red tree), the extended CountingHelperClient and the writer-half commit de3faf77 plus the test-only seed fae1fcb1 (plans 40-01 and 40-02)"
provides:
  - "StateProvider default no-op close(); KeeperStateProvider.close() closes once, only the helper client its own constructor built; ProxySinkTask.stop() stops the writer, unregisters the MBean and closes the state provider in nested try/finally"
  - "The Keeper-half commit b55f108a (seven files) with the FORK-DELTA Keeper row, harness row, Known cost 13 closed and four fork-anchors rows"
  - "Evidence E-08..E-11 (pass); the evidence index E-01..E-11 reads pass"
  - "Phase gate: verify-merge-invariants.sh result=PASS on the E-10 commit 6775cd93"
affects: [41-release]

actuals:
  tokens: 15921
  tasks: 3
  commits: 5

tech-stack:
  added: []
  patterns:
    - "Owned-client close: close() copies the field into a local, nulls the field, closes the local, so a second call is a no-op and chc itself is never nulled (state reads keep working after close)"
    - "Nested try/finally in stop(): each step's finally runs the next, the last exception thrown reaches the caller"
    - "Injected-client clause pinned by source identity plus a tripwire test, because no instance with an injected client can be built today"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java
    - src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java
  modified:
    - src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java
    - src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java
    - src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java
    - docs/FORK-DELTA.md
    - .planning/scripts/fork-anchors.txt
    - .planning/phases/40-helper-client-closing/40-COMMAND-EVIDENCE.md

key-decisions:
  - "The writer-files untouched check of E-09 is taken against fae1fcb1 (writer-half commit plus the user-authorised test-only seed), not de3faf77, because the seed legitimately changed ClickHouseWriterTest.java under src/test/.../sink/db; recorded in E-09 instead of editing the earlier E-06/E-07 records"
  - "The invariants gate ran once, on the E-10 commit, which covers both code commits because the script evaluates the anchor inventory at every commit of the range"

patterns-established:
  - "Gate adjustment recorded in the new entry: when an authorised prior-plan deviation moves a baseline, the later entry names the adjusted baseline rather than amending the earlier one"

requirements-completed: [CLS-03, CLS-04]

coverage:
  - id: D1
    description: "Tracer: KeeperStateProvider.close() closes only the client its constructor built; the two Keeper cases turn green on V1 and V2 while both ProxySinkTask.stop cases stay red on close count"
    requirement: CLS-03
    verification:
      - kind: unit
        ref: "KeeperStateProviderClosingUnitTest 5/0 and ProxySinkTaskStopUnitTest 2/2 red on close count, intermediate run (E-08)"
        status: pass
    human_judgment: false
  - id: D2
    description: "ProxySinkTask.stop() closes the state provider in the innermost finally after the writer and the MBean; all 7 Keeper-side cases, the 22 writer cases and the guards green with Docker unreachable"
    requirement: CLS-03
    verification:
      - kind: unit
        ref: "KeeperStateProviderClosingUnitTest 5/0, ProxySinkTaskStopUnitTest 2/0, WriterHelperClientClosingUnitTest 22/0 and the four guard classes (E-09)"
        status: pass
    human_judgment: false
  - id: D3
    description: "The exactly-once read and write paths are unchanged: getStateRecord through the end of KeeperStateProvider.java equals phase start and the writer-half commit, the injected-client constructor and init() equal phase start, ProxySinkTask.java equals the writer-half commit outside stop()"
    requirement: CLS-03
    verification:
      - kind: command
        ref: "region_checks and proxy_checks in the Task 2 verify (E-09)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Gate B on the Keeper-half commit: testUnit with Docker unreachable (1716 tests, 2 skipped) and the full suite on ClickHouse latest x V2 (2314 tests, 18 named skips), with the six exactly-once classes green"
    requirement: CLS-04
    verification:
      - kind: integration
        ref: "gradlew testUnit --rerun and gradlew test --rerun, CLICKHOUSE_VERSION=latest CLIENT_VERSION=V2 (E-10)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Phase gate: cumulative merge-invariant script result=PASS, anchors mirror rows=180 bad=0, src/main differs from phase start in exactly four files, VERSION and CHANGELOG.md untouched, origin unmoved"
    requirement: CLS-04
    verification:
      - kind: command
        ref: "verify-merge-invariants.sh 54e63648 6775cd93 (E-11)"
        status: pass
    human_judgment: false

duration: 50min
completed: 2026-11-02
status: complete
plan_head_before: e8e0318b41474a4f7e0ec679bea54b730ecddfd6
plan_head_after: ac12eae4047b8c33d37af2d6a3176d5b12435a08
commits: 5
---

# Phase 40 Plan 03: Keeper half of helper-client closing and the phase gate Summary

**`KeeperStateProvider.close()` closes only the client its constructor built, `ProxySinkTask.stop()` closes the state provider in a nested `finally` after the writer and the MBean, the exactly-once read and write paths are byte-identical, and gate B (1716 and 2314 tests, 0 failures) plus the cumulative invariants gate (`result=PASS`) hold on the result**

## Performance

- **Duration:** about 50 min (06:37Z to 07:27Z), of which 41 min were the single invariants-gate run and under 3 min the full suite
- **Tasks:** 3 of 3 complete
- **Files modified:** 8 (seven in the Keeper-half commit plus the evidence file)

## Accomplishments

- E-08: re-applied `40-red-keeper.patch` (SHA-256 equal to E-04, applied cleanly on the plan-40-02 tip) and reproduced E-04's four-method red set with the tripwire green; then `ownedClient`, `ownedClient = chc;` in the factory constructor and `close()` turned `KeeperStateProviderClosingUnitTest` green (5/0 on V1 and V2) while both `ProxySinkTaskStopUnitTest` cases stayed red on `close count`.
- E-09 and the Keeper-half commit `b55f108a`: `ProxySinkTask.stop()` as `try { dbWriter.stop(); } finally { try { statistics.unregisterMBean(); } finally { stateProvider.close(); } }`; 5 + 2 + 22 cases and the four guards green; `getStateRecord` through end of file, the injected-client constructor and `init()` byte-identical to phase start; `ProxySinkTask.java` identical to the writer-half commit outside `stop()`; FORK-DELTA Keeper row, harness row and Known cost 13 closed; four anchors rows (`rows=180 bad=0`).
- E-10: `testUnit` with Docker unreachable ran 1716 tests (1687 plus 29), 0 failures, 2 skipped; the full suite on ClickHouse `latest` (`26.10.3.7`, digest unchanged) x V2 ran 2314 tests (2285 plus 29), 0 failures, 0 errors, 18 named skips; `DedupTokenRetryLiveTest` 8, `KeeperStateProviderRestartLiveTest` 6, `ClickHouseSinkTaskBufferTest` 35, `ClickHouseSinkTaskWithSchemaTest` 97, `BufferedOffsetCommitUnitTest` 15 and `ProcessingTest` 14 all green.
- E-11: `verify-merge-invariants.sh 54e63648 6775cd93` ended `invariants: provenance_paths=21 anchor_commits=1356 anchor_rows=180 marker_commits=1422 anchor_exempt=19 result=PASS`; `src/main` differs from phase start in exactly `ClickHouseWriter.java`, `ProxySinkTask.java`, `StateProvider.java` and `KeeperStateProvider.java`; `VERSION` and `CHANGELOG.md` untouched; origin `custom_build` still `72e8ccff`; nothing pushed.

## Task Commits

1. **Task 1 (tracer): Keeper red re-run and the provider-close slice** - `7869750f` (docs: E-08; the code stays uncommitted by design until Task 2)
2. **Task 2: nested stop, state paths byte-identical, FORK-DELTA and anchors** - `b55f108a` (fix: the Keeper-half commit) and `19e5cf75` (docs: E-09)
3. **Task 3: gate B and the phase gate** - `6775cd93` (docs: E-10) and `ac12eae4` (docs: E-11)

**Plan metadata:** committed with this summary (docs: complete plan)

## Files Created/Modified

- `src/main/java/com/clickhouse/kafka/connect/sink/state/StateProvider.java` - `default void close()` with a one-line Javadoc
- `src/main/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProvider.java` - `Function` import, public constructor delegating to a package-private factory constructor, `ownedClient` field and assignment, `@Override public void close()` between `init()` and `getStateRecord`
- `src/main/java/com/clickhouse/kafka/connect/sink/ProxySinkTask.java` - `stop()` in nested `try/finally`
- `src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderClosingUnitTest.java` - five Keeper cases including the injected-client tripwire
- `src/test/java/com/clickhouse/kafka/connect/sink/ProxySinkTaskStopUnitTest.java` - two stop-order cases
- `docs/FORK-DELTA.md` - Keeper row, harness row, Known cost 13 closed with residuals and the accepted 5 s limit
- `.planning/scripts/fork-anchors.txt` - second `# Phase 40` comment and four Keeper rows
- `.planning/phases/40-helper-client-closing/40-COMMAND-EVIDENCE.md` - E-08..E-11

## Decisions Made

- The injected-client clause (an injected client stays open) has no behavioural pin because the injected-client constructor cannot complete today; it is pinned by source identity plus the `NullPointerException` tripwire, stated in E-09, E-11 and FORK-DELTA.
- Known cost 13 is closed, with four residuals read from the code and left unfixed (a Keeper constructor that fails on client V1, a helper constructor that fails after building its V2 client, a second `start()` on one writer, `ClickHouseSinkTask.stop()` calling `deliveryStrategy.stop()` without `try/finally`) and the accepted `stop()` 5 s timeout limit.
- The invariants gate ran once on the E-10 commit instead of once per code commit; its scope is recorded as `invariants_scope` in E-11.

## Deviations from Plan

### Operator-directed adjustment

**1. [Operator note - baseline adjustment] Writer-files check taken against the test-seed tip**
- **Found during:** Task 2 verify (and Task 3 `src_main` checks)
- **Issue:** the plan's `writer_files_untouched` clause is `git diff --quiet "$WC" HEAD -- $M/db $TS/db` with `WC` = E-06 `writer_commit` (`de3faf77`). Plan 40-02's user-authorised test-only seed `fae1fcb1` changed `ClickHouseWriterTest.java` (under `src/test/.../sink/db`) after `WC`, so the clause cannot hold as written.
- **Fix:** the clause was evaluated against `fae1fcb1`, the tip that holds the seed (`git diff --quiet fae1fcb1 HEAD -- src/main/.../sink/db src/test/.../sink/db` exits 0; `fae1fcb1` is an ancestor of the Keeper-half commit). The `src/main` comparison against `de3faf77` is unaffected. The adjustment is written in E-09 and no earlier evidence entry was edited. The Task 2 verify block was run with that single line changed.
- **Files modified:** none (evidence wording only, in E-09)
- **Committed in:** `19e5cf75`

---

**Total deviations:** 1 (a baseline adjustment named by the operator, no code or test change). **Impact on plan:** none on scope; every other plan expectation held as written.

## Issues Encountered

- None blocking. The first Task 1 sanity call for the red names used a mistyped method name once; the helper reported it as a mismatch and the correct four names were then verified (no recorded artefact depends on it).
- The shell wait for the invariants gate was backgrounded by the harness after 180 s; polling in shorter chunks avoided any Gradle run or commit while the gate ran.

## Known Stubs

None. No stub, placeholder or empty data flow was added.

## Threat Flags

None. No new network endpoint, auth path, file access or schema surface: the KeeperMap table, rows and state key are untouched, and `close()` logs nothing.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Ready: the Phase 40 gates are green (gate A in plan 40-02, gate B and the invariants gate here). Phase verification and the release phase can proceed; `VERSION` and `CHANGELOG.md` are untouched and belong to the release phase.
- Nothing was pushed; origin `custom_build` still equals `72e8ccff`. The Keeper-half commit `b55f108a` is revertable on its own (one `git revert` restores the old `stop()` and provider with no stored-state change).
- Recorded residuals for the owner (Known cost 13): the accepted 5 s `stop()` timeout race, the `deliveryStrategy.stop()` order in `ClickHouseSinkTask.stop()`, and a Keeper constructor that fails on client V1 (research OQ-1, out of scope).

## Self-Check: PASSED

- Created and modified files exist: `KeeperStateProviderClosingUnitTest.java`, `ProxySinkTaskStopUnitTest.java`, `StateProvider.java`, `KeeperStateProvider.java`, `ProxySinkTask.java`, `FORK-DELTA.md`, `fork-anchors.txt`, `40-COMMAND-EVIDENCE.md` (FOUND).
- Commits `7869750f`, `b55f108a`, `19e5cf75`, `6775cd93`, `ac12eae4` are ancestors of HEAD (FOUND); measured `commits: 5` from `plan_head_before` to `plan_head_after`.
- E-08..E-11 read `pass` in the index and their entries (`python3 -I 40-evidence-check.py index ... 11 --done` OK); the evidence `secrets` check is clean; origin equals E-01.
- Task verify blocks: Task 1, Task 2 (with the one adjusted line) and Task 3 printed `P40-03-T1-OK`, `P40-03-T2-OK` and `P40-03-T3-OK`.

---
*Phase: 40-helper-client-closing*
*Completed: 2026-11-02*
