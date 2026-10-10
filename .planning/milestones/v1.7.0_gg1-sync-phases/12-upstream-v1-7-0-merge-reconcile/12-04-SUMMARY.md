---
phase: 12-upstream-v1-7-0-merge-reconcile
plan: "04"
subsystem: testing
tags: [exactly-once, keeper-map, keeper-state-provider, mutation-check, eos, client-v2, setCompressionMethod]

requires:
  - phase: 12-03
    provides: "setCompressionMethod pinned on every client; testUnit 342 tests / 0 failed / 2 skipped"
provides:
  - "KeeperStateProviderRestartLiveTest: a fresh KeeperStateProvider reads back the exact (min, max, state) another instance wrote (EOS-01)"
  - "D-15 mutation evidence: the test fails against the pre-v1.7.0 summary-count read, restored byte-identical to v1.7.0"
affects: [12-05, phase-13, phase-15]

plan_head_before: 5111b530118ddc516907d74848a04f2d9bf534a9
plan_head_after: 0a0cf6b3a573c16a0a76730cd058e84d5b27eb57

actuals:
  tokens: 1328
  tasks: 2
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Restart simulation by building a NEW KeeperStateProvider from a NEW ClickHouseSinkConfig over the same props, so the empty in-memory stateMap forces the value to come from the KeeperMap table"
    - "Temporary production mutation applied as a surgical working-tree edit, observed RED, restored with git checkout, proven by an empty diff against refs/tags/v1.7.0"

key-files:
  created:
    - src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderRestartLiveTest.java
  modified: []

key-decisions:
  - "Hypothesis A1 held on the first mutation run, so the D-16 hunt and its blocking-human stop rule were not needed"
  - "KeeperStateProvider.java was never committed in changed form: zero fork edits, byte-identical to v1.7.0"

requirements-completed: [EOS-01]

coverage:
  - id: D1
    description: "State written through one KeeperStateProvider is read back by a new instance with the exact (minOffset, maxOffset, state), first BEFORE_PROCESSING then AFTER_PROCESSING, on client V2 with setCompressionMethod true and false"
    requirement: EOS-01
    verification:
      - kind: integration
        ref: "src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderRestartLiveTest.java#stateWrittenByOneProviderIsReadBackByAFreshProvider (4 invocations)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Offsets above Integer.MAX_VALUE (+10, +19) round-trip exactly; distinct min 10 / max 19 expose a swapped (maxOffset, minOffset) constructor order"
    requirement: EOS-01
    verification:
      - kind: integration
        ref: "KeeperStateProviderRestartLiveTest#stateWrittenByOneProviderIsReadBackByAFreshProvider min=2147483657 max=2147483666 (true and false)"
        status: pass
    human_judgment: false
  - id: D3
    description: "A never-written topic-partition key reads back as State.NONE with min 0 and max 0 from a fresh provider"
    requirement: EOS-01
    verification:
      - kind: integration
        ref: "KeeperStateProviderRestartLiveTest#freshKeyReadsBackNone (2 invocations)"
        status: pass
    human_judgment: false
  - id: D4
    description: "With getStateRecord temporarily reverted to the old summary-count read, the test fails (4 of 6, actual NONE), and KeeperStateProvider.java is restored byte-identical to v1.7.0"
    requirement: EOS-01
    verification:
      - kind: other
        ref: "temporary revert run: 6 tests, 4 failed 'expected: <BEFORE_PROCESSING> but was: <NONE>'; restore: git diff refs/tags/v1.7.0 -- KeeperStateProvider.java is 0 bytes; re-run 6 tests, 0 failures"
        status: pass
    human_judgment: false

duration: 12min
completed: 2026-10-07
status: complete
---

# Phase 12 Plan 04: KeeperStateProvider restart-read pin Summary

**A fresh KeeperStateProvider is proven to read back the exact (min, max, state) another instance wrote to the KeeperMap table, and the test was seen to fail (state read as NONE) against the pre-v1.7.0 response-summary row count.**

## Performance

- **Duration:** about 12 min
- **Completed:** 2026-10-07T19:55Z
- **Tasks:** 2
- **Files modified:** 1 (new test class); `KeeperStateProvider.java` mutated and restored, never committed

## Accomplishments

- `KeeperStateProviderRestartLiveTest` (6 invocations, `extends ClickHouseBase`, shared container with `clickhouse_keeper_map.xml`): writer A sets BEFORE_PROCESSING, a new provider B reads it, A sets AFTER_PROCESSING, a new provider C reads it. Four parameter rows: setCompressionMethod true and false, each at offsets (10, 19) and (Integer.MAX_VALUE + 10, Integer.MAX_VALUE + 19). Plus two rows for an unwritten key reading as NONE with min 0 and max 0.
- D-15 mutation proof recorded below.
- Nothing mocked. Each state table is a KeeperMap table named `eos01_<nanoTime>` on its own keeper path, dropped in a `finally` block.

## Task Commits

1. **Task 1 (no commit): write the test, run it green on both flag values** - 6 tests, 0 failures.
2. **Task 2: D-15 mutation check, restore, single EOS commit** - `0a0cf6b3` (test)

**Plan metadata:** committed separately as `docs(12-04)`.

## D-15 mutation evidence

**Mutation (working tree only, never staged).** In `getStateRecord`, replaced the `response.stream().collect(...)` line and the `records.size()` count with `long totalResultsFound = response.getSummary().getResultRows();`, changed the debug line to log `response.getSummary().getReadRows()`, and replaced `records.get(0)` with `response.firstRecord()`. This reproduces the pre-v1.7.0 read exactly (compared against `git show pre-v1.7.0-sync:...KeeperStateProvider.java` lines 95-125). The `.setCompressionMethod(...)` builder line was left alone, so the result is not confounded.

**RED run under the mutation:** `6 tests, 2 succeeded, 4 failed`.

- Failed, all four `stateWrittenByOneProviderIsReadBackByAFreshProvider` rows (true/10/19, false/10/19, true/2147483657/2147483666, false/2147483657/2147483666), each at the first read (the BEFORE_PROCESSING assertion): `org.opentest4j.AssertionFailedError: expected: <BEFORE_PROCESSING> but was: <NONE>`.
- Passed under the mutation, as expected: both `freshKeyReadsBackNone` rows (a key with no row reads NONE under either code). Hypothesis A1 is confirmed: the summary header's `result_rows` is 0 on the streamed response, so the old read returns NONE for a key that has state. In production that would make the state machine treat an already-processed range as NEW.

**Restore proof:**

- `git checkout -- src/main/java/.../KeeperStateProvider.java`
- `git diff --quiet -- <file>` rc=0 (working-tree diff empty)
- `git diff --quiet refs/tags/v1.7.0 HEAD -- <file>` rc=0
- `git diff refs/tags/v1.7.0 -- <file> | wc -c` prints `0`
- `git status --porcelain` shows no source change besides the orchestrator's own `.planning/` bookkeeping

**GREEN re-run after restore:** `TEST-...KeeperStateProviderRestartLiveTest.xml` shows `tests="6" skipped="0" failures="0" errors="0"`.

**D-16 hunt:** not run. The mutation went RED on the first attempt, so the blocking-human stop rule did not apply and EOS-01 is stated as written.

## Verification

- EOS commit `0a0cf6b3` touches only the new test file (`git show --name-only --format= HEAD` is that one path); the commit message starts with `test(12-04):`.
- `bash .planning/scripts/verify-merge-invariants.sh <M> HEAD` ends `provenance_paths=21 anchor_commits=12 anchor_rows=40 marker_commits=59 result=PASS`.
- The only commit in `pre-v1.7.0-sync..HEAD` that touches `KeeperStateProvider.java` is `a7cc5ec9`, an upstream commit (ancestor of `refs/tags/v1.7.0`) brought in by the merge. No commit authored on this branch touches it.
- Post-commit deletion check: no deleted files.
- Not run (out of scope for this plan): full `testUnit` and the wider live suite; only the new class was run, as the plan specifies. `spotlessCheck` is red repo-wide and was not used as a gate.

## Files Created/Modified

- `src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderRestartLiveTest.java` - EOS-01 restart-read live test (new test package directory `sink/state/provider`).

## Decisions Made

- The test forces `CLIENT_VERSION=V2` because that is what production uses; the state read goes through the V1 `ClickHouseClient` internally either way.
- `StateRecord` is constructed with `maxOffset` before `minOffset`, and the fixture offsets are distinct, so a swapped order fails an assertion.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## Known Stubs

None.

## Threat Flags

None. No new network endpoint, auth path or schema change; the test uses generated identifiers only (T-12-13 accepted as planned). T-12-12 mitigated: the mutation never crossed into history.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

12-05 can record the mutation result as a Recorded Fact: summary-count read gives NONE for a key that has state (4 of 6 fail), stream-count read gives the stored state. `KeeperStateProvider.java` remains byte-identical to v1.7.0.

---
*Phase: 12-upstream-v1-7-0-merge-reconcile*
*Completed: 2026-10-07*

## Self-Check: PASSED

- FOUND: src/test/java/com/clickhouse/kafka/connect/sink/state/provider/KeeperStateProviderRestartLiveTest.java
- FOUND: commit 0a0cf6b3 (ancestor of HEAD)
- KeeperStateProvider.java diff vs refs/tags/v1.7.0: empty
